/*
 * Copyright (c) 2017 NCIC, Institute of Computing Technology, Chinese Academy of Sciences
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.ncic.bioinfo.sparkseq.algorithms.walker.haplotypecaller.afcalculate;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.GenotypeLikelihoods;
import htsjdk.variant.variantcontext.GenotypesContext;
import htsjdk.variant.variantcontext.VariantContext;
import org.ncic.bioinfo.sparkseq.algorithms.utils.GATKVariantContextUtils;
import org.ncic.bioinfo.sparkseq.algorithms.utils.MathUtils;
import org.ncic.bioinfo.sparkseq.algorithms.walker.haplotypecaller.ExactACcounts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Author: wbc
 */
public abstract class DiploidExactAFCalculator extends ExactAFCalculator {

    private static final double LOG10_OF_2 = MathUtils.Log10Cache.get(2);

    public DiploidExactAFCalculator() {
    }

    @Override
    protected AFCalculationResult computeLog10PNonRef(final VariantContext vc, final int defaultPloidy,
                                                      final double[] log10AlleleFrequencyPriors, final StateTracker stateTracker) {
        final int numAlternateAlleles = vc.getNAlleles() - 1;

        final ArrayList<double[]> genotypeLikelihoods = getGLs(vc.getGenotypes(), true);
        final int numSamples = genotypeLikelihoods.size()-1;
        final int numChr = 2*numSamples;

        // queue of AC conformations to process
        final LinkedList<ExactACset> ACqueue = new LinkedList<>();

        // mapping of ExactACset indexes to the objects
        final HashMap<ExactACcounts, ExactACset> indexesToACset = new HashMap<>(numChr+1);

        // add AC=0 to the queue
        final int[] zeroCounts = new int[numAlternateAlleles];
        ExactACset zeroSet = new ExactACset(numSamples+1, new ExactACcounts(zeroCounts));
        ACqueue.add(zeroSet);
        indexesToACset.put(zeroSet.getACcounts(), zeroSet);

        while ( !ACqueue.isEmpty() ) {
            stateTracker.incNEvaluations(); // keep track of the number of evaluations

            // compute log10Likelihoods
            final ExactACset set = ACqueue.remove();

            calculateAlleleCountConformation(set, genotypeLikelihoods, numChr, ACqueue,
                    indexesToACset, log10AlleleFrequencyPriors,stateTracker);

            // clean up memory
            indexesToACset.remove(set.getACcounts());
            //if ( DEBUG )
            //    System.out.printf(" *** removing used set=%s%n", set.ACcounts);
        }

        return getResultFromFinalState(vc, log10AlleleFrequencyPriors, stateTracker);
    }


    @Override
    protected GenotypesContext reduceScopeGenotypes(final VariantContext vc, final int defaultPloidy, final List<Allele> allelesToUse) {
        return GATKVariantContextUtils.subsetDiploidAlleles(vc, allelesToUse, GATKVariantContextUtils.GenotypeAssignmentMethod.SET_TO_NO_CALL);
    }

    @Override
    protected void reduceScopeCalculateLikelihoodSums(final VariantContext vc, final int defaultPloidy, final LikelihoodSum[] likelihoodSums) {
        final ArrayList<double[]> GLs = getGLs(vc.getGenotypes(), true);
        for ( final double[] likelihoods : GLs ) {
            final int PLindexOfBestGL = MathUtils.maxElementIndex(likelihoods);
            if ( PLindexOfBestGL != PL_INDEX_OF_HOM_REF ) {
                final GenotypeLikelihoods.GenotypeLikelihoodsAllelePair alleles = GenotypeLikelihoods.getAllelePair(PLindexOfBestGL);
                final int alleleLikelihoodIndex1 = alleles.alleleIndex1 - 1;
                final int alleleLikelihoodIndex2 = alleles.alleleIndex2 - 1;
                if ( alleles.alleleIndex1 != 0 )
                    likelihoodSums[alleleLikelihoodIndex1].sum += likelihoods[PLindexOfBestGL] - likelihoods[PL_INDEX_OF_HOM_REF];
                // don't double-count it
                if ( alleles.alleleIndex2 != 0 && alleles.alleleIndex2 != alleles.alleleIndex1 )
                    likelihoodSums[alleleLikelihoodIndex2].sum += likelihoods[PLindexOfBestGL] - likelihoods[PL_INDEX_OF_HOM_REF];
            }
        }
    }

    private static final class DependentSet {
        public final int[] ACcounts;
        public final int PLindex;

        public DependentSet(final int[] ACcounts, final int PLindex) {
            this.ACcounts = ACcounts;
            this.PLindex = PLindex;
        }
    }

    private double calculateAlleleCountConformation(final ExactACset set,
                                                    final ArrayList<double[]> genotypeLikelihoods,
                                                    final int numChr,
                                                    final LinkedList<ExactACset> ACqueue,
                                                    final HashMap<ExactACcounts, ExactACset> indexesToACset,
                                                    final double[] log10AlleleFrequencyPriors,
                                                    final StateTracker stateTracker) {

        //if ( DEBUG )
        //    System.out.printf(" *** computing LofK for set=%s%n", set.ACcounts);

        // compute the log10Likelihoods
        computeLofK(set, genotypeLikelihoods, log10AlleleFrequencyPriors, stateTracker);

        final double log10LofK = set.getLog10Likelihoods()[set.getLog10Likelihoods().length-1];

        // can we abort early because the log10Likelihoods are so small?
        if ( stateTracker.abort(log10LofK, set.getACcounts(), true, false) ) {
            //if ( DEBUG )
            //    System.out.printf(" *** breaking early set=%s log10L=%.2f maxLog10L=%.2f%n", set.ACcounts, log10LofK, maxLog10L);
            return log10LofK;
        }

        // iterate over higher frequencies if possible
        final int ACwiggle = numChr - set.getACsum();
        if ( ACwiggle == 0 ) // all alternate alleles already sum to 2N so we cannot possibly go to higher frequencies
            return log10LofK;

        final int numAltAlleles = set.getACcounts().getCounts().length;

        // add conformations for the k+1 case
        for ( int allele = 0; allele < numAltAlleles; allele++ ) {
            final int[] ACcountsClone = set.getACcounts().getCounts().clone();
            ACcountsClone[allele]++;
            // to get to this conformation, a sample would need to be AB (remember that ref=0)
            final int PLindex = GenotypeLikelihoods.calculatePLindex(0, allele+1);
            updateACset(ACcountsClone, numChr, set, PLindex, ACqueue, indexesToACset, genotypeLikelihoods);
        }

        // add conformations for the k+2 case if it makes sense; note that the 2 new alleles may be the same or different
        if ( ACwiggle > 1 ) {
            final ArrayList<DependentSet> differentAlleles = new ArrayList<>(numAltAlleles * numAltAlleles);
            final ArrayList<DependentSet> sameAlleles = new ArrayList<>(numAltAlleles);

            for ( int allele_i = 0; allele_i < numAltAlleles; allele_i++ ) {
                for ( int allele_j = allele_i; allele_j < numAltAlleles; allele_j++ ) {
                    final int[] ACcountsClone = set.getACcounts().getCounts().clone();
                    ACcountsClone[allele_i]++;
                    ACcountsClone[allele_j]++;

                    // to get to this conformation, a sample would need to be BB or BC (remember that ref=0, so add one to the index)
                    final int PLindex = GenotypeLikelihoods.calculatePLindex(allele_i+1, allele_j+1);
                    if ( allele_i == allele_j )
                        sameAlleles.add(new DependentSet(ACcountsClone, PLindex));
                    else
                        differentAlleles.add(new DependentSet(ACcountsClone, PLindex));
                }
            }

            // IMPORTANT: we must first add the cases where the 2 new alleles are different so that the queue maintains its ordering
            for ( DependentSet dependent : differentAlleles )
                updateACset(dependent.ACcounts, numChr, set, dependent.PLindex, ACqueue, indexesToACset, genotypeLikelihoods);
            for ( DependentSet dependent : sameAlleles )
                updateACset(dependent.ACcounts, numChr, set, dependent.PLindex, ACqueue, indexesToACset, genotypeLikelihoods);
        }

        return log10LofK;
    }

    // adds the ExactACset represented by the ACcounts to the ACqueue if not already there (creating it if needed) and
    // also pushes its value to the given callingSetIndex.
    private void updateACset(final int[] newSetCounts,
                             final int numChr,
                             final ExactACset dependentSet,
                             final int PLsetIndex,
                             final Queue<ExactACset> ACqueue,
                             final HashMap<ExactACcounts, ExactACset> indexesToACset,
                             final ArrayList<double[]> genotypeLikelihoods) {
        final ExactACcounts index = new ExactACcounts(newSetCounts);
        if ( !indexesToACset.containsKey(index) ) {
            ExactACset set = new ExactACset(numChr/2 +1, index);
            indexesToACset.put(index, set);
            ACqueue.add(set);
        }

        // push data from the dependency to the new set
        //if ( DEBUG )
        //    System.out.println(" *** pushing data from " + index + " to " + dependencySet.ACcounts);
        pushData(indexesToACset.get(index), dependentSet, PLsetIndex, genotypeLikelihoods);
    }

    private void computeLofK(final ExactACset set,
                             final ArrayList<double[]> genotypeLikelihoods,
                             final double[] log10AlleleFrequencyPriors, final StateTracker stateTracker) {

        set.getLog10Likelihoods()[0] = 0.0; // the zero case
        final int totalK = set.getACsum();

        // special case for k = 0 over all k
        if ( totalK == 0 ) {
            for ( int j = 1; j < set.getLog10Likelihoods().length; j++ )
                set.getLog10Likelihoods()[j] = set.getLog10Likelihoods()[j-1] + genotypeLikelihoods.get(j)[HOM_REF_INDEX];

            final double log10Lof0 = set.getLog10Likelihoods()[set.getLog10Likelihoods().length-1];
            stateTracker.setLog10LikelihoodOfAFzero(log10Lof0);
            stateTracker.setLog10PosteriorOfAFzero(log10Lof0 + log10AlleleFrequencyPriors[0]);
            return;
        }

        // if we got here, then k > 0 for at least one k.
        // the non-AA possible conformations were already dealt with by pushes from dependent sets;
        // now deal with the AA case (which depends on previous cells in this column) and then update the L(j,k) value
        for ( int j = 1; j < set.getLog10Likelihoods().length; j++ ) {

            if ( totalK < 2*j-1 ) {
                final double[] gl = genotypeLikelihoods.get(j);
                final double conformationValue = MathUtils.Log10Cache.get(2*j-totalK) + MathUtils.Log10Cache.get(2*j-totalK-1) + set.getLog10Likelihoods()[j-1] + gl[HOM_REF_INDEX];
                set.getLog10Likelihoods()[j] = MathUtils.approximateLog10SumLog10(set.getLog10Likelihoods()[j], conformationValue);
            }

            final double logDenominator = MathUtils.Log10Cache.get(2*j) + MathUtils.Log10Cache.get(2*j-1);
            set.getLog10Likelihoods()[j] = set.getLog10Likelihoods()[j] - logDenominator;
        }

        double log10LofK = set.getLog10Likelihoods()[set.getLog10Likelihoods().length-1];

        // update the MLE if necessary
        stateTracker.updateMLEifNeeded(log10LofK, set.getACcounts().getCounts());

        // apply the priors over each alternate allele
        for ( final int ACcount : set.getACcounts().getCounts() ) {
            if ( ACcount > 0 )
                log10LofK += log10AlleleFrequencyPriors[ACcount];
        }

        stateTracker.updateMAPifNeeded(log10LofK, set.getACcounts().getCounts());
    }

    private void pushData(final ExactACset targetSet,
                          final ExactACset dependentSet,
                          final int PLsetIndex,
                          final ArrayList<double[]> genotypeLikelihoods) {
        final int totalK = targetSet.getACsum();

        for ( int j = 1; j < targetSet.getLog10Likelihoods().length; j++ ) {

            if ( totalK <= 2*j ) { // skip impossible conformations
                final double[] gl = genotypeLikelihoods.get(j);
                final double conformationValue =
                        determineCoefficient(PLsetIndex, j, targetSet.getACcounts().getCounts(), totalK) + dependentSet.getLog10Likelihoods()[j-1] + gl[PLsetIndex];
                targetSet.getLog10Likelihoods()[j] = MathUtils.approximateLog10SumLog10(targetSet.getLog10Likelihoods()[j], conformationValue);
            }
        }
    }

    private double determineCoefficient(int PLindex, final int j, final int[] ACcounts, final int totalK) {
        // the closed form representation generalized for multiple alleles is as follows:
        // AA: (2j - totalK) * (2j - totalK - 1)
        // AB: 2k_b * (2j - totalK)
        // AC: 2k_c * (2j - totalK)
        // BB: k_b * (k_b - 1)
        // BC: 2 * k_b * k_c
        // CC: k_c * (k_c - 1)

        // find the 2 alleles that are represented by this PL index
        GenotypeLikelihoods.GenotypeLikelihoodsAllelePair alleles = GenotypeLikelihoods.getAllelePair(PLindex);

        // *** note that throughout this method we subtract one from the alleleIndex because ACcounts ***
        // *** doesn't consider the reference allele whereas the GenotypeLikelihoods PL cache does.   ***

        // the AX het case
        if ( alleles.alleleIndex1 == 0 )
            return MathUtils.Log10Cache.get(2*ACcounts[alleles.alleleIndex2-1]) + MathUtils.Log10Cache.get(2*j-totalK);

        final int k_i = ACcounts[alleles.alleleIndex1-1];

        // the hom var case (e.g. BB, CC, DD)
        final double coeff;
        if ( alleles.alleleIndex1 == alleles.alleleIndex2 ) {
            coeff = MathUtils.Log10Cache.get(k_i) + MathUtils.Log10Cache.get(k_i - 1);
        }
        // the het non-ref case (e.g. BC, BD, CD)
        else {
            final int k_j = ACcounts[alleles.alleleIndex2-1];
            coeff = LOG10_OF_2 + MathUtils.Log10Cache.get(k_i) + MathUtils.Log10Cache.get(k_j);
        }

        return coeff;
    }

    @Override
    public GenotypesContext subsetAlleles(final VariantContext vc,
                                          final int defaultPloidy,
                                          final List<Allele> allelesToUse,
                                          final boolean assignGenotypes) {
        if (defaultPloidy != 2)
            throw new IllegalArgumentException("cannot support ploidy different than 2 and the default ploidy is " + defaultPloidy);
        return allelesToUse.size() == 1
                ? GATKVariantContextUtils.subsetToRefOnly(vc, 2)
                : GATKVariantContextUtils.subsetDiploidAlleles(vc, allelesToUse,
                assignGenotypes ? GATKVariantContextUtils.GenotypeAssignmentMethod.USE_PLS_TO_ASSIGN : GATKVariantContextUtils.GenotypeAssignmentMethod.SET_TO_NO_CALL);
    }
}

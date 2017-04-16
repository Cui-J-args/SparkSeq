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
package org.ncic.bioinfo.sparkseq.algorithms;

import htsjdk.samtools.SAMSequenceDictionary;
import org.ncic.bioinfo.sparkseq.algorithms.adapters.IndelRealignAdapter;
import org.ncic.bioinfo.sparkseq.algorithms.utils.GenomeLocParser;
import org.ncic.bioinfo.sparkseq.algorithms.walker.AbstractTestCase;
import org.ncic.bioinfo.sparkseq.data.common.ReadGroupInfo;
import org.ncic.bioinfo.sparkseq.data.common.RefContigInfo;
import org.ncic.bioinfo.sparkseq.data.common.SamHeaderInfo;
import org.ncic.bioinfo.sparkseq.data.partition.FastaPartition;
import org.ncic.bioinfo.sparkseq.data.partition.SamRecordPartition;
import org.ncic.bioinfo.sparkseq.data.partition.VcfRecordPartition;
import org.ncic.bioinfo.sparkseq.debug.Dumper;
import org.ncic.bioinfo.sparkseq.transfer.SAMSequenceDictTransfer;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: wbc
 */
public class TestDebug extends AbstractTestCase {

    /*public void testDebug() {
        RefContigInfo refContigInfo = RefContigInfo.apply(getClass().getResource("/human_g1k_v37.dict").getFile());
        SamHeaderInfo headerInfo = SamHeaderInfo.sortedHeader(refContigInfo, null);
        headerInfo.addReadGroupInfo(ReadGroupInfo.apply("SRR504516", "sample1"));

        SamRecordPartition samRecordPartition = Dumper.dedumpSamRecordPartition("test_result/0_sam_dedupedSam", refContigInfo);
        FastaPartition fastaPartition = Dumper.deDumpRefPartition("test_result/0_ref");
        VcfRecordPartition indel1000G = Dumper.deDumpRODPartitions("test_result/0_rod_1000G_phase1.indels", refContigInfo);
        VcfRecordPartition dbsnp = Dumper.deDumpRODPartitions("test_result/0_rod_dbsnp", refContigInfo);
        VcfRecordPartition millsIndel = Dumper.deDumpRODPartitions("test_result/0_rod_Mills_and_1000G_gold_standard.indels", refContigInfo);

        List<VcfRecordPartition> rods = new ArrayList<>();
        rods.add(indel1000G);
        rods.add(millsIndel);
        rods.add(dbsnp);
        IndelRealignAdapter.realign(refContigInfo, samRecordPartition, fastaPartition, rods);
    }*/
}

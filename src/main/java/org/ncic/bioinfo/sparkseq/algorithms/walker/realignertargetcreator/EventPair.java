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
package org.ncic.bioinfo.sparkseq.algorithms.walker.realignertargetcreator;

import org.ncic.bioinfo.sparkseq.algorithms.utils.GenomeLoc;

import java.util.TreeSet;

/**
 * Author: wbc
 */
public class EventPair {
    public Event left, right;
    public TreeSet<GenomeLoc> intervals = new TreeSet<GenomeLoc>();

    public EventPair(Event left, Event right) {
        this.left = left;
        this.right = right;
    }

    public EventPair(Event left, Event right, TreeSet<GenomeLoc> set1, TreeSet<GenomeLoc> set2) {
        this.left = left;
        this.right = right;
        intervals.addAll(set1);
        intervals.addAll(set2);
    }
}
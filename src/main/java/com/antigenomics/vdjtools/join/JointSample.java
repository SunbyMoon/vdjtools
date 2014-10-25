/**
 * Copyright 2014 Mikhail Shugay (mikhail.shugay@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.antigenomics.vdjtools.join;

import com.antigenomics.vdjtools.Clonotype;
import com.antigenomics.vdjtools.intersection.IntersectionUtil;
import com.antigenomics.vdjtools.sample.Sample;

import java.util.*;

public class JointSample implements Iterable<JointClonotype> {
    private final Sample[] samples;
    private final double[] intersectionFreq;
    private final double[][] intersectionFreqMatrix;
    private final int[] intersectionDiv;
    private final int[][] intersectionDivMatrix;
    private final List<JointClonotype> jointClonotypes;
    private final double totalMeanFreq, minMeanFreq;
    private final int numberOfSamples;
    private final IntersectionUtil intersectionUtil;

    public JointSample(IntersectionUtil intersectionUtil, Sample[] samples) {
        this(intersectionUtil, samples, new OccurenceJoinFilter());
    }

    public JointSample(IntersectionUtil intersectionUtil, Sample[] samples, JoinFilter joinFilter) {
        this.numberOfSamples = samples.length;
        this.samples = samples;
        this.intersectionDiv = new int[numberOfSamples];
        this.intersectionFreq = new double[numberOfSamples];
        this.intersectionFreqMatrix = new double[numberOfSamples][numberOfSamples];
        this.intersectionDivMatrix = new int[numberOfSamples][numberOfSamples];
        this.intersectionUtil = intersectionUtil;

        Map<String, JointClonotype> clonotypeMap = new HashMap<>();
        int sampleIndex = 0;
        for (Sample sample : samples) {
            for (Clonotype clonotype : sample) {
                String key = intersectionUtil.generateKey(clonotype);

                JointClonotype jointClonotype = clonotypeMap.get(key);

                if (jointClonotype == null) {
                    clonotypeMap.put(key, jointClonotype = new JointClonotype(key, this));
                }

                jointClonotype.addVariant(clonotype, sampleIndex);
            }
            sampleIndex++;
        }

        this.jointClonotypes = new ArrayList<>(clonotypeMap.size() / 2);

        double totalMeanFreq = 0, minMeanFreq = 1;
        for (JointClonotype jointClonotype : clonotypeMap.values()) {
            if (joinFilter.pass(jointClonotype)) {
                jointClonotypes.add(jointClonotype);

                double meanFreq = jointClonotype.getGeomeanFreq();
                totalMeanFreq += meanFreq;
                minMeanFreq = Math.min(minMeanFreq, meanFreq);

                for (int i = 0; i < numberOfSamples; i++) {
                    if (jointClonotype.present(i)) {
                        double freq1 = jointClonotype.getFreq(i);
                        intersectionFreq[i] += freq1;
                        intersectionDiv[i]++;
                        for (int j = i + 1; j < numberOfSamples; j++) {
                            if (jointClonotype.present(j)) {
                                double freq2 = jointClonotype.getFreq(j);
                                intersectionFreqMatrix[i][j] += Math.sqrt(freq1 * freq2);
                                intersectionDivMatrix[i][j]++;
                            }
                        }
                    }
                }
            }
        }
        this.totalMeanFreq = totalMeanFreq;
        this.minMeanFreq = minMeanFreq;

        Collections.sort(jointClonotypes);
    }

    public int getNumberOfSamples() {
        return numberOfSamples;
    }

    public Sample getSample(int sampleIndex) {
        return samples[sampleIndex];
    }

    public int size() {
        return jointClonotypes.size();
    }

    public JointClonotype getAt(int index) {
        return jointClonotypes.get(index);
    }

    public int getIntersectionDiv(int sampleIndex) {
        return intersectionDiv[sampleIndex];
    }

    public int getIntersectionDiv(int sampleIndex1, int sampleIndex2) {
        return sampleIndex1 < sampleIndex2 ? intersectionDivMatrix[sampleIndex1][sampleIndex2] :
                intersectionDivMatrix[sampleIndex2][sampleIndex1];
    }

    public double getIntersectionFreq(int sampleIndex) {
        return intersectionFreq[sampleIndex];
    }

    public double getIntersectionFreq(int sampleIndex1, int sampleIndex2) {
        return sampleIndex1 < sampleIndex2 ? intersectionFreqMatrix[sampleIndex1][sampleIndex2] :
                intersectionFreqMatrix[sampleIndex2][sampleIndex1];
    }

    public IntersectionUtil getIntersectionUtil() {
        return intersectionUtil;
    }

    double getTotalMeanFreq() {
        return totalMeanFreq;
    }

    double getMinMeanFreq() {
        return minMeanFreq;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JointSample that = (JointSample) o;

        return Arrays.equals(samples, that.samples);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(samples);
    }

    @Override
    public Iterator<JointClonotype> iterator() {
        return jointClonotypes.iterator();
    }
}

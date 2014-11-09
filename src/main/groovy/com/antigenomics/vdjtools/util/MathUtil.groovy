/**
 Copyright 2014 Mikhail Shugay (mikhail.shugay@gmail.com)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.antigenomics.vdjtools.util

class MathUtil {
    // todo: to complex math
    public static double JSD(double[] pArr, double[] qArr) {
        int n = pArr.length

        if (n != qArr.length)
            throw new Exception("Input histograms must be of same length")

        double pSum = 0, qSum = 0

        for (int i = 0; i < n; i++) {
            pSum += pArr[i]
            qSum += qArr[i]
        }

        double jsd = 0
        for (int i = 0; i < n; i++) {
            double p = pArr[i] / pSum, q = qArr[i] / qSum,
                   m = (p + q) / 2.0
            jsd += (p > 0 ? (Math.log(p / m) * p) : 0d) + (q > 0 ? (Math.log(q / m) * q) : 0d)
        }


        jsd / 2.0 / Math.log(2.0)
    }
}

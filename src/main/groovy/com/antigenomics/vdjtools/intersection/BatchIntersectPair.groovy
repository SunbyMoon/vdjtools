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

package com.antigenomics.vdjtools.intersection

import com.antigenomics.vdjtools.CommonUtil
import com.antigenomics.vdjtools.Software
import com.antigenomics.vdjtools.sample.SampleCollection
import com.antigenomics.vdjtools.sample.SamplePair
import groovyx.gpars.GParsPool

import java.util.concurrent.atomic.AtomicInteger

def cli = new CliBuilder(usage: "BatchIntersectPair [options] sample_metadata_file output_prefix")
cli.h("display help message")
cli.S(longOpt: "software", argName: "string", required: true, args: 1,
        "Software used to process RepSeq data. Currently supported: ${Software.values().join(", ")}")

def opt = cli.parse(args)

if (opt == null)
    System.exit(-1)

if (opt.h || opt.arguments().size() < 2) {
    cli.usage()
    System.exit(-1)
}

def software = Software.byName(opt.S), inputFileName = opt.arguments()[0], outputFileName = opt.arguments()[1]

def scriptName = getClass().canonicalName.split("\\.")[-1]

//
// Batch load all samples
//

println "[${new Date()} $scriptName] Reading samples"

def sampleCollection = new SampleCollection(inputFileName, software, false, false)

println "[${new Date()} $scriptName] ${sampleCollection.size()} samples loaded"

//
// Do intersection in parallel
//

IntersectionType.values().each { IntersectionType intersectionType ->
    println "[${new Date()} $scriptName] Intersecting by $intersectionType"
    def intersectionUtil = new IntersectionUtil(intersectionType)
    def pairs = sampleCollection.listPairs(), results
    def counter = new AtomicInteger()
    GParsPool.withPool CommonUtil.THREADS, {
        results = pairs.collectParallel { SamplePair pair ->
            def pairedIntersection = intersectionUtil.generatePairedIntersection(pair, false)
            println "[${new Date()} $scriptName] " +
                    "Intersected ${counter.incrementAndGet()} of ${pairs.size()} so far\n" +
                    "Last result\n${PairedIntersection.HEADER}\n$pairedIntersection"
            pairedIntersection
        }
    }

    println "[${new Date()} $scriptName] Writing results"
    new File(outputFileName + "_" + intersectionType.shortName + ".txt").withPrintWriter { pw ->
        pw.println("#" +
                [PairedIntersection.HEADER,
                 sampleCollection.metadataHeader.collect { "1_$it" },
                 sampleCollection.metadataHeader.collect { "2_$it" }  ].flatten().join("\t"))
        results.each { PairedIntersection pairedIntersection ->
            pw.println(pairedIntersection.toString() + "\t" +
                    pairedIntersection.sample1.metadata.toString() + "\t" +
                    pairedIntersection.sample2.metadata.toString())
        }
    }
}
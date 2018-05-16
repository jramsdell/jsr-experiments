package embedding

import koma.pow
import org.apache.lucene.index.Term
import stochastic.KotlinStochasticIntegrator
import utils.AnalyzerFunctions
import utils.getIndexSearcher
import utils.normalize
import java.io.File
import java.lang.Double.sum
import java.lang.Math.abs
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis


/**
 * Class: KotlinEmbedding
 * Desc: Used to embed paragraphs into a space of topics.
 */
class KotlinEmbedding(indexLoc: String) {
    private val totalFreqs = HashMap<String, Double>()
    val searcher = getIndexSearcher(indexLoc)
    private val kernelAnalyzer =
                KotlinKernelAnalyzer(0.0, 1.0, corpus = {null}, partitioned = true)

    var topicTime: Long = 0

    // Ignore, used in debugging
    fun query(query: String, nQueries: Int = 10): List<String> {
        val boolQuery = AnalyzerFunctions.createQuery(query)
        val results = searcher.search(boolQuery, nQueries)
        return results.scoreDocs.map { scoreDoc ->
            val doc = searcher.doc(scoreDoc.doc)
            doc.get("text")
        }
    }

    // Ignore, used in debugging
    fun scoreResults(combined: TopicMixtureResult, docs: List<Pair<String, TopicMixtureResult>>) {
        docs.map { doc -> doc to doc.second.deltaSim(combined) }
            .sortedBy { (_, distance) -> distance }
            .forEach { (doc, distance) -> println("${doc.first}:\n\t $distance") }
    }

    // Ignore, used in debugging
    fun reportQueryResults(queryString: String, smoothDocs: Boolean = false, smoothCombined: Boolean = false) {
        val docs = query(queryString)

        val total = docs.joinToString("\n")
        val results = ArrayList<Pair<String, TopicMixtureResult>>()

        docs.forEach { doc ->
            val result = embed(doc, nSamples = 1000, nIterations = 400, smooth = smoothDocs)
            result.reportResults()
            results += doc to result
            println(doc + "\n\n")
        }

        val otherText = """
            During Microsoft's early history, Gates was an active software developer, particularly in the company's programming language products, but his basic role in most of the company's history was primarily as a manager and executive. Gates has not officially been on a development team since working on the TRS-80 Model 100,[65] but as late as 1989 he wrote code that shipped with the company's products.[63] He remained interested in technical details; in 1985, Jerry Pournelle wrote that when he watched Gates announce Microsoft Excel, "Something else impressed me. Bill Gates likes the program, not because it's going to make him a lot of money (although I'm sure it will do that), but because it's a neat hack."[66]
            """
        results += otherText to embed(otherText, nSamples = 1000, nIterations = 400, smooth = smoothDocs)

        println("COMBINED")
        val result = embed(total, nSamples = 1000, nIterations = 400, smooth = smoothCombined)
        result.reportResults()
        println("\n\n")

        println("QUERY")
        embed(queryString, nSamples = 1000, nIterations = 400, smooth = false).reportResults()
        println("\n\n")
        scoreResults(result, results)
    }


    /**
     * Func: loadtopics
     * Desc: Loads directory of topics (paragraph/). Each subdirectory is named after a topic, and contains
     *       abstracts. This is what is used to construct the topic distributions.
     *
     *       This is in severe need of simplification (I don't really need kernelAnalyzer anymore)
     */
    fun loadTopics(directory: String, filterList: List<String> = listOf(), smooth: Boolean = false) {
        val time = measureTimeMillis {

            // For each paragraph, get unigram frequencies.
            kernelAnalyzer.analyzeTopicDirectories(directory, filterList, smooth)
            kernelAnalyzer.normalizeTopics()
            if (smooth) {
                kernelAnalyzer.topics.values.forEach { it.equilibriumCovariance() }
            }

            // merge all of the paragraph unigram frequencies and normalize to get final topic distribution over words
            kernelAnalyzer.topics.forEach { topic ->
                topic.value.getKernelFreqs().forEach { (k,v) -> totalFreqs.merge(k, v, ::sum)}
            }

            val total = totalFreqs.values.sum()
            totalFreqs.forEach { k, v -> totalFreqs[k] = v/total  }
        }
        topicTime = time
    }

    // Ignore: used for debugging / research
    fun loadQueries(queryString: String, filterList: List<String> = listOf(), smooth: Boolean = false, nQueries: Int = 5) {
        val results = query(queryString, nQueries)
        results.mapIndexed { index, s -> kernelAnalyzer.createTopicFromParagraph("$index", s, smooth)  }
        kernelAnalyzer.normalizeTopics()
    }

    // Ignore: used for debugging / research
    fun expandQueryText(queryString: String, nQueries: Int = 5) =
        AnalyzerFunctions.createTokenList(queryString)
            .map { query(it, nQueries).joinToString("\n") }
            .joinToString()


    /**
     * Func: runEmbedding
     * Desc: Given a distribution representing a paragraph/query, find embedding in space of topics.
     */
    private fun runEmbedding(kernelDist: KernelDist, text: String, nSamples: Int = 300, nIterations: Int = 500,
                             smooth: Boolean = false): Triple<List<String>, List<Double>, Double> {

        val identityFreqs = "identity" to kernelDist.getKernelFreqs()
        val samples = kernelDist.perturb(nSamples)

        val topicStats = kernelAnalyzer.retrieveTopicFrequencies() + identityFreqs
        val stochasticIntegrator = KotlinStochasticIntegrator(samples, topicStats, corpus = {_ -> null})
        val integrals = stochasticIntegrator.integrate()

        return kernelAnalyzer.classifyByDomainSimplex2(integrals, nIterations = nIterations,smooth = false)
    }

    /**
     * Func: embed
     * Desc: Given a distribution representing a paragraph/query, find embedding in space of topics.
     */
    fun embed(text: String, nSamples: Int = 300, nIterations: Int = 500, smooth: Boolean = false,
              letterGram: Boolean = false): TopicMixtureResult {
        val kernelDist = KernelDist(0.0, 20.0)
            .apply { analyzePartitionedDocument(text, letterGram = letterGram) }
            .apply { normalizeKernels() }
        if (smooth) {
            kernelDist.equilibriumCovariance()
        }


        // Since the results can vary a lot, I run the embedding a few times and average the results.
        // Although right now I'm currently not...
        val nTimes = 1
        val (names, weights, kld) =
                (0 until nTimes)
                    .map { runEmbedding(kernelDist, text, nSamples, nIterations, smooth) }
                    .reduce { (names, accWeights, accKld), (_, nextWeights, nextKld) ->
                        val newWeights = accWeights.zip(nextWeights).map { (w1, w2) -> w1 + w2 }
                        Triple(names, newWeights, accKld + nextKld) }
                    .let { (names, weights, kld) ->
                        val finalWeights = weights.map { weight -> weight / nTimes.toDouble() }
                        Triple(names, finalWeights, kld / nTimes)}


        val results = names.zip(weights).toMap()
        val topicResult =  TopicMixtureResult(results.toSortedMap(), kld)
        topicResult.reportResults()
        topicResult.fitLog()
//        topicResult.fitDecay()
        topicResult.reportResults()
        val base = kernelDist.getKernelFreqs()
        val mixed = mix(topicResult, kernelAnalyzer.retrieveTopicFrequencies().toMap())

        var tt = HashMap<String, Double>()
        val t1 = kernelAnalyzer.retrieveTopicFrequencies().filter { it.first in listOf("Computers", "Travel", "Games") }
            .forEach { (_, values) ->
                values.forEach { k, v -> tt.merge(k, v, ::sum)  }
            }
        val tt2 = tt.toMap().normalize()
//        val perturbations = kernelDist.perturb(1, pmean = 1.0, psd = 0.00001)

//        kernelAnalyzer.retrieveTopicFrequencies().forEach { (t, f) ->
//            println(t)
//            val difference = (perturbations.first.zip(perturbations.second))
//                .sumByDouble { (name, values) ->
////                    values.sumByDouble { v -> (v - (f[name] ?: 1/kernelDist.kernels.size.toDouble())).pow(2.0) }
//                    values.sumByDouble { v -> (v - (f[name] ?: 0.0)).absoluteValue }
//                }
//            println("DIFF is: $difference\n")
//        }

//        kernelAnalyzer.retrieveTopicFrequencies().forEach { (t, f) ->
//            println(t)
//            val difference = base.entries.sumByDouble { (k,v) ->
//                //            abs(v - (mixed[k] ?: 0.0))
//                (v - (f[k] ?: 0.0)).absoluteValue
//            }
//            println("DIFF is: $difference\n")
//        }
//
//        println("COMBINED")
//        val difference = (perturbations.first.zip(perturbations.second))
//            .sumByDouble { (name, values) ->
////                values.sumByDouble { v -> (v - (mixed[name] ?: 1/kernelDist.kernels.size.toDouble())).pow(2.0) }
//                values.sumByDouble { v -> (v - (mixed[name] ?: 0.0)).absoluteValue }
//            }
//        println("DIFF is: $difference\n")

        println("COMBINED")
        val difference = base.entries.sumByDouble { (k,v) ->
//            abs(v - (mixed[k] ?: 0.0))
            (v - (mixed[k] ?: 0.0)).absoluteValue
        }
        println("DIFF is: $difference\n")

        println("T1")
        val difference2 = base.entries.sumByDouble { (k,v) ->
            //            abs(v - (mixed[k] ?: 0.0))
            (v - (tt2[k] ?: 0.0)).absoluteValue
        }
        println("DIFF is: $difference2\n")

        return topicResult
    }

    fun mix(mixResult: TopicMixtureResult, freqs: Map<String, Map<String, Double>>): Map<String, Double> {
        val combined = HashMap<String, Double>()
        mixResult.results
            .filter { it.value > 0.0 }
            .forEach { topicName, weight ->
            freqs[topicName]!!.forEach { word, freq ->
                combined.merge(word, freq * weight, ::sum)
            }
        }
        return combined.normalize()
    }
}



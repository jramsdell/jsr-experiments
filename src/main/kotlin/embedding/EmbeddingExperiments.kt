package embedding

import org.apache.lucene.search.TermQuery
import java.io.File

/**
 * Fair warning: this is used to test my embedding methods and is not used to produce the runfiles.
 * I left it in here because I still need to use it to test variations and to make sure nothing breaks.
 * It's goind to be messy...
 */

fun testQuery() {
//    embedder.reportQueryResults("Infinitesimal calculus", smoothDocs = false, smoothCombined = false)
//    embedder.reportQueryResults("Variational calculus and bayes theorem", smoothDocs = false, smoothCombined = false)

//    embedder.reportQueryResults("Health benefits of chocolate", smoothDocs = false, smoothCombined = false)

//    embedder.reportQueryResults("Microsoft", smoothDocs = false, smoothCombined = false)
//    embedder.reportQueryResults("Laplace operator", smoothDocs = false, smoothCombined = false)

}

/**
 * Func: testBasisParagraphs
 * Desc: For the paragraphs used to create the topics, it should be the case that if you embedd the paragraphs, that
 *       they are mostly in the topics they created. If I mix (concatenate) these paragraphs, it should be the case that
 *       their embedded coordinates are inbetween the coordinates of the original paragraphs.
 */
fun testBasisParagraphs(embedder: KotlinEmbedding): TopicMixtureResult  {
    val res = "resources/paragraphs"
    val testText =
            File("$res/Computers/doc_1.txt").readText() +
//                    File("$res/Travel/doc_3.txt").readText() +
                    File("$res/Travel/doc_5.txt").readText() +
//                    File("$res/Medicine/doc_3.txt").readText() +
                    File("$res/Games/doc_3.txt").readText()

//    val testText =
//            File("paragraphs/Biology/doc_0.txt").readText() +
//                    File("paragraphs/People/doc_0.txt").readText()
    return embedder.embed(testText, nSamples = 500, nIterations = 2000, smooth = false)
}



/**
 * Func: testText
 * Desc: I just copy/paste sections from Wikipedia to see how they are embedded and if it makes sense.
 */
fun testText(embedder: KotlinEmbedding): TopicMixtureResult {
    val text = """
        A party is a gathering of people who have been invited by a host for the purposes of socializing, conversation, recreation, or as part of a festival or other commemoration of a special occasion. A party will typically feature food and beverages, and often music and dancing or other forms of entertainment. In many Western countries, parties for teens and adults are associated with drinking alcohol such as beer, wine or distilled spirits.
        """


    return embedder.embed(text, nSamples = 1000, nIterations = 1000, smooth = false)
}




fun main(args: Array<String>) {
    val indexLoc = "/home/hcgs/data_science/index"
    val embedder = KotlinEmbedding(indexLoc)
    embedder.loadTopics("resources/paragraphs/")


//    unpack(embedder)

//    testBasisParagraphs(embedder).reportResults()
    testBasisParagraphs(embedder)
//    testText(embedder)

    val myquery = "Arachnophobia signs and symptoms"
//    val queryResults = embedder.query(myquery, 100).mapIndexed { index, s -> index.toString() to s  }.toMap()
//
//    embedder.loadQueries(myquery, nQueries = 100)
//    val expanded = embedder.expandQueryText(myquery, 10)
//
//    val results = embedder.embed(expanded, nSamples = 3000, nIterations = 100, smooth = false)
//    results.results.entries.sortedByDescending { it.value }
//        .forEach { (k,v) -> println("${queryResults[k]}:\n\t$v") }

//    println(expanded)


//    testBasisParagraphs(embedder).reportResults()
//    testText(embedder).reportResults()



}

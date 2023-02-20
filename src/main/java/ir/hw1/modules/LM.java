/* Modified from lucene Demo Searcher*/
package ir.hw1.modules;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.FSDirectory;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
/** Simple command-line based search demo. */
public class LM {

    static final String spechar = "[\\[+\\]+:{}^~?\\\\/()><=\"!*-]";
    public LM() {}

    public static void run(String[] args) throws Exception {
        String usage =
                "Usage:\tjava -jar HW1.jar LM [IndexPath] [QueriesPath] [Output]";

        String index = "testdata/index";
        String field = "TEXT";
        String output = "testdata/BM25_results.txt"; // = "testdata/BM25_results.txt"
        String queries = "testdata/eval/topics.351-400";//"testdata/queries.txt";
        System.out.println(args.length);
        if (args.length<4){
            System.out.println(usage);
            System.exit(0);
        } else {
            index = args[1];
            queries = args[2];
            output = args[3];
        }


        boolean hwformat = true;

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new StandardAnalyzer();

        List<String[]> queryparsed = ParseQuery.parse(queries);

        QueryParser parser = new QueryParser(field, analyzer);

        BufferedWriter outWriter = null;
        try {
            FileWriter fstream = new FileWriter(output, false);
            outWriter = new BufferedWriter(fstream);
            for (String[] topic: queryparsed) {
                String topicID = topic[0];
                String line = topic[1];

                line = line.replaceAll(spechar, "\\\\$0");
                Query query = parser.parse(line);
                //            System.out.println(query);

                doSearch(searcher, query, hwformat, topicID, outWriter);
                System.out.println("Finish Quering Topic " + topicID);

            }
            // close after writing
            if(outWriter != null) {
                outWriter.close();
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        reader.close();
    }

    /**
     * This demonstrates a typical paging search scenario, where the search engine presents
     * pages of size n to the user. The user can then go to the next page if interested in
     * the next hits.
     *
     * When the query is executed for the first time, then only enough results are collected
     * to fill 5 result pages. If the user wants to page beyond this limit, then the query
     * is executed another time and all hits are collected.
     *
     */
    public static void doSearch(IndexSearcher searcher, Query query, boolean hwformat
            ,String topicID, BufferedWriter outWriter) throws IOException {

        // Collect enough docs to show 5 pages
        searcher.setSimilarity(new LMDirichletSimilarity());
        TopDocs results = searcher.search(query, 1000);
        ScoreDoc[] hits = results.scoreDocs;

        int numTotalHits = Math.toIntExact(results.totalHits.value);
//        System.out.println(numTotalHits + " total matching documents");

        int start = 0;
        int end = Math.min(numTotalHits, 1000);;


        for (int i = start; i < end; i++) {
            Document doc = searcher.doc(hits[i].doc);
            String DOCNO = doc.get("DOCNO");
            if (DOCNO != null) {
                if (hwformat) {
                    outWriter.write(topicID +"\tQ0\t" + DOCNO +"\t"+ (i+1) +"\t"+ hits[i].score +"\t"+ "chua259\n");
                    outWriter.flush();
                } else {
                    System.out.println((i+1) + ".\t" + DOCNO + "\t" + doc.get("Path"));
                }
//                String title = doc.get("TITLE");
            }
        }

    }
}
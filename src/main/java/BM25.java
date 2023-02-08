/* Modified from lucene Demo Searcher*/
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

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
import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.search.similarities.BM25Similarity;
/** Simple command-line based search demo. */
public class BM25 {

    private BM25() {}

    public static void main(String[] args) throws Exception {
        String usage =
                "Usage:\tjava -cp HW1.jar [-index dir] [-queries file] [-output output] [-query string]";
        if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
            System.out.println(usage);
            System.exit(0);
        }
        String index = "testdata/index";
        String field = "TEXT";
        String output = "testdata/BM25_results.txt";
        String queries = "testdata/queries.txt";
        boolean hwformat = true;
        int topicID = 350;
        String queryString = null;

        for(int i = 0;i < args.length;i++) {
            if ("-index".equals(args[i])) {
                index = args[i+1];
                i++;
            } else if ("-field".equals(args[i])) {
                field = args[i+1];
                i++;
            } else if ("-queries".equals(args[i])) {
                queries = args[i+1];
                i++;
            } else if ("-query".equals(args[i])) {
                queryString = args[i+1];
                i++;
            }
        }

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new StandardAnalyzer();

        BufferedReader in = null;
        if (queries != null) {
            in = Files.newBufferedReader(Paths.get(queries), StandardCharsets.UTF_8);
        }
        QueryParser parser = new QueryParser(field, analyzer);

        String spechar = "[\\[+\\]+:{}^~?\\\\/()><=\"!]";
        BufferedWriter outWriter = null;
        try {
            FileWriter fstream = new FileWriter(output, false);
            outWriter = new BufferedWriter(fstream);
            while (true) {
                String line = queryString != null ? queryString : in.readLine();
                if (line == null || line.length() == -1) {
                    break;
                }
                line = line.trim();
                if (line.length() == 0) {
                    break;
                }
                line = line.replaceAll(spechar, "\\\\$0");
                Query query = parser.parse(line);
                //            System.out.println(query);
                topicID++;
                doSearch(searcher, query, hwformat, topicID, outWriter);
                System.out.println("Finish Quering Topic " + topicID);
                if (queryString != null) {
                    break;
                }
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
            ,int topicID, BufferedWriter outWriter) throws IOException {

        // Collect enough docs to show 5 pages
        searcher.setSimilarity(new BM25Similarity((float)1.2,(float)0.75));
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
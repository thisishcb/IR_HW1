/* Modified from lucene Demo Searcher*/

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

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.*;
import java.util.stream.Collectors;



/** Simple command-line based search demo. */
public class RM1 {

    private RM1() {}

    public static void main(String[] args) throws Exception {
        String usage =
                "Usage:\tjava -cp HW1.jar [-index dir] [-queries file] [-output output] [-query string]";
        if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
            System.out.println(usage);
            System.exit(0);
        }
        String index = "testdata/index_store";
        String field = "TEXT";
        String output = "testdata/RM1_results.txt";
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
        IndexSearcher initsearcher = new IndexSearcher(reader);
        initsearcher.setSimilarity(new LMDirichletSimilarity());
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
                Query query = parser.parse(line); // boolean query
                //            System.out.println(query);
                topicID++;
                doSearch(initsearcher, query, hwformat, topicID, outWriter);
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

        TopDocs results = searcher.search(query, 1000);
        ScoreDoc[] hits = results.scoreDocs;

        List<String> query_exp = queryExpansion(results, searcher);
//        System.out.println(query_exp);
        /*
        TODO:
         expand the original query
         boolean builder
         re-rank the results - just edit ScoreDoc
         */

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

    static List<String> queryExpansion(TopDocs results, IndexSearcher searcher) throws IOException {
        // frequent table to find the top tables
        Map<String, Integer> termFrequency = new HashMap<>();
        int cnter = 0;
        for (ScoreDoc primscdoc : results.scoreDocs) {
            if (cnter>50) {break;}
            Document primdoc = searcher.doc(primscdoc.doc);
            String[] terms = primdoc.get("TEXT").split(" ");
            for (String term : terms) {
                termFrequency.put(term
                        , termFrequency.getOrDefault(term,1) + 1);
            }
            cnter++;
        }

        // expand the query with the top50 new terms
        List<String> expandedTerms = new ArrayList<>();
        termFrequency.entrySet().stream()
                .sorted(
                        Collections.reverseOrder(
                                Map.Entry.comparingByValue()
                        )
                )
                .limit(50)
                .forEach(entry -> expandedTerms.add(entry.getKey()));
        /*
        Map<String, Integer> sorted = termFrequency.entrySet().stream()
                .sorted(
                        Collections.reverseOrder(
                                Map.Entry.comparingByValue()
                        )
                )
                .limit(50)
                .collect( Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
        System.out.println(sorted);
         */
        return expandedTerms;

    }
}
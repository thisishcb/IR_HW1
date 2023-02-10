/* Modified from lucene Demo Searcher*/
package ir.hw1;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.util.BytesRef;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.*;
import java.util.stream.Collectors;



/** Simple command-line based search demo. */
public class RM1 {

    private RM1() {}

    static final String spechar = "[\\[+\\]+:{}^~?\\\\/()><=\"!*-]";
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

                //            System.out.println(query);
                topicID++;
                doSearch(initsearcher, line, hwformat, topicID, outWriter, parser, reader);
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
    static void doSearch(IndexSearcher searcher, String line, boolean hwformat
            ,int topicID, BufferedWriter outWriter, QueryParser parser, IndexReader reader) throws Exception {

        Query query = parser.parse(line);
        TopDocs results = searcher.search(query, 1000);
        ScoreDoc[] hits = results.scoreDocs;

        List<String> query_exp = queryExpansion(results, searcher);
//        System.out.println(query_exp);
        /*
         expand the original query
         boolean builder
        */
//        BooleanQuery.Builder builder = new BooleanQuery.Builder();
//        builder.add(query, BooleanClause.Occur.SHOULD);
//        builder.add(parser.parse(String.join(" ", query_exp).replaceAll(spechar, "\\\\$0")), BooleanClause.Occur.SHOULD);
        Query expandedQuery = parser.parse(line + " "
                + String.join(" ", query_exp).replaceAll(spechar, "\\\\$0")
        );

        int numTotalHits = Math.toIntExact(results.totalHits.value);
//        System.out.println(numTotalHits + " total matching documents");

        /*
        re-rank the results - just edit ScoreDoc
        * */

        ScoreDoc[] hits_rerank = reRankResult(hits, query
                ,expandedQuery, searcher);


        // print output
        for (int i = 0; i < 1000; i++) {
            Document doc = searcher.doc(hits_rerank[i].doc);
            System.out.println(hits_rerank[i].score);
            String DOCNO = doc.get("DOCNO");
            if (DOCNO != null) {
                outWriter.write(topicID +"\tQ0\t"
                        + DOCNO +"\t"+ (i+1) +"\t"
                        + hits_rerank[i].score +"\t"
                        + "chua259\n");
                outWriter.flush();
                System.out.println(topicID +"\t"
                        + DOCNO +"\t"+ (i+1) +"\t"
                        + hits_rerank[i].score +"\t"
                        + doc.get("Path"));
            }
        }

    }

    static List<String> queryExpansion(TopDocs results, IndexSearcher searcher) throws IOException {
        // frequent table to find the top tables
        Map<String, Integer> termFrequency = new HashMap<>();
        int cnter = 0;
        for (ScoreDoc primscdoc : results.scoreDocs) {
            if (cnter>25) {break;}
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

    static ScoreDoc[] reRankResult(ScoreDoc[] oriDocs, Query query
            ,Query expandedQuery, IndexSearcher searcher
    ) throws Exception {

        //term prioir table for smoothing
        IndexReader idxReader= searcher.getIndexReader();
        long mu = 2000;
        long totalTF = idxReader.getSumTotalTermFreq("TEXT");

        // get all the old query terms
        List<String> oriTerms = new ArrayList<>();
        for (BooleanClause bc : ((BooleanQuery) query).clauses()) {
            Term bcterm = ((TermQuery) bc.getQuery()).getTerm();
            oriTerms.add(bcterm.text());
        }
        // get all the new query terms
        List<String> expTerms = new ArrayList<>();
        for (BooleanClause bc : ((BooleanQuery) expandedQuery).clauses()) {
            Term bcterm = ((TermQuery) bc.getQuery()).getTerm();
            expTerms.add(bcterm.text());
        }
        // iterate through all docs
        ScoreDoc[] reScoreDocs = oriDocs.clone();
        double[] scores = new double[oriDocs.length];

        for (int i=0; i<oriDocs.length;i++) {
            // iterate through docs

            Terms tfv = idxReader
                    .getTermVector(oriDocs[i].doc, "TEXT");
            TermsEnum termsEnum = tfv.iterator();
            BytesRef bytesRef = null;
            long totalCounts = tfv.getSumTotalTermFreq();
            double pqd = 1; // p(q|D)
            while ((bytesRef = termsEnum.next()) != null) {
                String term = bytesRef.utf8ToString();
                if (oriTerms.contains(term)) {
                    long freq = termsEnum.totalTermFreq();
//                    pqd *= (double)freq/totalCounts;
                    Term tm = new Term("TEXT",term);
                    pqd *= (double) (freq+
                            mu*(idxReader.totalTermFreq(tm)/totalTF))
                            /(totalCounts+mu);
                    if(pqd==0){throw new Exception("PQD Become Zero");}
                }
            }
//            System.out.println(pqd);
            //reset iterator; start cal ptd
            termsEnum = tfv.iterator();
            double ptd = 0; // p(t|D)
            while ((bytesRef = termsEnum.next()) != null) {
                String term = bytesRef.utf8ToString();
                if (expTerms.contains(term)) {
                    long freq = termsEnum.totalTermFreq();
//                    ptd += (double)freq/totalCounts;
                    Term tm = new Term("TEXT",term);
                    ptd += (double) (freq+
                            mu*(idxReader.totalTermFreq(tm)/totalTF))
                            /(totalCounts+mu);
                    if(ptd==0){throw new Exception((freq/totalCounts) + "PTD Become Zero");}
                }
            }
            scores[i] = ptd*pqd;
        }
        // normalize score
        double sum = 0.0;
        for (int i = 0; i < scores.length; i++) {
            sum += scores[i];
        }
//        System.out.println(sum);
        if (sum > 0.0d) {
            for (int i = 0; i < scores.length; i++) {
                scores[i] = scores[i] / sum;
            }
        }
//        System.out.println(Arrays.stream(scores).sum());
//        System.out.println(Arrays.stream(scores).max());
//        System.out.println(Arrays.stream(scores).min());
        // assign to docs and re-order
        for (int i=0; i<oriDocs.length;i++) {
            reScoreDocs[i].score = (float)scores[i];
        }
        Arrays.sort(reScoreDocs, (a, b) -> Float.compare(b.score, a.score));
        return reScoreDocs;
    }
}
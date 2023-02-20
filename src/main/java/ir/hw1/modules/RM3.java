/* Modified from lucene Demo Searcher*/
package ir.hw1.modules;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/** Simple command-line based search demo. */
public class RM3 {

    public RM3() {}

    static double lambda = 0.5;
    static final String spechar = "[\\[+\\]+:{}^~?\\\\/()><=\"!*-]";
    public static void run(String[] args) throws Exception {
        String usage =
                "Usage:\tjava -jar HW1.jar RM3 [IndexPath] [QueriesPath] [Output] [lambda]";

        String index = "testdata/index";
        String field = "TEXT";
        String output = "testdata/RM3_test.txt";
        String queries = "testdata/eval/topics.351-400";//"testdata/queries.txt";

        if (args.length<4){
            System.out.println(usage);
            System.exit(0);
        } else {
            index = args[1];
            queries = args[2];
            output = args[3];
        }

        if (args.length==5) {
            double arg4 = Double.parseDouble(args[4]);
            assert (0<=arg4)&&(arg4<=1) ;
            lambda = arg4 ;
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

                doSearch(searcher, line, topicID, outWriter,parser,reader);
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
    static void doSearch(IndexSearcher searcher, String line
            ,String topicID, BufferedWriter outWriter, QueryParser parser, IndexReader reader) throws Exception {

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
//            System.out.println(hits_rerank[i].score);
            String DOCNO = doc.get("DOCNO");
            if (DOCNO != null) {
                outWriter.write(topicID +"\tQ0\t"
                        + DOCNO +"\t"+ (i+1) +"\t"
                        + hits_rerank[i].score +"\t"
                        + "chua259\n");
                outWriter.flush();
                /*
                System.out.println(topicID +"\t"
                        + DOCNO +"\t"+ (i+1) +"\t"
                        + hits_rerank[i].score +"\t"
                        + doc.get("Path"));
                 */
            }
        }

    }

    static List<String> queryExpansion(TopDocs results, IndexSearcher searcher) throws IOException {
        // frequent table to find the top tables
        Map<String, Integer> termFrequency = new HashMap<>();
        // NLTK stop words https://gist.github.com/sebleier/554280
        String[] sw = {"i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now"};
        List<String> stopWords = Arrays.asList(sw);
        int cnter = 0;
        for (ScoreDoc primscdoc : results.scoreDocs) {
            if (cnter>25) {break;}
            Document primdoc = searcher.doc(primscdoc.doc);
            String[] terms = primdoc.get("TEXT").split(" ");
            for (String term : terms) {
                if (stopWords.contains(term)) continue;
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
        List<String> expTerms_wd = new ArrayList<>();
        for (BooleanClause bc : ((BooleanQuery) expandedQuery).clauses()) {
            Term bcterm = ((TermQuery) bc.getQuery()).getTerm();
            expTerms_wd.add(bcterm.text());
        }
        // iterate through all docs
        List<String> expTerms = new ArrayList<String>(new HashSet<String>(expTerms_wd));
        // iterate through all docs
        ScoreDoc[] reScoreDocs = oriDocs.clone();
        double[] scores = new double[oriDocs.length];
        double[] MLEs = new double[oriDocs.length];

        for (int i=0; i<oriDocs.length;i++) {
            // iterate through docs

            Terms tfv = idxReader
                    .getTermVector(oriDocs[i].doc, "TEXT");
            TermsEnum termsEnum = tfv.iterator();
            BytesRef bytesRef = null;
            long totalCounts = tfv.getSumTotalTermFreq();

            Map<String, Double> docTermCount = new HashMap<String, Double>();
            while ((bytesRef = termsEnum.next()) != null) {
                String term = bytesRef.utf8ToString();
                docTermCount.put(term, (double)termsEnum.totalTermFreq());
            }

            double pqd = 1; // p(q|D)
            for (String term: oriTerms) {
                Term tm = new Term("TEXT",term);
                double freq = docTermCount.getOrDefault(term,0.0);
                if (freq > 0) {
//                    pqd *= (double)freq/totalCounts;
                    double tmpCnt = (double)idxReader.totalTermFreq(tm);
                    pqd *= 1.0 + (freq/totalCounts) * (totalTF/tmpCnt);
//                    pqd *= (freq+
//                            mu*(((double)idxReader.totalTermFreq(tm))/totalTF))
//                            /(totalCounts+mu);
                    if(pqd==0){throw new Exception("PQD Become Zero");}
                } else {
                    double tmpCnt = Math.max(freq,1.0);
                    pqd *= 1.0 + (freq/totalCounts) * (totalTF/tmpCnt);
//                    pqd *= ((mu*(tmpCnt/totalTF))
//                            /(totalCounts+mu));
                }
            }
            MLEs[i]=pqd;
//            System.out.println(pqd);
            //reset iterator; start cal ptd
//            termsEnum = tfv.iterator();
            double ptd = 0; // p(t|D)
            for (String term: expTerms) {
                Term tm = new Term("TEXT",term);
                double freq = docTermCount.getOrDefault(term,0.0);
                if (freq > 0) {
//                    pqd *= (double)freq/totalCounts;
                    ptd += (freq+
                            mu*(((double)idxReader.totalTermFreq(tm))/totalTF))
                            /(totalCounts+mu);
                } else {
                    double tmpCnt = Math.max(freq,1.0);
                    ptd += ((mu*(tmpCnt/totalTF))
                            /(totalCounts+mu));
                }
            }
            scores[i] = ptd*pqd;
        }
        // normalize score
        double sum = 0.0;
        double summle = 0.0;
        for (int i = 0; i < scores.length; i++) {
            summle += MLEs[i];
            sum += scores[i];
        }
//        System.out.println(sum);
        if (sum > 0.0d) {
            for (int i = 0; i < scores.length; i++) {
                scores[i] /= sum;
                MLEs[i] /= summle;
            }
        }
        for (int i = 0; i < scores.length; i++) {
            scores[i] = lambda*scores[i] + (1-lambda)*MLEs[i];
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
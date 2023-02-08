/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
Modified from org.apache.lucene.demo IndexFiles
*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.demo.knn.DemoEmbeddings;
import org.apache.lucene.demo.knn.KnnVectorDict;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.IOUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
/**
 * Index all text files under a directory.
 * Indexed 472525 documents in 146970 milliseconds
 *
 * <p>This is a command-line application demonstrating simple Lucene indexing. Run it with no
 * command-line arguments for usage information.
 */
public class IndexFiles implements AutoCloseable {
    static final String KNN_DICT = "knn-dict";

    // Calculates embedding vectors for KnnVector search
    private final DemoEmbeddings demoEmbeddings;
    private final KnnVectorDict vectorDict;

    static Integer docCount=0;

    private IndexFiles(KnnVectorDict vectorDict) throws IOException {
        if (vectorDict != null) {
            this.vectorDict = vectorDict;
            demoEmbeddings = new DemoEmbeddings(vectorDict);
        } else {
            this.vectorDict = null;
            demoEmbeddings = null;
        }
    }

    /** Index all text files under a directory. */
    public static void main(String[] args) throws Exception {
        String usage =
                "java org.apache.lucene.demo.IndexFiles"
                        + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update] [-knn_dict DICT_PATH]\n\n"
                        + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                        + "in INDEX_PATH that can be searched with SearchFiles\n"
                        + "IF DICT_PATH contains a KnnVector dictionary, the index will also support KnnVector search";
        String indexPath = "testdata/index";
        String docsPath = "testdata/text";
        String vectorDictSource = null;
        boolean create = true;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-index":
                    indexPath = args[++i];
                    break;
                case "-docs":
                    docsPath = args[++i];
                    break;
                case "-knn_dict":
                    vectorDictSource = args[++i];
                    break;
                case "-update":
                    create = false;
                    break;
                case "-create":
                    create = true;
                    break;
                default:
                    throw new IllegalArgumentException("unknown parameter " + args[i]);
            }
        }

        if (docsPath == null) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        final Path docDir = Paths.get(docsPath);
        if (!Files.isReadable(docDir)) {
            System.out.println(
                    "Document directory '"
                            + docDir.toAbsolutePath()
                            + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        Date start = new Date();
        try {
            System.out.println("Indexing to directory '" + indexPath + "'...");

            Directory dir = FSDirectory.open(Paths.get(indexPath));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            if (create) {
                // Create a new index in the directory, removing any
                // previously indexed documents:
                iwc.setOpenMode(OpenMode.CREATE);
            } else {
                // Add new documents to an existing index:
                iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            }

            // Optional: for better indexing performance, if you
            // are indexing many documents, increase the RAM
            // buffer.  But if you do this, increase the max heap
            // size to the JVM (eg add -Xmx512m or -Xmx1g):
            //
            // iwc.setRAMBufferSizeMB(256.0);

            KnnVectorDict vectorDictInstance = null;
            long vectorDictSize = 0;
            if (vectorDictSource != null) {
                KnnVectorDict.build(Paths.get(vectorDictSource), dir, KNN_DICT);
                vectorDictInstance = new KnnVectorDict(dir, KNN_DICT);
                vectorDictSize = vectorDictInstance.ramBytesUsed();
            }

            try (IndexWriter writer = new IndexWriter(dir, iwc);
                 IndexFiles indexFiles = new IndexFiles(vectorDictInstance)) {
                indexFiles.indexDocs(writer, docDir);

                // NOTE: if you want to maximize search performance,
                // you can optionally call forceMerge here.  This can be
                // a terribly costly operation, so generally it's only
                // worth it when your index is relatively static (ie
                // you're done adding documents to it):
                //
                // writer.forceMerge(1);
            } finally {
                IOUtils.close(vectorDictInstance);
            }

            Date end = new Date();
            try (IndexReader reader = DirectoryReader.open(dir)) {
                System.out.println(
                        "Indexed "
                                + reader.numDocs()
                                + " documents in "
                                + (end.getTime() - start.getTime())
                                + " milliseconds");
                if (reader.numDocs() > 100
                        && vectorDictSize < 1_000_000
                        && System.getProperty("smoketester") == null) {
                    throw new RuntimeException(
                            "Are you (ab)using the toy vector dictionary? See the package javadocs to understand why you got this exception.");
                }
            }
        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
        }
    }

    /**
     * Indexes the given file using the given writer, or if a directory is given, recurses over files
     * and directories found under the given directory.
     *
     * <p>NOTE: This method indexes one document per input file. This is slow. For good throughput,
     * put multiple documents into your input file(s). An example of this is in the benchmark module,
     * which can create "line doc" files, one document per line, using the <a
     * href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
     * >WriteLineDocTask</a>.
     *
     * @param writer Writer to the index where the given file/dir info will be stored
     * @param path The file to index, or the directory to recurse into to find files to index
     * @throws IOException If there is a low-level I/O error
     */
    void indexDocs(final IndexWriter writer, Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(
                    path,
                    new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            try {
                                indexDoc(writer, file);
                            } catch (
                                    @SuppressWarnings("unused")
                                    IOException ignore) {
                                ignore.printStackTrace(System.err);
                                // don't index files that can't be read.
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } else {
            try {
                indexDoc(writer, path);
            } catch (
                    @SuppressWarnings("unused")
                    IOException ignore) {
                ignore.printStackTrace(System.err);
                // don't index files that can't be read.
            }

        }
    }

    /* Parse a file */
    void parseFile(Path fpath, IndexWriter writer){
        // parse document based on resources
        String filecontent = "";
        try {
            InputStream stream = Files.newInputStream(fpath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            filecontent = reader.lines().collect(Collectors.joining(" ")).replaceAll("[\\t\\n\\r]+"," ");
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        // loop through all docs in the file
        Pattern patternDoc = Pattern.compile("(?<=<DOC>)(.*?)(?=</DOC>)", Pattern.CASE_INSENSITIVE);
        Matcher alldocs = patternDoc.matcher(filecontent);
        while (alldocs.find()) {
            String str = alldocs.group();
            String docno = regexfilter("(?<=<DOCNO>)(.*?)(?=</DOCNO>)",str);
            String doctitle = regexfilter("(?<=<HEADLINE>)(.*?)(?=</HEADLINE>)",str);
            if (fpath.toString().contains("/fbis/")) {
                doctitle = regexfilter("(?<=<TI>)(.*?)(?=</TI>)",str);
            }
            String doctxt = regexfilter("(?<=<TEXT>)(.*?)(?=</TEXT>)",str);

            Document doc = new Document();
            Field pathField = new StringField("Path", fpath.toString(), Field.Store.YES);
            doc.add(pathField);
            doc.add(new StringField("DOCNO", docno.toString(), Field.Store.YES));
            doc.add(new IntField( "DOCID", docCount));
            doc.add(new StringField("TITLE", doctitle, Field.Store.NO));
            doc.add(new TextField("TEXT", doctitle + " " + doctxt, Field.Store.NO));
            try {
                if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
                    // create new index
                    writer.addDocument(doc);
                } else {
                    //update index
                    writer.updateDocument(new Term("DOCNO", docno), doc);
                }
                docCount ++;
            } catch (Exception e) {
                System.out.println("Error at:\t" + fpath.toString() + "\t" + docCount.toString());
                e.printStackTrace(System.err);
            }
        }
        System.out.println("Finished:\t" + fpath.toString() + "\t" + docCount.toString());
    }

    /* Get the first match result*/
    String regexfilter(String regStr, String inStr) {
        String outstr = "";
        Pattern pattern2m = Pattern.compile(regStr, Pattern.CASE_INSENSITIVE);
        Matcher matcherop = pattern2m.matcher(inStr);
        if (matcherop.find()){
            outstr = matcherop.group().replaceAll("<[^>]*>","").trim().replaceAll(" +", " ");
        }
        return outstr;
    }

    /** Indexes a single document */
    void indexDoc(IndexWriter writer, Path file) throws IOException {
        // skip readmes
        if (file.toString().contains("read")) {
            System.err.println("Encountered readme files, skip; file is " + file.toString());
            throw new IOException("Should be Ignored. Skip readme files");
        }
        parseFile(file, writer);
        System.out.println("Handling " + file.toString());
    }

    @Override
    public void close() throws IOException {
        IOUtils.close(vectorDict);
    }
}

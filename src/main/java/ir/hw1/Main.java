
package ir.hw1;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ir.hw1.*;

public class Main {
    public static void main(String[] args) throws Exception {
        String usage = "USAGE:\n" +
                "\tjava -jar HW1.jar [algorithm::={BM25,LM,RM1,RM3}] [IndexPath] [QueriesPath] [Output]\n" +
                "\tjava -jar HW1.jar IndexFiles [IndexOutputPath] [SourcePath]\n" +
                "\tjava -jar HW1.jar SearchFiles #will start searching with manual input (BM25)";
        String algorithm = args[0];
//        String index = args[1];
//        String queries = args[2];
//        String result = args[3];
        switch (algorithm) {
            case "IndexFiles": {
                IndexFiles task = new IndexFiles();
                task.run(args);
                System.out.println("Start Indexing");
                break;
            }
            case "SearchFiles": {
                SearchFiles task = new SearchFiles();
                task.run(args);
                System.out.println("Start Searching");
                break;
            }
            case "BM25": {
                System.out.println("BM25");
                break;
            }
            case "LM": {
                System.out.println("LM");
                break;
            }
            case "RM1": {
                System.out.println("RM1");
                break;
            }
            case "RM3": {
                System.out.println("RM3");
                break;
            }
            default:
                System.out.println("usage");
                break;
        }



    }

}

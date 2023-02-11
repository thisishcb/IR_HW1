
package ir.hw1;

import ir.hw1.modules.*;

public class Main {
    public static void main(String[] args) throws Exception {
        String usage = "USAGE:\n" +
                "\tjava -jar HW1.jar [algorithm::={BM25,LM,RM1,RM3}] [IndexPath] [QueriesPath] [Output]\n" +
                "\tjava -jar HW1.jar IndexFiles [IndexOutputPath] [SourcePath]\n" +
                "\tjava -jar HW1.jar SearchFiles #will start searching with manual input (BM25)";
        if (args.length ==0 ) {
            System.out.println(usage);
            System.exit(0);
        }
        String algorithm = args[0];
        if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
            System.out.println(usage);
            System.exit(0);
        }
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
                System.out.println("Start Searching");
                SearchFiles task = new SearchFiles();
                task.run(args);
                break;
            }
            case "BM25": {
                BM25 task = new BM25();
                task.run(args);
                break;
            }
            case "LM": {
                LM task = new LM();
                task.run(args);
                break;
            }
            case "RM1": {
                RM1 task = new RM1();
                task.run(args);
                break;
            }
            case "RM3": {
                RM3 task = new RM3();
                task.run(args);
                break;
            }
            default:
                System.out.println(usage);
                break;
        }



    }

}

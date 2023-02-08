
//import org.xml.sax.InputSource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
//        File file = new File("testdata/text/fbis/fb396002");
        Path path = Paths.get("testdata/text/ft/ft934/ft934_46");
//        System.out.println(path.toAbsolutePath().toString() + path.toString().contains("/ft/"));
        String filecontent = "";
        try {
            InputStream stream = Files.newInputStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
//            filecontent = org.apache.commons.io.IOUtils.toString(reader).replaceAll("[\\t\\n\\r]+"," ");//reader.lines().collect(Collectors.joining());
            filecontent = reader.lines().collect(Collectors.joining(" "));
//            System.out.println(filecontent.substring(0, 10));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        Pattern patternDoc = Pattern.compile("(?<=<DOC>)(.*?)(?=</DOC>)", Pattern.CASE_INSENSITIVE);
        Matcher alldocs = patternDoc.matcher(filecontent);
        Integer i = 0;
        while (alldocs.find() && i<5) {
            String docstr = alldocs.group();
//            System.out.println(docstr);

            Pattern patternDOCNO = Pattern.compile("(?<=<DOCNO>)(.*?)(?=</DOCNO>)", Pattern.CASE_INSENSITIVE);
            Matcher matcherDocno = patternDOCNO.matcher(docstr); matcherDocno.find();
            String docno = matcherDocno.group().replaceAll("<[^>]*>","").trim().replaceAll(" +", " ");

            Pattern patternTitle = Pattern.compile("(?<=<HEADLINE>)(.*?)(?=</HEADLINE>)", Pattern.CASE_INSENSITIVE);
            Matcher matcherTitle = patternTitle.matcher(docstr); matcherTitle.find();
            String doctitle = matcherTitle.group().replaceAll("<[^>]*>","").trim().replaceAll(" +", " ");

            Pattern patternText  = Pattern.compile("(?<=<TEXT>)(.*?)(?=</TEXT>)", Pattern.CASE_INSENSITIVE);
            Matcher matcherText = patternText.matcher(docstr); matcherText.find();
            String doctxt = matcherText.group().replaceAll("<[^>]*>","").trim().replaceAll(" +", " ");

            String[] res = {i.toString(), docno, doctitle, doctxt};
            System.out.println(res[1]);
            System.out.println(res[2]);
            System.out.println(res[3]);
            i += 1;
        }
//        BufferedReader reader;
//        Integer i = 0;
//        try {
//            reader = new BufferedReader(new FileReader(file));
//            String line = reader.readLine();
//
//            while (line != null && i<50) {
//                System.out.println(line);
//                // read next line
//                line = reader.readLine();
//                i++;
//            }
//
//            reader.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

}

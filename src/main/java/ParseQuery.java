import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParseQuery {
    public static void main(String[] args) {
        System.out.println("Parsing Query");
        Path path = Paths.get("testdata/eval/topics.351-400");
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

        Pattern patternDoc = Pattern.compile("(?<=<top>)(.*?)(?=</top>)", Pattern.CASE_INSENSITIVE);
        Matcher alldocs = patternDoc.matcher(filecontent);
        // open the writer to in queries.txt
        BufferedWriter outWriter = null;
        Integer cnt = 0;
        try {
            FileWriter fstream = new FileWriter("testdata/queries.txt", false);
            outWriter = new BufferedWriter(fstream);
            // write each query to file
            while (alldocs.find()) {
                String docstr = alldocs.group();
                String title = regexfilter("(?<=<title>)(.*?)(?=<desc>)",docstr);
                String desc = regexfilter("(?<=<desc> Description:)(.*?)(?=<narr>)",docstr);
                outWriter.write(title + " " + desc +"\n");
                outWriter.flush();
                cnt++;
            }
            if(outWriter != null) {
                outWriter.close();
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            System.out.println("Finish Parsing, with total num of: " + cnt.toString());
        }
    }

    /* Get the first match result*/
    static String regexfilter(String regStr, String inStr) {
        String outstr = "";
        Pattern pattern2m = Pattern.compile(regStr, Pattern.CASE_INSENSITIVE);
        Matcher matcherop = pattern2m.matcher(inStr);
        if (matcherop.find()){
            outstr = matcherop.group().replaceAll("<[^>]*>","").trim().replaceAll(" +", " ");
        }
        return outstr;
    }
}

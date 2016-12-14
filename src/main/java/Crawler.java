import com.restfb.*;
import com.restfb.exception.FacebookGraphException;
import com.restfb.json.JsonObject;
import com.restfb.types.Post;
import com.restfb.util.StringUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class Crawler {

    private static final int MAX_PAGES = 100000;

    private static FacebookClient facebookClient;


    public static void main(String[] args) throws IOException {
        crawlPage(args[0]);
    }

    private static void crawlPage(String pageName) throws IOException {
        final Writer out = new FileWriter(pageName + ".csv");
        final CSVPrinter printer = CSVFormat.DEFAULT.withHeader("id", ReactionTypes.ANGRY.toString(), ReactionTypes.HAHA.toString(), ReactionTypes.LIKE.toString(), ReactionTypes.LOVE.toString(), ReactionTypes.SAD.toString(), ReactionTypes.WOW.toString(), "link", "message").print(out);

        facebookClient = new DefaultFacebookClient(Config.ACCESS_TOKEN, Version.VERSION_2_8);

        Connection<Post> targetedSearch = facebookClient.fetchConnection(pageName + "/posts", Post.class, Parameter.with("fields", "link,message"));
        int i = 0;

        for (List<Post> posts : targetedSearch) {
            for (Post post : posts) {
                Map<ReactionTypes, Integer> data = getReactionCount(post.getId());
                if (data == null) {
                    continue;
                }
                printer.printRecord(post.getId(), data.get(ReactionTypes.ANGRY), data.get(ReactionTypes.HAHA), data.get(ReactionTypes.LIKE), data.get(ReactionTypes.LOVE), data.get(ReactionTypes.SAD), data.get(ReactionTypes.WOW), post.getLink(), post.getMessage());
            }

            // process log
            i++;
            if (i > MAX_PAGES) {
                break;
            }
            System.out.print(".");
            if (i % (MAX_PAGES / 100) == 0) {
                System.out.println(" ");
                System.out.println(i  / MAX_PAGES * 100 + "%");
            }
        }

        printer.close();
        out.close();
    }

    private static Map<ReactionTypes, Integer> getReactionCount(String id) {
        List<String> parameters = Arrays.stream(ReactionTypes.values()).map(r -> "reactions.type("+r+").limit(0).summary(total_count).as("+r+")").collect(Collectors.toList());
        try {
            JsonObject reactionData = facebookClient.fetchObject(id, JsonObject.class, Parameter.with("fields", StringUtils.join(parameters)));
            return Arrays.stream(ReactionTypes.values()).collect(Collectors.toMap(r -> r, r -> reactionData.getJsonObject(r.toString()).getJsonObject("summary").getInt("total_count")));
        } catch (FacebookGraphException e) {
            System.out.print(",");
            return null;
        }
    }

}

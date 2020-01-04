import atea.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

public class Ateabot {
    private Properties properties;
    private String version;
    private String target_platform;
    private String app_id;
    private String author_username;
    private String about_url;
    private String feedback_url;
    private String version_url;

    private String username;
    private String client_id;
    private String comment_footer;
    private String user_agent;

    private HttpConn conn;

    private String access_token;
    private String token_type;
    private long access_expires;

    private int ratelimit_used;
    private int ratelimit_remaining;
    private int ratelimit_reset;

    // Tasks
    boolean respond;
    boolean removeNegativeComments;
    int removeCommentThreshold;
    boolean terminate;

    private Atea atea;

    public Ateabot(Atea atea) throws SQLException, IOException {
        properties = new Properties();
        properties.load(this.getClass().getClassLoader().getResourceAsStream(".properties"));
        version = properties.getProperty("version");

        target_platform = "android";
        app_id = "com.github.mike-brady.atea";
        author_username = "mikebrady";
        about_url = "https://www.reddit.com/r/ateabot/comments/eh6uij/about/";
        feedback_url = "https://www.reddit.com/r/ateabot/comments/eh6vim/feedback/";
        version_url = "https://www.reddit.com/r/ateabot/comments/eh6w6o/version_history/";

        username = properties.getProperty("reddit_username");;
        client_id =  properties.getProperty("reddit_client_id");;
        comment_footer = "\n\n[^(v"+version+")]("+version_url+") ^(|) [^(about)]("+about_url+") ^(|) [^(feedback)]("+feedback_url+")";

        this.atea = atea;
        this.user_agent = target_platform + ":" + app_id + ":" + version + " (by /u/" + author_username + ")";

        try {
            connectToReddit();
        } catch (IOException | URISyntaxException | InterruptedException ex) {
            ex.printStackTrace();
        }

        getTasks();
    }

    /**
     * Starts Ateabot running continuously. Only stops when terminate is set to true.
     * @throws InterruptedException
     * @throws IOException
     * @throws URISyntaxException
     */
    public void run() throws InterruptedException, IOException, URISyntaxException {
        while(!terminate) {
            getTasks();

            if(respond) {
                ArrayList<String> usernameMentions = getUsernameMentions();
            }

            if(removeNegativeComments) {
                removeNegativeComments();
            }
        }

        System.out.println("Terminating...");
    }

    private boolean connectToReddit() throws IOException, URISyntaxException, InterruptedException {
        conn = new HttpConn();
        startRequest("https://www.reddit.com/api/v1/access_token", "POST");
        conn.setAuthHeader(client_id, "YXA8GdyVPSU8y-5kvgPjjpNJXUc");
        conn.setParam("grant_type", "password");
        conn.setParam("username", username);
        conn.setParam("password", "C7@daRBZ!F3VX9Hh");
        int responseCode = sendRequest();

        if(responseCode != 200) {
            return false;
        }

        String content = conn.getBody();
        System.out.println(content);
        JsonElement jsonTree = JsonParser.parseString(content);
        if(jsonTree.isJsonObject()) {
            JsonObject jsonObject = jsonTree.getAsJsonObject();
            this.access_token = jsonObject.get("access_token").getAsString();
            this.token_type = jsonObject.get("token_type").getAsString();
            Date d = new Date();
            long now = d.getTime();
            long expires_secs = jsonObject.get("expires_in").getAsLong();
            this.access_expires = now + expires_secs*1000;

            System.out.println("           Access Token: ****************");
            System.out.println("             Expires In: " + expires_secs/60 + " minutes");
        }



        return true;
    }

    private void getTasks() throws IOException {
        Properties control = new Properties();
        control.load(getClass().getResourceAsStream("control.txt"));
        String respond = control.getProperty("respond");
        if(respond != null && respond.equals("true")) {
            this.respond = true;
        } else {
            this.respond = false;
        }

        try {
            removeCommentThreshold = Integer.parseInt(control.getProperty("removeNegativeComments"));
            removeNegativeComments = true;
        }
        catch(NumberFormatException E) {
            removeNegativeComments = false;
        }

        String terminate = control.getProperty("terminate");
        if(terminate != null && terminate.equals("true")) {
            this.terminate = true;
        } else {
            this.terminate = false;
        }
    }

    private String get(String url) throws InterruptedException, IOException, URISyntaxException {
        startRequest("https://oauth.reddit.com/" + url, "GET");
        conn.setHeader("Authorization", "bearer " + access_token);
        int responseCode = sendRequest();

        String headers = conn.stringifyHeaders();
        return conn.getBody();
    }

    private Response post(String url, ArrayList<String[]> params) throws InterruptedException, IOException, URISyntaxException {
        startRequest("https://oauth.reddit.com/" + url, "POST");
        conn.setHeader("Authorization", "bearer " + access_token);
        for(String[] param : params) {
            conn.setParam(param[0], param[1]);
            System.out.println(param[0] + ":" + param[1]);
        }
        int responseCode = sendRequest();

        String headers = conn.stringifyHeaders();
        String body =  conn.getBody();
        return new Response(responseCode, headers, body);
    }

    private ArrayList<String> getUsernameMentions() throws InterruptedException, IOException, URISyntaxException {
        String content = get("/message/unread");
        JsonElement jsonTree = JsonParser.parseString(content);
        if(jsonTree.isJsonObject()) {
            JsonObject jsonObject = jsonTree.getAsJsonObject();
            JsonArray messages = jsonObject.get("data").getAsJsonObject().get("children").getAsJsonArray();
            for(JsonElement message : messages) {
                JsonObject messageData = message.getAsJsonObject().get("data").getAsJsonObject();
                String subject = messageData.get("subject").getAsString();
                if(subject.equals("username mention") || subject.equals("comment reply") || subject.equals("post reply")) {
                    String body = messageData.get("body").getAsString();

                    // If there is no username mention at the start, we assume this isn't a command request
                    if(!body.startsWith("u/"+username) && !body.startsWith("/u/"+username)) {
                        continue;
                    }

                    String commentId = messageData.get("name").getAsString();
                    String author = messageData.get("author").getAsString();
                    String parentId = messageData.get("parent_id").getAsString();
                    String subreddit = messageData.get("subreddit").getAsString();


                    String command;
                    String input;
                    int colon_index = body.indexOf(":");
                    if(colon_index >= 0) {
                        command = body.substring(0, colon_index - 1);
                        input = body.substring(colon_index + 1);
                    } else {
                        command = body;
                        input = getContentById(subreddit, parentId);
                    }

                    String response = "Hi u/" + author + ", I couldn't find any abbreviations in the parent comment.";

                    if(command.contains("explain")) {
                        response = atea.explain(input);
                    } else if(command.contains("expand")) {
                        response = atea.expand(input);
                    } else {
                        if (input.length() > 0) {
                            ArrayList<Abbreviation> abbrs = atea.getAbbreviations(input, true);
                            response = abbrs.size() + " abbreviation";
                            if (abbrs.size() != 1) {
                                response += "s";
                            }
                            response += " found.";
                            response += "\n\n|Abbreviation|Expansion|Confidence|";
                            response += "\n|:--|:--|:--|";
                            for (Abbreviation abbr : abbrs) {
                                response += "\n|**" + abbr.getValue() + "**|";

                                Expansion expansion = abbr.getBestExpansion();
                                response += expansion.getValue() + "|";
                                response += expansion.getConfidence() + "|";
                            }
                        }
                    }

                    ArrayList<String[]> params = new ArrayList<>();
                    params.add(new String[]{"api_type", "json"});
                    params.add(new String[]{"thing_id", commentId});
                    params.add(new String[]{"text", response + comment_footer});
                    Response commentResponse = post("api/comment", params);

                    if(commentResponse.getResponseCode() == 200) {
                        params = new ArrayList<>();
                        params.add(new String[]{"id", commentId});
                        Response readResponse = post("/api/read_message", params);
                    }
                }
            }
        }

        return new ArrayList<String>();
    }

    private void removeNegativeComments() throws InterruptedException, IOException, URISyntaxException {
        String content = get("user/" + username + "/comments");
        JsonElement jsonTree = JsonParser.parseString(content);
        if(jsonTree.isJsonObject()) {
            JsonObject jsonObject = jsonTree.getAsJsonObject();
            JsonArray children = jsonObject.get("data").getAsJsonObject().get("children").getAsJsonArray();
            for(JsonElement item : children) {
                JsonObject itemData = item.getAsJsonObject().get("data").getAsJsonObject();
                int score = itemData.get("score").getAsInt();
                String commentId = itemData.get("name").getAsString();
                if(score <= removeCommentThreshold) {
                    System.out.println("removing comment.");
                    ArrayList<String[]> params = new ArrayList<>();
                    params.add(new String[]{"id", commentId});
                    post("api/del", params);
                }
            }
        }
    }

    private String getContentById(String subreddit, String id) throws InterruptedException, IOException, URISyntaxException {
        String content = get("r/" + subreddit + "/api/info?id=" + id);
        JsonElement jsonTree = JsonParser.parseString(content);
        if(jsonTree.isJsonObject()) {
            JsonObject jsonObject = jsonTree.getAsJsonObject();
            JsonArray children = jsonObject.get("data").getAsJsonObject().get("children").getAsJsonArray();
            for(JsonElement item : children) {
                JsonObject itemData = item.getAsJsonObject().get("data").getAsJsonObject();
                if(id.startsWith("t1_")) {
                    return itemData.get("body").getAsString();
                } else if(id.startsWith("t3_")) {
                    return itemData.get("selftext").getAsString();
                }
            }
        }

        return "";
    }

    private String findAbbreviationExample(String subreddit) throws IOException, URISyntaxException, InterruptedException {
        get("r/" + subreddit + "/comments/");
        String content = conn.getBody();
        JsonElement jsonTree = JsonParser.parseString(content);
        if(jsonTree.isJsonObject()) {
            JsonObject jsonObject = jsonTree.getAsJsonObject();
            JsonArray comments = jsonObject.get("data").getAsJsonObject().get("children").getAsJsonArray();
            for(JsonElement comment : comments) {
                JsonObject commentData = comment.getAsJsonObject().get("data").getAsJsonObject();
                String commentBody = commentData.get("body").getAsString();
                ArrayList<Abbreviation> abbrs = atea.getAbbreviations(commentBody, true);
                for(Abbreviation abbr : abbrs) {
                    System.out.println("\nPossible Abbreviation: \033[1m" + abbr.getValue() + "\033[0m");
                    System.out.println("Context: " + abbr.getContext().toString(true));
                    System.out.println("Permalink: https://www.reddit.com" + commentData.get("permalink").getAsString());
                    System.out.println("Possible Expansions: ");
                    for(Expansion expansion : abbr.getExpansions()) {
                        System.out.println(expansion.getValue() + " : " + expansion.getConfidence());
                    }
                }
            }
        }

        return content;
    }

//    public boolean disconnect() throws IOException {
//        HttpConn conn = startConn("https://www.reddit.com/api/v1/revoke_token");
//        conn.setParam("token", access_token);
//        conn.setParam("token_type_hint", "access_token");
//
//        String responseCode = conn.sendRequest("POST");
//        if(responseCode.equals("204")) {
//            return true;
//        }
//
//        return false;
//    }

    private void startRequest(String uri, String method) {
        conn.startRequest(uri, method);
        conn.setHeader("User-Agent", user_agent);
        conn.setHeader("Content-Type", "application/x-www-form-urlencoded");
    }

    private int sendRequest() throws InterruptedException, IOException, URISyntaxException {
        return sendRequest(true);
    }

    private int sendRequest(boolean output) throws InterruptedException, IOException, URISyntaxException {
        // Delay all requests by calculated number of milliseconds to stay within rate limit
        int sleepDuration = (int) (((float) ratelimit_reset / (float) ratelimit_remaining) * 1000f);

        // Minimum time between requests
        sleepDuration = Integer.max(100, sleepDuration);

        System.out.println("Sleeping for " + sleepDuration + " milliseconds");
        Thread.sleep(sleepDuration);

        if(output) {
            System.out.println("\n" + conn.getRequestMethod() + ": " + conn.getRequestUri());
        }
        int responseCode = conn.sendRequest();
        if(output) {
            describeResponse();
        }
        return responseCode;
    }

    private void describeResponse() {
        System.out.println("HTTP Response Code: " + conn.getResponseCode());
        try {
            ratelimit_used = Integer.parseInt(conn.getHeader("x-ratelimit-used"));
            ratelimit_remaining = (int) Float.parseFloat(conn.getHeader("x-ratelimit-remaining"));
            ratelimit_reset = Integer.parseInt(conn.getHeader("x-ratelimit-reset"));
            System.out.print("Ratelimit: " + ratelimit_used + "/" + (ratelimit_remaining + ratelimit_used));
            System.out.println(" Resets in " + ratelimit_reset + "s");
        } catch(NumberFormatException e) {}
        catch (NullPointerException e) {}

        try {

        } catch(NumberFormatException e) {}
    }
}

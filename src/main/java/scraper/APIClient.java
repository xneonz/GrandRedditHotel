package scraper;

import org.json.simple.JSONArray;
import post.PostParams;
import post.RedditPost;
import org.jsoup.Jsoup;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class APIClient {

    public RedditPost fetchClientTree(String url) {

        try {

            RedditPost redditPost = new RedditPost();
            ArrayList<RedditPost> replies = redditPost.getComments();
            Document webpage = Jsoup.connect(url).get();
            Element titleElement = webpage.getElementsByTag("title").first();
            String titleString = titleElement.toString();
            titleString = titleString.replaceAll("<title>","");
            titleString = titleString.replaceAll("</title>","");
            String subredditString = "r/" + titleString.substring(titleString.lastIndexOf(":") + 1);
            titleString = titleString.substring(0,titleString.lastIndexOf(":"));
            redditPost.setProperty(PostParams.TITLE,titleString);
            redditPost.setProperty(PostParams.BODY,titleString);
            redditPost.setProperty(PostParams.SUBREDDIT,subredditString);
            redditPost.setProperty(PostParams.TYPE, "post");
            JSONObject json = scrapeDataFromPage(webpage);
            json = (JSONObject) json.get("comments");
            json = (JSONObject) json.get("models");

            for(Object c : json.keySet()) {
                JSONObject comment = (JSONObject) json.get(c);
                String commentID = (String) comment.get("id");
                String parentID = (String) comment.get("parentId");
                String commentAuthor = (String) comment.get("author");
                Long commentScore = (Long) comment.get("score");
                Long commentTime = (Long) comment.get("created");
                comment = (JSONObject) comment.get("media");
                comment = (JSONObject) comment.get("richtextContent");
                JSONArray commentParticles = (JSONArray) comment.get("document");
                String commentBody = "";
                for(Object d : commentParticles) {
                    JSONObject commentParticle = (JSONObject) d;
                    JSONArray particleArray = (JSONArray) commentParticle.get("c");
                    try {
                        commentParticle = (JSONObject) particleArray.get(0);
                    } catch(Exception e) {
                        continue;
                    }
                    String particleString = (String) commentParticle.get("t");
                    commentBody = commentBody + particleString + "\n";
                }
                RedditPost redditComment = new RedditPost();
                redditComment.setProperty(PostParams.BODY, commentBody);
                redditComment.setProperty(PostParams.POST_ID, commentID);
                redditComment.setProperty(PostParams.PARENT_ID, parentID);
                redditComment.setProperty(PostParams.POINTS, commentScore.toString());
                redditComment.setProperty(PostParams.AUTHOR_NAME, commentAuthor);
                redditComment.setProperty(PostParams.TIMESTAMP, commentTime.toString());
                redditComment.setProperty(PostParams.TYPE, "comment");
                replies.add(redditComment);
            }

            arrangeTree(redditPost);
            sortCommentsByScore(redditPost);
            return redditPost;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sortCommentsByScore(RedditPost redditPost) {

        ArrayList<RedditPost> commentList = redditPost.getComments();
        Collections.sort(commentList, (a, b) -> Long.parseLong(a.getProperty(PostParams.POINTS)) >
                Long.parseLong(b.getProperty(PostParams.POINTS)) ? -1 : 1);
        for(RedditPost comment : commentList) {
            sortCommentsByScore(comment);
        }

    }

    private void arrangeTree(RedditPost redditPost) {

        HashMap<String,RedditPost> commentMap = new HashMap<String,RedditPost>();
        ArrayList<RedditPost> commentList = redditPost.getComments();
        ArrayList<RedditPost> replyComments = new ArrayList<RedditPost>();

        for(RedditPost comment : commentList) {
            commentMap.put(comment.getProperty(PostParams.POST_ID),comment);
        }

        for(RedditPost comment : commentList) {
            String parentID = comment.getProperty(PostParams.PARENT_ID);
            if(parentID == null) {
                continue;
            }
            RedditPost parentPost = commentMap.get(parentID);
            ArrayList<RedditPost> parentReplies = parentPost.getComments();
            parentReplies.add(comment);
            replyComments.add(comment);
        }

        for(RedditPost comment : replyComments) {
            commentList.remove(comment);
        }

    }

    private JSONObject scrapeDataFromPage(Document webpage) {

        Element dataElement = webpage.getElementById("data");
        String dataString = dataElement.toString();
        int firstBracket = dataString.indexOf('{');
        int lastBracket = dataString.lastIndexOf('}');
        dataString = dataString.substring(firstBracket, lastBracket + 1);
        JSONParser jsonParser = new JSONParser();
        try {
            return (JSONObject) jsonParser.parse(dataString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

}

package renderer;

import com.sun.tools.javac.util.Pair;
import post.PostParams;
import post.RedditPost;
import util.TextUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;

public class PostRenderer {

    private final String FONTNAME = "verdana";
    private final int IMAGE_WIDTH = 1920;
    private final int IMAGE_HEIGHT = 1080;
    private final float TEXT_WIDTH_RATIO = 0.9f;
    private final float TEXT_HEIGHT_RATIO = 0.8f;
    private final int POST_TITLE_SIZE = 64;
    private final int POST_BODY_SIZE = 64;
    private final int COMMENT_TITLE_SIZE = 32;
    private final int COMMENT_BODY_SIZE = 32;
    private final Color POST_AUTHOR_COLOR = new Color(248,248,248);
    private final Color POST_TITLE_COLOR = new Color(186,186,186);
    private final Color POST_BODY_COLOR = new Color(170,170,170);
    private final Color POST_BACKGROUND_COLOR = new Color(32,32,32);
    private final Color COMMENT_AUTHOR_COLOR = new Color(248,248,248);
    private final Color COMMENT_TITLE_COLOR = new Color(186,186,186);
    private final Color COMMENT_BODY_COLOR = new Color(170,170,170);
    private final Color COMMENT_BACKGROUND_COLOR = new Color(32,32,32);

    public void renderPost(RedditPost redditPost) {

        BufferedImage image = new BufferedImage(IMAGE_WIDTH,IMAGE_HEIGHT,BufferedImage.TYPE_INT_ARGB);
        setBackground(image,POST_BACKGROUND_COLOR);
        TextUtil textUtil = new TextUtil();
        ArrayList<String> tokens = textUtil.tokenizePostBody(redditPost);
        int textStartX = (int) (IMAGE_WIDTH * (0.5f * (1.0f - TEXT_WIDTH_RATIO)));
        int textEndX = IMAGE_WIDTH - textStartX;
        int textStartY = (int) (IMAGE_HEIGHT * (0.5f * (1.0f - TEXT_HEIGHT_RATIO)));
        writeTokensOverImage(image, tokens, textStartX, textStartY, textEndX, "post");
        ArrayList<Pair<String,BufferedImage>> postPages = redditPost.getPages();
        Pair<String,BufferedImage> page =
                new Pair<String,BufferedImage>(redditPost.getProperty(PostParams.SUBREDDIT)
                        + ", " + redditPost.getProperty(PostParams.TITLE),image);
        postPages.add(page);
        for(RedditPost comment : redditPost.getComments()) {
            renderComment(comment);
        }

    }

    private void renderComment(RedditPost redditPost) {

        TextUtil textUtil = new TextUtil();
        ArrayList<String> tokens = textUtil.tokenizePostBody(redditPost);
        BufferedImage bi = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bi.createGraphics();
        g2d.setFont(new Font(FONTNAME, Font.PLAIN, COMMENT_BODY_SIZE));
        ArrayList<Pair<String,BufferedImage>> postPages = redditPost.getPages();
        int textStartX = (int) (IMAGE_WIDTH * (0.5f * (1.0f - TEXT_WIDTH_RATIO)));
        int textEndX = IMAGE_WIDTH - textStartX;
        int textStartY = (int) (IMAGE_HEIGHT * (0.5f * (1.0f - TEXT_HEIGHT_RATIO)));
        int textWidth = (int) (IMAGE_WIDTH * TEXT_WIDTH_RATIO);
        int textHeight = (int) (IMAGE_WIDTH * TEXT_HEIGHT_RATIO);
        ArrayList<ArrayList<String>> paginatedStrings =
                textUtil.paginateTokens(textWidth, textHeight, g2d.getFontMetrics(), tokens);
        for(ArrayList<String> page : paginatedStrings) {
            BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            setBackground(image,COMMENT_BACKGROUND_COLOR);
            writeTokensOverImage(image, page, textStartX, textStartY, textEndX, "comment");
            String compressedPageString = textUtil.compressTokens(page);
            Pair<String,BufferedImage> pagePair = new Pair<String, BufferedImage>(compressedPageString,image);
            postPages.add(pagePair);
        }
        writeTitleOverImage(postPages.get(0).snd,
                redditPost.getProperty(PostParams.AUTHOR_NAME),
                redditPost.getProperty(PostParams.POINTS),
                redditPost.getProperty(PostParams.TIMESTAMP),
                textStartX,
                textStartY - g2d.getFontMetrics().getHeight());
        for(RedditPost reply : redditPost.getComments()) {
            renderComment(reply);
        }

    }

    private void setBackground(BufferedImage image, Color color) {

        Graphics2D g2d = image.createGraphics();
        g2d.setPaint(color);
        g2d.fillRect(0,0,IMAGE_WIDTH,IMAGE_HEIGHT);

    }

    private void writeTitleOverImage(BufferedImage image, String author, String score, String timestamp, int startX, int startY) {

        String postUpSince = timeSincePosted(timestamp);
        String formattedScore = formatPoints(score);
        Graphics2D g2d = image.createGraphics();
        g2d.setFont(new Font(FONTNAME, Font.PLAIN, COMMENT_TITLE_SIZE));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.setPaint(COMMENT_AUTHOR_COLOR);
        g2d.drawString(author, startX, startY);
        startX = startX + fm.stringWidth(author + " ");
        g2d.setPaint(COMMENT_TITLE_COLOR);
        g2d.drawString(formattedScore + " - " + postUpSince, startX, startY);

    }

    private String timeSincePosted(String timestamp) {

        long postDate = Long.parseLong(timestamp);
        long currentDate = Instant.now().getEpochSecond();
        long postTime = currentDate - postDate;
        String unitName = "";

        if(postTime < 60) {
            unitName = "second";
        } else if((postTime/=60) < 60) {
            unitName = "minute";
        } else if((postTime/=60) < 24) {
            unitName = "hour";
        } else if((postTime/=24) < 30) {
            unitName = "day";
        } else if((postTime/=30) < 12) {
            unitName = "month";
        } else {
            postTime/=12;
            unitName = "year";
        }

        if(postTime > 1) {
            unitName = unitName + "s";
        }

        return Long.toString(postTime) + " " + unitName + " ago";

    }

    private String formatPoints(String score) {

        long scoreLong = Long.parseLong(score);
        String scoreMagnitude = "";
        if(scoreLong < 1000) {
            scoreMagnitude = Long.toString(scoreLong);
        } else if(scoreLong < 10000) {
            scoreMagnitude = Long.toString(scoreLong / 1000) + "." + Long.toString((scoreLong % 1000) / 100);
            scoreMagnitude = scoreMagnitude + "k";
        } else {
            scoreMagnitude = Long.toString(scoreLong / 1000);
            scoreMagnitude = scoreMagnitude + "k";
        }
        if(scoreLong == 1) {
            return "1 point";
        } else {
            return scoreMagnitude + " points";
        }

    }

    private void writeTokensOverImage(BufferedImage image, ArrayList<String> tokens, int startX, int startY, int endX, String type) {

        Font font = null;
        Graphics2D g2d = image.createGraphics();
        if(type.equals("post")) {
            font = new Font(FONTNAME, Font.BOLD, POST_BODY_SIZE);
            g2d.setPaint(POST_BODY_COLOR);
        } else if(type.equals("comment")) {
            font = new Font(FONTNAME, Font.PLAIN, COMMENT_BODY_SIZE);
            g2d.setPaint(COMMENT_BODY_COLOR);
        }
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int x = startX;
        int y = startY;
        for(String token : tokens) {
            if(token.equals("\n")) {
                if(x == startX) {
                    y = y + fm.getHeight();
                } else {
                    x = startX;
                    y = y + 2 * fm.getHeight();
                }
            } else {
                if (x + fm.stringWidth(token) > endX) {
                    x = startX;
                    y = y + fm.getHeight();
                }
                g2d.drawString(token, x, y);
                x = x + fm.stringWidth(token + " ");
            }
        }

    }

}

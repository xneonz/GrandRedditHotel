package util;

import post.PostParams;
import post.RedditPost;

import java.awt.*;
import java.util.ArrayList;
import java.util.Scanner;

public class TextUtil {

    public ArrayList<String> tokenizePostBody(RedditPost redditPost) {

        ArrayList<String> tokens = new ArrayList<String>();
        Scanner bodyLines = new Scanner(redditPost.getProperty(PostParams.BODY));
        bodyLines.useDelimiter("\n");
        while(bodyLines.hasNext()) {
            Scanner bodyLine = new Scanner(bodyLines.next());
            bodyLine.useDelimiter(" ");
            while(bodyLine.hasNext()) {
                tokens.add(bodyLine.next());
            }
            if(bodyLines.hasNext()) {
                tokens.add("\n");
            }
        }
        return tokens;

    }

    public ArrayList<ArrayList<String>> paginateTokens(int width, int height, FontMetrics fontMetrics, ArrayList<String> tokens) {

        ArrayList<ArrayList<String>> pages = new ArrayList<ArrayList<String>>();
        int x = 0;
        int y = 0;
        ArrayList<String> page = new ArrayList<String>();
        for(String token : tokens) {
            if(token.equals("\n")) {
                page.add(token);
                if(x == 0) {
                    y = y + fontMetrics.getHeight();
                } else {
                    x = 0;
                    y = y + 2 * fontMetrics.getHeight();
                }
                if(y + fontMetrics.getHeight() > height) {
                    pages.add(page);
                    page = new ArrayList<String>();
                    x = 0;
                    y = 0;
                }
            } else {
                if(x + fontMetrics.stringWidth(token) > width) {
                    x = 0;
                    y = y + fontMetrics.getHeight();
                    if(y + fontMetrics.getHeight() > height) {
                        pages.add(page);
                        page = new ArrayList<String>();
                        y = 0;
                    }
                }
                page.add(token);
                x = x + fontMetrics.stringWidth(token + " ");
            }
        }
        pages.add(page);
        return pages;

    }

    public String compressTokens(ArrayList<String> tokens) {
        String compressed = "";
        for(String token : tokens) {
            if(token.equals("\n")) {
                compressed = compressed + " ... ";
            } else {
                compressed = compressed + token + " ";
            }
        }
        return compressed;
    }

}

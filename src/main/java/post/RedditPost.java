package post;

import com.sun.tools.javac.util.Pair;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

public class RedditPost {

    private final HashMap<String,String> properties = new HashMap<String,String>();
    private final ArrayList<RedditPost> comments = new ArrayList<RedditPost>();
    private final ArrayList<Pair<String,BufferedImage>> pages = new ArrayList<Pair<String,BufferedImage>>();

    public ArrayList<Pair<String,BufferedImage>> getPages() {

        return pages;

    }

    public ArrayList<RedditPost> getComments() {

        return comments;

    }

    public void setProperty(PostParams key, String value) {

        setProperty(key.value(), value);

    }

    public void setProperty(String key, String value) {

        properties.put(key, value);

    }

    public String getProperty(PostParams key) {

        return getProperty(key.value());

    }

    public String getProperty(String key) {

        return properties.get(key);

    }

}

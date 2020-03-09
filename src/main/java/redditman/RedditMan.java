package redditman;

import post.RedditPost;
import renderer.PostRenderer;
import scraper.APIClient;
import util.FileWriter;

public class RedditMan {

    public static void main(String[] args) {

        for(String a : args) {
            System.out.println(a);
            makeVideo(a);
        }

    }

    public static void makeVideo(String url) {
        APIClient apiClient = new APIClient();
        RedditPost redditPost = apiClient.fetchClientTree(url);
        FileWriter fileWriter = new FileWriter();
        String directoryName = fileWriter.createDirectoryName(redditPost);
        fileWriter.createDirectory(directoryName);
        ProcessBuilder pb = new ProcessBuilder();
        System.out.println("cp " + System.getProperty("user.dir") +
                "/t.mp4 " + fileWriter.getRedmanHome().getAbsolutePath() + "/" + directoryName + "/t.mp4");
        pb.command("bash", "-c", "cp " + System.getProperty("user.dir") +
                "/t.mp4 " + fileWriter.getRedmanHome().getAbsolutePath() + "/" + directoryName + "/t.mp4");
        System.out.println(System.getProperty("user.dir"));
        try {
            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        PostRenderer renderer = new PostRenderer();
        String directory = FileWriter.getRedmanHome().getAbsolutePath() + "/" + directoryName;
        renderer.renderPost(redditPost);
        fileWriter.writePostToFiles(redditPost,directory, 1, 0.6f);
        fileWriter.finalizeVideo(directory);
    }

}

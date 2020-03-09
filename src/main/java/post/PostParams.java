package post;

public enum PostParams {

    AUTHOR_NAME("authorName"),
    BODY("postBody"),
    POINTS("points"),
    TIMESTAMP("timestamp"),
    TYPE("type"),
    POST_ID("postID"),
    PARENT_ID("parentID"),
    SUBREDDIT("subreddit"),
    TITLE("title");

    PostParams(String paramName) {

        this.paramName = paramName;

    }

    public String value() {

        return paramName;

    }

    private final String paramName;

}

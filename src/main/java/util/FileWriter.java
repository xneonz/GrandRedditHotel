package util;

import com.sun.tools.javac.util.Pair;
import post.PostParams;
import post.RedditPost;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class FileWriter {

    private static File redmanHome;
    private static final String TRANSITION = "t.mp4";

    public File createDirectory(String directoryName) {

        return createDirectory(directoryName, FileWriter.getRedmanHome());

    }

    public String createDirectoryName(RedditPost redditPost) {

        String title = redditPost.getProperty(PostParams.TITLE);
        title = title.toLowerCase();
        title = title.replaceAll("[^a-z]","");
        return title;

    }

    public File createDirectory(String directoryName, File parentDirectory) {

        String fullDirectoryName = parentDirectory.getAbsolutePath() + "/" + directoryName;
        File directory = new File(fullDirectoryName);
        directory.mkdir();
        return directory;

    }

    public void writePostToFiles(RedditPost post, String directory, int maxReplies, float minScore) {
        int i = 0;
        writePostToFiles(post, directory, "/o", 0, 0, 1.0f);
        for(RedditPost comment : post.getComments()) {
            writePostToFiles(comment, directory, "/c", i++, maxReplies, minScore);
        }
    }

    private void writePostToFiles(RedditPost post, String directory, String prefix, int index, int maxReplies, float minScore) {
        int i = 0;
        for(Pair<String, BufferedImage> page : post.getPages()) {
            File txtOut = new File(directory + prefix + index + "p" + i + ".txt");
            File imgOut = new File(directory + prefix + index + "p" + i++ + ".png");
            try {
                Files.write(Paths.get(txtOut.getAbsolutePath()),page.fst.getBytes());
                ImageIO.write(page.snd, "png", imgOut);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        i = 0;
        for(RedditPost reply : post.getComments()) {
            if(i < maxReplies &&
                    Long.parseLong(reply.getProperty(PostParams.POINTS))
                            > (long) (Long.parseLong(post.getProperty(PostParams.POINTS)) * minScore)) {
                writePostToFiles(reply, directory, prefix + index + "r", i++, 0, minScore);
            }
        }
    }

    public static File getRedmanHome() {

        if(redmanHome == null) {
            redmanHome = new File(System.getProperty("user.home") + "/redman");
            redmanHome.mkdir();
        }
        return redmanHome;

    }

    private String vidFromPair(String fileName) {
        String txtName = fileName + ".txt";
        String pngName = fileName + ".png";
        String wavName = fileName + ".wav";
        String mp4Name = fileName + ".mp4";
        String tempMp4Name = fileName + "_temp.mp4";
        ProcessBuilder pb = new ProcessBuilder();
        String makeWav = "cat " + txtName + " | pico2wave --wave=" + wavName;
        pb.command("bash", "-c", makeWav);
        try {
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        pb = new ProcessBuilder();
        String vidLength = getAudioLength(wavName);
        String makeMp4 = "ffmpeg -loop 1 -i " + pngName + " -i " + wavName +
                " -c:v libx264 -c:a aac -strict experimental -b:a 192k -shortest " + tempMp4Name;
        pb.command("bash", "-c", makeMp4);
        try {
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        pb = new ProcessBuilder();
        String cutMp4 = "ffmpeg -i " + tempMp4Name + " -ss 00:00:00 -t " + vidLength + " -async 1 " + mp4Name;
        pb.command("bash", "-c", cutMp4);
        try {
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mp4Name;
    }

    public void finalizeVideo(String directoryPath) {
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();
        HashSet<String> fileSet = new HashSet<String>();
        for(File f : files) {
            String filePath = f.getAbsolutePath();
            if(!filePath.contains("t.mp4")) {
                fileSet.add(filePath.substring(0, filePath.length() - 4));
            }
        }
        ArrayList<String> fileList = new ArrayList<String>();
        for(String s : fileSet) {
            fileList.add(s.substring(directoryPath.length() + 1));
        }
        Collections.sort(fileList, new FileComparator());
        for(String s : fileSet) {
            vidFromPair(s);
        }
        combineVideos(fileList, directoryPath);
    }

    private void combineVideos(ArrayList<String> videos, String directory) {
        int i = 0;
        int j = 0;
        ArrayList<String> combineCommands = new ArrayList<String>();
        ArrayList<String> combineFiles = new ArrayList<String>();
        String combineCommand = "";
        for(String v : videos) {
            v = v + ".mp4";
            if(v.contains("r") || v.contains("o")) {
                combineCommand = combineCommand + v;
            } else {
                combineCommand = combineCommand + TRANSITION + " \\+ " + v;
            }
            i++;
            if(i > 10) {
                String combineFile = "temp_" + j + ".mp4";
                combineFiles.add(combineFile);
                combineCommands.add(combineCommand);
                combineCommand = "";
                i = 0;
                j++;
            } else {
                combineCommand = combineCommand + " \\+ ";
            }
        }
        i = 0;
        for(String c : combineCommands) {
            String f = combineFiles.get(i);
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(directory));
            pb.command("bash", "-c", "mkvmerge -o " + f + " " + c);
            System.out.println("mkvmerge -o " + f + " " + c);
            try {
                Process p = pb.start();
                p.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
            i++;
        }
        String combineAllCommand = "";
        i = 0;
        for(String f : combineFiles) {
            if(i == 0) {
                combineAllCommand = combineAllCommand + f;
            } else {
                combineAllCommand = combineAllCommand + " \\+ " + f;
            }
            i++;
        }
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(directory));
        pb.command("bash", "-c", "mkvmerge -o final.mp4 " + combineAllCommand);
        try {
            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    class FileComparator implements Comparator<String> {

        @Override
        public int compare(String a, String b) {
            if(a.contains("o")) {
                return -1;
            } else if(b.contains("o")) {
                return 1;
            }
            String a_i = a.replaceAll("[^0-9]"," ");
            String b_i = b.replaceAll("[^0-9]"," ");
            Scanner sc_a = new Scanner(a_i);
            Scanner sc_b = new Scanner(b_i);
            int a_c = sc_a.nextInt();
            int b_c = sc_b.nextInt();
            if(a_c < b_c) {
                return -1;
            } else if(a_c > b_c) {
                return 1;
            } else if(a.contains("r") && b.contains("r")) {
                int a_r = sc_a.nextInt();
                int b_r = sc_b.nextInt();
                if(a_r < b_r) {
                    return -1;
                } else if(a_r > b_r) {
                    return 1;
                } else {
                    int a_p = sc_a.nextInt();
                    int b_p = sc_b.nextInt();
                    if(a_p < b_p) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            } else if(a.contains("r") && !b.contains("r")) {
                return 1;
            } else if(!a.contains("r") && b.contains("r")) {
                return -1;
            } else {
                int a_p = sc_a.nextInt();
                int b_p = sc_b.nextInt();
                if(a_p < b_p) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }
    }

    private String getAudioLength(String audioFileName) {
        try {
            File audioFile = new File(audioFileName);
            AudioInputStream in = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat f = in.getFormat();
            long n = in.getFrameLength();
            int t = (int) Math.ceil(((float) n) / f.getFrameRate());
            String sec = Integer.toString(t % 60);
            if(sec.length() == 1) {
                sec = "0" + sec;
            }
            String min = Integer.toString((t / 60) % 60);
            if(min.length() == 1) {
                min = "0" + min;
            }
            String hour = Integer.toString(t / 3600);
            if(hour.length() == 1) {
                hour = "0" + hour;
            }
            return hour + ":" + min + ":" + sec;
        } catch (Exception e) {
            e.printStackTrace();
            return "00:00:00";
        }
    }

}

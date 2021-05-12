package com.mbn;

import java.io.FileNotFoundException;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        // write your code here
        System.out.println(Arrays.toString(args));
        InfoCatcher.DL_info dl_info = InfoCatcher.catchInfo(args[0]);
        System.out.println(dl_info);

        Downloader.DlRequest dlRequest = new Downloader.DlRequest(generateThreadInfo(dl_info, 4), dl_info, 4
                , dl_info.remoteFileName);

        try {
            Downloader downloader = new Downloader(dlRequest, new Downloader.DownloadCallback() {
                private volatile boolean finished = false;

                @Override
                public void onProgress(float progress, int totalDownloaded, int speed) {
                    if (!finished) {
                        System.out.printf("%s %% - %d MB - %s MB/Sec%n", Math.round(progress * 10000.0) / 100.0,
                                totalDownloaded / Downloader.MEG_BYTE, Math.round((speed / (float) Downloader.MEG_BYTE) * 100) / 100f);
                    }
//                    System.out.print(progress);
//                    System.out.println(totalDownloaded);
//                    System.out.println(speed / (float) Downloader.MEG_BYTE);
                }

                @Override
                public void onFinish() {
                    finished = true;
                    System.out.println("Download finished successfully!");
                    deleteResumeFile();
                }

                @Override
                public void onError() {

                }
            });
            downloader.start();

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    downloader.stop(true);
                    writeResumeFile();
                }
            }));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


//        String turl = "https://www.google.com/api/hey%20file.txt?name=noway&type=nice";
//        try {
//            URL url = new URL(turl);
//            System.out.println(url.getFile());
//            System.out.println(url.getPath());
//            System.out.println(url.getQuery());
//            String path = url.getPath();
//            System.out.println("File name: " + URLDecoder.decode(path.substring(path.lastIndexOf('/') + 1), StandardCharsets.UTF_8));
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }


        // TODO: 5/7/21 add a hook to terminate the threads and write the last log...


    }

    private static void writeResumeFile() {

    }

    private static void deleteResumeFile() {

    }


    private static Downloader.ThreadInfo[] generateThreadInfo(InfoCatcher.DL_info info, int threadCount) {
        Downloader.ThreadInfo[] threads = new Downloader.ThreadInfo[threadCount];
        int th_Len = info.contentLength / threadCount;
        int i = 0;
        for (; i < threadCount - 1; i++) {
            threads[i] = new Downloader.ThreadInfo(i * th_Len, th_Len, 0);
        }
        threads[i] = new Downloader.ThreadInfo(th_Len * i, info.contentLength - (th_Len * i), 0);
//        System.out.println(Arrays.toString(threads));
        return threads;
    }
}

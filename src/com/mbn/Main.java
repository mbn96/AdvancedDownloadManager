package com.mbn;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
            Downloader downloader = new Downloader(dlRequest);
            downloader.start();
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
//        Runtime.getRuntime().addShutdownHook();

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

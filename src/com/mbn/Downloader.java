package com.mbn;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Queue;

public class Downloader {

    //    private final Object msgQueueLOCK = new Object();
    private final InfoCatcher.DL_info dlInfo;
    private volatile boolean isRunning = true; // TODO: 5/7/21 change after stop...

    public Downloader(InfoCatcher.DL_info dlInfo) {
        this.dlInfo = dlInfo;
    }

    public static class ThreadInfo {
        public volatile int startIndex, length, downloaded;
        public final Object LOCK = new Object();

        public ThreadInfo() {
        }

        public ThreadInfo(ThreadInfo info) {
            this(info.startIndex, info.length, info.downloaded);
        }

        public ThreadInfo(int startIndex, int length, int downloaded) {
            this.startIndex = startIndex;
            this.length = length;
            this.downloaded = downloaded;
        }

        public ThreadInfo(String info) {
            String[] parts = info.split("-");
            this.startIndex = Integer.parseInt(parts[0]);
            this.length = Integer.parseInt(parts[1]);
            this.downloaded = Integer.parseInt(parts[2]);
        }

        public int getStartIndex() {
            return startIndex;
        }

        public void setStartIndex(int startIndex) {
            this.startIndex = startIndex;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public int getDownloaded() {
            return downloaded;
        }

        public void setDownloaded(int downloaded) {
            this.downloaded = downloaded;
        }

        @Override
        public String toString() {
            return String.format("%d-%d-%d", startIndex, length, downloaded);

        }
    }

    private class QMSG {
    }

    private class ThreadInfoHolder {
        private ThreadInfo threadInfo;
        private Queue<QMSG> msgQueue;
    }

    private class DownloaderThread extends ArguableRunnable<ThreadInfoHolder> {

        public DownloaderThread(ThreadInfoHolder arg) {
            super(arg);
        }

        @Override
        public void run(ThreadInfoHolder args) {
            HttpURLConnection httpClient = null;
            try {
                URL connectionURL = new URL(URLEncoder.encode(dlInfo.url, StandardCharsets.UTF_8));
                httpClient = (HttpURLConnection) connectionURL.openConnection();
                httpClient.setRequestMethod("GET");

                httpClient.addRequestProperty("range",
                        String.format("bytes=%d-%d",
                                args.threadInfo.startIndex + args.threadInfo.downloaded,
                                args.threadInfo.length - 1));

                httpClient.setConnectTimeout(10_000);
                httpClient.setReadTimeout(10_000);
                httpClient.setInstanceFollowRedirects(true);
                httpClient.connect();
                if (httpClient.getResponseCode() == HttpURLConnection.HTTP_PARTIAL || httpClient.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    boolean finished = false;
                    while (isRunning) {
                        synchronized (args.threadInfo.LOCK) {
                            QMSG msg = args.msgQueue.poll();
                            if (msg != null) {
                                // TODO: 5/7/21 implement ...
                            }
                        }


                    }
                    if (finished) {
                        // TODO: 5/7/21 finished ok...
                    } else {
                        // TODO: 5/7/21 probably stopped by the user...
                    }
                } else {
                    // TODO: 5/7/21 do something... bad response code...
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (httpClient != null) {
                    httpClient.disconnect();
                }
                QMSG msg;
                while ((msg = args.msgQueue.poll()) != null) {
                    // TODO: 5/7/21 reject request...
                }
            }
        }
    }
}

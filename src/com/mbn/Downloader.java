package com.mbn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Downloader {

    private static final int BUFF_SIZE = 1024 * 8;
    private final Object writeRequestQueueLOCK = new Object();
    private final InfoCatcher.DL_info dlInfo;
    private final DlRequest dlRequest;
    private final ThreadInfo[] threads;
    private final int threadCount;
    private volatile boolean isRunning = true; // TODO: 5/7/21 change after stop...
    private final MasterWriter masterWriter;
    private final SelfGeneratingPool<WriteRequest> requestPool = new SelfGeneratingPool<>() {
        @Override
        public WriteRequest getNew() {
            return new WriteRequest(new byte[BUFF_SIZE], 0, 0);
        }
    };

    private WriteRequest getWriteRequest() {
        synchronized (writeRequestQueueLOCK) {
            return requestPool.acquire();
        }
    }

    private void releaseWriteRequest(WriteRequest request) {
        synchronized (writeRequestQueueLOCK) {
            requestPool.release(request);
        }
    }


    public Downloader(DlRequest dlRequest) throws FileNotFoundException {
        this.dlRequest = dlRequest;
        this.dlInfo = dlRequest.dlInfo;
        this.threads = dlRequest.threads;
        this.threadCount = dlRequest.threadCount;
        masterWriter = new MasterWriter(dlRequest.downloadPath);
    }

    public static class ThreadInfo {
        public volatile int startIndex, length, downloaded;
        public final Object LOCK = new Object();
        public volatile boolean finished = false;

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

        public boolean isFinished() {
            return finished;
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
        private final AtomicBoolean result = new AtomicBoolean(false);
        // TODO: 5/8/21 implement...
    }

    public static class DlRequest {
        private ThreadInfo[] threads;
        private InfoCatcher.DL_info dlInfo;
        private int threadCount;
        private String downloadPath;

        public DlRequest(ThreadInfo[] threads, InfoCatcher.DL_info dlInfo, int threadCount, String dlPath) {
            this.threads = threads;
            this.dlInfo = dlInfo;
            this.threadCount = threadCount;
            this.downloadPath = dlPath;
        }

        public String getDownloadPath() {
            return downloadPath;
        }

        public void setDownloadPath(String downloadPath) {
            this.downloadPath = downloadPath;
        }

        public ThreadInfo[] getThreads() {
            return threads;
        }

        public void setThreads(ThreadInfo[] threads) {
            this.threads = threads;
        }

        public InfoCatcher.DL_info getDlInfo() {
            return dlInfo;
        }

        public void setDlInfo(InfoCatcher.DL_info dlInfo) {
            this.dlInfo = dlInfo;
        }

        public int getThreadCount() {
            return threadCount;
        }

        public void setThreadCount(int threadCount) {
            this.threadCount = threadCount;
        }
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
                    int download_temp;
                    InputStream inputStream = httpClient.getInputStream();
                    while (isRunning) {
                        synchronized (args.threadInfo.LOCK) {
                            QMSG msg = args.msgQueue.poll();
                            if (msg != null) {
                                // TODO: 5/7/21 implement ...
                            }
                        }
                        // TODO: 5/7/21 download...
                        if (args.threadInfo.downloaded < args.threadInfo.length) {
                            WriteRequest request_temp = getWriteRequest();
                            download_temp = Math.min(BUFF_SIZE, (args.threadInfo.length - args.threadInfo.downloaded));
                            download_temp = inputStream.read(request_temp.buff, 0, download_temp);
                            request_temp.setStartIndex_length(args.threadInfo.startIndex + args.threadInfo.downloaded, download_temp);
                            if (masterWriter.addRequest(request_temp)) {
                                download_temp += args.threadInfo.downloaded;
                                args.threadInfo.downloaded = download_temp;
                            } else {
                                // TODO: 5/8/21 probably stopped by the user...
                            }
                        } else {
                            finished = true;
                            args.threadInfo.finished = true;
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
            } catch (InterruptedException e) {
                e.printStackTrace();
                // TODO: 5/8/21 master writer has problem...
            } finally {
                if (httpClient != null) {
                    httpClient.disconnect();
                }
                QMSG msg;
                while ((msg = args.msgQueue.poll()) != null) {
                    // TODO: 5/7/21 reject request...
                    msg.result.set(false);
                    args.threadInfo.LOCK.notify();
                }
            }
        }
    }

    private class WriteRequest {
        private final byte[] buff;
        private volatile int startIndex, length;

        public WriteRequest(byte[] buff, int startIndex, int length) {
            this.buff = buff;
            this.startIndex = startIndex;
            this.length = length;
        }

        public void setStartIndex_length(int startIndex, int length) {
            this.startIndex = startIndex;
            this.length = length;
        }
    }

    public abstract static class SelfGeneratingPool<T> extends LinkedList<T> {
        private int capacity = 10;

        public SelfGeneratingPool(int capacity) {
            this.capacity = capacity;
        }

        public SelfGeneratingPool() {
        }

        public void release(T item) {
            if (size() < capacity) {
                add(item);
            }
        }

        public abstract T getNew();

        public T acquire() {
            T item = poll();
            if (item == null) item = getNew();
            return item;
        }

    }

    private class MasterWriter {
        private final RandomAccessFile randomAccessFile;
        private final String filePath;
        private final LinkedList<WriteRequest> requests = new LinkedList<>();
        private final Object QUEUE_LOCK = new Object();

        public MasterWriter(String filePath) throws FileNotFoundException {
            this.filePath = filePath;
            randomAccessFile = new RandomAccessFile(filePath, "rwd");
        }

        public boolean addRequest(WriteRequest request) throws InterruptedException {
            synchronized (QUEUE_LOCK) {
                if (isRunning) {
                    requests.add(request);
                    QUEUE_LOCK.notify();
                    return true;
                }
            }
            return false;
        }
    }


}

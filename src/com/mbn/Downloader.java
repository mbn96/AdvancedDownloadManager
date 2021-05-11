package com.mbn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class Downloader {

    private static final int MEG_BYTE = 1024 * 1024;

    private static final int BUFF_SIZE = 1024 * 8;
    private final Object writeRequestQueueLOCK = new Object();
    private final InfoCatcher.DL_info dlInfo;
    private final DlRequest dlRequest;
    //    private final ThreadInfo[] threads;
    private final int threadCount;
    private volatile boolean isRunning = true; // TODO: 5/7/21 change after stop...
    private boolean hasStarted = false;
    private final ArrayList<ThreadInfoHolder> threads = new ArrayList<>();
    private final Object threadListLock = new Object();
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
        for (ThreadInfo info : dlRequest.threads) {
            this.threads.add(new ThreadInfoHolder(info));
        }
        this.threadCount = dlRequest.threadCount;
        masterWriter = new MasterWriter(dlRequest.downloadPath);
    }

    public void start() {
        if (!hasStarted) {
            int activeParts = 0;
            for (ThreadInfoHolder th_info : threads) {
                if (!th_info.threadInfo.finished) {
                    startThread(th_info);
                    activeParts++;
                }
            }
            if (activeParts < threadCount) {
                for (int i = 0; i < threadCount - activeParts; i++) {
                    // TODO: 5/9/21 implement a method to check for Unfinished parts and try to divide them in two...
                }
            }
            hasStarted = true;
        }
    }

    public static class ThreadInfo {
        public volatile int startIndex, length, downloaded;
        public final Object LOCK = new Object();
        public volatile boolean finished = false;

        public ThreadInfo() {
        }

        @SuppressWarnings("CopyConstructorMissesField")
        public ThreadInfo(ThreadInfo info) {
            this(info.startIndex, info.length, info.downloaded);
        }

        public ThreadInfo(int startIndex, int length, int downloaded) {
            this.startIndex = startIndex;
            this.length = length;
            this.downloaded = downloaded;
            finished = this.downloaded >= this.length;
        }

        public ThreadInfo(String info) {
            String[] parts = info.split("-");
            this.startIndex = Integer.parseInt(parts[0]);
            this.length = Integer.parseInt(parts[1]);
            this.downloaded = Integer.parseInt(parts[2]);
            finished = this.downloaded >= this.length;
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

//    private class QMSG {
//        private volatile boolean result = false;
//        private volatile int startIndex;
//    }

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
        private final ThreadInfo threadInfo;
//        private final LinkedList<QMSG> msgQueue = new LinkedList<>();

        public ThreadInfoHolder(ThreadInfo threadInfo) {
            this.threadInfo = threadInfo;
        }
    }

    private void addToThreads(ThreadInfo info) {
        synchronized (threadListLock) {
            threads.add(new ThreadInfoHolder(info));
        }
    }

    private void startThread(ThreadInfoHolder infoHolder) {
        new Thread(new DownloaderThread(infoHolder)).start();
    }

    private class DownloaderThread extends ArguableRunnable<ThreadInfoHolder> {

        public DownloaderThread(ThreadInfoHolder arg) {
            super(arg);
        }

        @Override
        public void run(ThreadInfoHolder args) {
            HttpURLConnection httpClient = null;
            try {
                URL connectionURL = new URL(dlInfo.url);
                httpClient = (HttpURLConnection) connectionURL.openConnection();
                httpClient.setRequestMethod("GET");

                httpClient.addRequestProperty("range",
                        String.format("bytes=%d-%d",
                                args.threadInfo.startIndex + args.threadInfo.downloaded,
                                (args.threadInfo.startIndex + args.threadInfo.length) - 1));

                httpClient.setConnectTimeout(10_000);
                httpClient.setReadTimeout(10_000);
                httpClient.setInstanceFollowRedirects(true);
                httpClient.connect();
                if (httpClient.getResponseCode() == HttpURLConnection.HTTP_PARTIAL || httpClient.getResponseCode() == HttpURLConnection.HTTP_OK) {
//                    System.out.println(httpClient.getHeaderFields());
                    boolean finished = false;
                    int download_temp;
                    InputStream inputStream = httpClient.getInputStream();
                    while (isRunning && !finished) {
                        synchronized (args.threadInfo.LOCK) {
//                            QMSG msg = args.msgQueue.poll();
//                            if (msg != null) {
//                                // TODO: 5/7/21 implement ...
//                            }
//                        }
                            // TODO: 5/7/21 download...
                            if (args.threadInfo.downloaded < args.threadInfo.length) {
                                WriteRequest request_temp = getWriteRequest();
                                download_temp = Math.min(BUFF_SIZE, (args.threadInfo.length - args.threadInfo.downloaded));
//                                System.out.println("before: " + download_temp);
//                                download_temp = inputStream.read(request_temp.buff, 0, download_temp);
//                                System.out.println("after: " + download_temp);
                                JavaUtils.readFully(inputStream, request_temp.buff, download_temp);

                                request_temp.setStartIndex_length(args.threadInfo.startIndex + args.threadInfo.downloaded, download_temp);
                                if (masterWriter.addRequest(request_temp)) {
                                    download_temp += args.threadInfo.downloaded;
                                    args.threadInfo.downloaded = download_temp;
//                                    System.out.println(download_temp / MEG_BYTE);
                                } else {
                                    // TODO: 5/8/21 probably stopped by the user...
                                }
                            } else {
                                finished = true;
                                args.threadInfo.finished = true;
                            }
                        }
                    }
                    if (finished) {
                        // TODO: 5/7/21 finished ok...
                        System.out.println("finished... :) " + args.threadInfo);
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
//                QMSG msg;
//                while ((msg = args.msgQueue.poll()) != null) {
//                    // TODO: 5/7/21 reject request...
//                    msg.result = false;
//                    args.threadInfo.LOCK.notify();
//                }
            }
        }
    }


    private static class WriteRequest {
        //        private static final AtomicInteger test_ID = new AtomicInteger();
        private final byte[] buff;
        private volatile int startIndex, length;

        public WriteRequest(byte[] buff, int startIndex, int length) {
            this.buff = buff;
            this.startIndex = startIndex;
            this.length = length;
//            System.out.println(test_ID.incrementAndGet());
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
        private final Thread writerThread;
        private final Object QUEUE_LOCK = new Object();

        public MasterWriter(String filePath) throws FileNotFoundException {
            this.filePath = filePath;
            randomAccessFile = new RandomAccessFile(filePath, "rw");
            writerThread = new Thread(worker);
            writerThread.start();
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

        private final Runnable worker = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    WriteRequest request = null;
                    synchronized (QUEUE_LOCK) {
                        request = requests.poll();
                        if (request == null) {
                            if (isRunning) {
                                try {
                                    QUEUE_LOCK.wait(1000);
                                    request = requests.poll();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    // TODO: 5/9/21 Report back...
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    }
                    if (request != null) {
                        try {
                            randomAccessFile.seek(request.startIndex);
                            randomAccessFile.write(request.buff, 0, request.length);
//                            System.out.println("Wrote to file: " + request.startIndex + " <---> " + request.length);
                            releaseWriteRequest(request);
                        } catch (IOException e) {
                            e.printStackTrace();
                            // TODO: 5/9/21 Report back...
                            break;
                        }
                    }
                }
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Master Writer Out :))");
            }
        };

    }


}

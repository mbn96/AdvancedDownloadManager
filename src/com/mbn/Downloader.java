package com.mbn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Downloader {

    public static final int MEG_BYTE = 1024 * 1024;

    private static final int BUFF_SIZE = 1024 * 8;
    private final Object writeRequestQueueLOCK = new Object();
    private final InfoCatcher.DL_info dlInfo;
    //    private final ThreadInfo[] threads;
    private final int threadCount;
    private volatile boolean isRunning = true; // TODO: 5/7/21 change after stop...
    private boolean hasStarted = false;
    private final ArrayList<ThreadInfoHolder> threads = new ArrayList<>();
    private final Object threadListLock = new Object();
    private final MasterWriter masterWriter;
    private DownloadCallback downloadCallback;
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
        this.dlInfo = dlRequest.dlInfo;
        for (ThreadInfo info : dlRequest.threads) {
            this.threads.add(new ThreadInfoHolder(info));
        }
        this.threadCount = dlRequest.threadCount;
        masterWriter = new MasterWriter(dlRequest.downloadPath);
    }

    public Downloader(DlRequest dlRequest, DownloadCallback callback) throws FileNotFoundException {
        this(dlRequest);
        this.downloadCallback = callback;
    }

    public void setCallback(DownloadCallback callback) {
        this.downloadCallback = callback;
    }

    public void start() {
        if (!hasStarted) {
            int activeParts = 0;
            for (ThreadInfoHolder th_info : threads) {
                if (!th_info.threadInfo.isFinished()) {
                    startThread(th_info);
                    activeParts++;
                }
            }
            if (activeParts < threadCount) {
                for (int i = 0; i < threadCount - activeParts; i++) {
                    // TODO: 5/9/21 implement a method to check for Unfinished parts and try to divide them in two...
                    checkSplit();
                }
            }
            hasStarted = true;
            new Thread(progressReportThread).start();
        }
    }

    public void stop(boolean sync) {
        isRunning = false;
        if (sync) {
            synchronized (masterWriter.SHUT_DOWN_LOCK) {
                try {
                    masterWriter.SHUT_DOWN_LOCK.wait(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ThreadInfo[] getThreadInfo() {
        synchronized (threadListLock) {
            ThreadInfo[] threadInfo = new ThreadInfo[threads.size()];
            for (int i = 0; i < threads.size(); i++) {
                threadInfo[i] = threads.get(i).threadInfo;
            }
            return threadInfo;
        }
    }

    public static class ThreadInfo {
        public volatile int startIndex, length, downloaded;
        //        public final Object LOCK = new Object();
        private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
        public final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();
//        public volatile boolean finished = false;

        public ThreadInfo() {
        }

        public ThreadInfo(ThreadInfo info) {
            this(info.startIndex, info.length, info.downloaded);
        }

        public ThreadInfo(int startIndex, int length, int downloaded) {
            this.startIndex = startIndex;
            this.length = length;
            this.downloaded = downloaded;
//            finished = this.downloaded >= this.length;
        }

        public ThreadInfo(String info) {
            String[] parts = info.split("-");
            this.startIndex = Integer.parseInt(parts[0]);
            this.length = Integer.parseInt(parts[1]);
            this.downloaded = Integer.parseInt(parts[2]);
//            finished = this.downloaded >= this.length;
        }

        public boolean isFinished() {
            return this.downloaded >= this.length;
        }

        public int left() {
            return length - downloaded;
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

        public DlRequest() {
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

    private boolean isFinished() {
        synchronized (threadListLock) {
            for (ThreadInfoHolder h : threads) {
                if (!h.threadInfo.isFinished()) return false;
            }
        }
        return true;
    }

    private void calculateProgress(ProgressHolder holder) {
        synchronized (threadListLock) {
            int dlSum = 0;
            for (ThreadInfoHolder h : threads) {
                dlSum += h.threadInfo.downloaded;
            }
            holder.totalDownload = dlSum;
            holder.progress = dlSum / (float) (dlInfo.contentLength);
        }
    }

    private ThreadInfoHolder addToThreads(ThreadInfo info) {
        synchronized (threadListLock) {
            ThreadInfoHolder infoHolder = new ThreadInfoHolder(info);
            threads.add(infoHolder);
            return infoHolder;
        }
    }

    private synchronized void checkSplit() {
        ThreadInfo threadInfo;
        for (ThreadInfoHolder holder : threads) {
            threadInfo = holder.threadInfo;
            System.out.println("Inside split, Thread left: " + threadInfo.left());
            if (!threadInfo.isFinished() && (threadInfo.left()) >= MEG_BYTE) {

                threadInfo.writeLock.lock();
                System.out.println("Inside Lock, left:" + threadInfo.left());
                if (threadInfo.left() >= MEG_BYTE) {
                    int left = threadInfo.left();
                    int splitPart = left / 2;
                    ThreadInfo newInfo = new ThreadInfo(threadInfo.startIndex + threadInfo.downloaded + splitPart, left - splitPart, 0);
                    threadInfo.length = threadInfo.downloaded + splitPart;
                    ThreadInfoHolder newHolder = addToThreads(newInfo);
                    startThread(newHolder);
                    System.out.println("Split, New thread: " + newInfo);
                    threadInfo.writeLock.unlock();
                    break;
                }

            }
        }

    }

    private void startThread(ThreadInfoHolder infoHolder) {
        new Thread(new DownloaderThread(infoHolder)).start();
    }

    private class ProgressHolder {
        private float progress;
        private int totalDownload;
    }

    public interface DownloadCallback {
        /**
         * @param progress        Progress as a float in range 0-1
         * @param totalDownloaded The total downloaded in bytes.
         * @param speed           The speed in bytes per second.
         */
        void onProgress(float progress, int totalDownloaded, int speed);

        void onFinish();

        void onError(Exception e); // TODO: 5/12/21 Implement...
    }

    private final Runnable progressReportThread = new Runnable() {
        private final ProgressHolder progressHolder = new ProgressHolder();
        private long lastTime = System.currentTimeMillis();
        private int lastDlSize;

        @Override
        public void run() {
            while (isRunning) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(1000);
                    calculateProgress(progressHolder);
                    if (downloadCallback != null) {
                        downloadCallback.onProgress(progressHolder.progress, progressHolder.totalDownload, getSpeed());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private int getSpeed() {
            int newSpeed = ((int) ((progressHolder.totalDownload - lastDlSize) / (System.currentTimeMillis() - lastTime))) * 1000;
            lastDlSize = progressHolder.totalDownload;
            lastTime = System.currentTimeMillis();
            return newSpeed;
        }
    };

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

                httpClient.setConnectTimeout(60_000);
                httpClient.setReadTimeout(60_000);
                httpClient.setInstanceFollowRedirects(true);
                httpClient.connect();
                if (httpClient.getResponseCode() == HttpURLConnection.HTTP_PARTIAL || httpClient.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    int download_temp;
                    InputStream inputStream = httpClient.getInputStream();
                    while (isRunning && !args.threadInfo.isFinished()) {
                        args.threadInfo.writeLock.lock();
                        if (args.threadInfo.downloaded < args.threadInfo.length) {
                            WriteRequest request_temp = getWriteRequest();
                            download_temp = Math.min(BUFF_SIZE, (args.threadInfo.length - args.threadInfo.downloaded));
                            JavaUtils.readFully(inputStream, request_temp.buff, download_temp);
                            request_temp.setStartIndex_length(args.threadInfo.startIndex + args.threadInfo.downloaded, download_temp);
                            if (masterWriter.addRequest(request_temp)) {
                                download_temp += args.threadInfo.downloaded;
                                args.threadInfo.downloaded = download_temp;
                            } else {
                                // TODO: 5/8/21 probably stopped by the user...
                                System.out.println("Master writer sent false...");
                            }
                        }
                        args.threadInfo.writeLock.unlock();
                    }
                    if (args.threadInfo.isFinished()) {
                        reportThreadFinish(args);
                    } else {
                        // TODO: 5/7/21 probably stopped by the user...
                        System.out.printf("Thread exit without finish and no exception. IsRunning: %s%n", isRunning);
                        if (isRunning) {
                            // TODO: 5/13/21 See if it's just this thread... Very unlikely...
                        }
                    }
                } else {
                    System.out.printf("Bad response code: %d%n", httpClient.getResponseCode());
                    reportErr(new ErrorReport(ErrorReport.ERR_BAD_RES, args, null));
                }
            } catch (IOException | InterruptedException e) {
                if (e instanceof SocketTimeoutException) {
                    reportErr(new ErrorReport(ErrorReport.ERR_TIME_OUT, args, null));
                } else {
                    reportErr(new ErrorReport(ErrorReport.ERR_EXCEPTION, args, e));
                }
                e.printStackTrace();
            } finally {
                if (httpClient != null) {
                    httpClient.disconnect();
                }
            }
        }
    }


    private synchronized void reportErr(ErrorReport errorReport) {
        switch (errorReport.type) {
            case ErrorReport.ERR_BAD_RES:
                isRunning = false;
                break;
            case ErrorReport.ERR_TIME_OUT:
                // TODO: 5/13/21 add test to see if it's frequently happening...
                startThread(errorReport.holder);
                break;
            case ErrorReport.ERR_EXCEPTION:
                // TODO: 5/13/21 Probably fatal , but investigate more...
                isRunning = false;
                if (downloadCallback != null) {
                    downloadCallback.onError(errorReport.exception);
                }
                break;
            case ErrorReport.ERR_MASTER_WRITER:
                // TODO: 5/13/21 see if it is solvable...
                //noinspection DuplicateBranchesInSwitch
                isRunning = false;
                break;
        }
    }

    private synchronized void reportThreadFinish(ThreadInfoHolder infoHolder) {
        if (isRunning) {
            if (isFinished()) {
                isRunning = false;
                if (downloadCallback != null) {
                    downloadCallback.onFinish();
                }
            } else {
                // TODO: 5/12/21 implement a method to check for Unfinished parts and try to divide them in two...
                checkSplit();
            }
        }
    }

    private static class ErrorReport {
        static final int ERR_BAD_RES = 1;
        static final int ERR_TIME_OUT = ERR_BAD_RES << 1;
        static final int ERR_EXCEPTION = ERR_BAD_RES << 2;
        static final int ERR_MASTER_WRITER = ERR_BAD_RES << 3;

        private Exception exception;
        private int type;
        private ThreadInfoHolder holder;

        public ErrorReport(int type, ThreadInfoHolder holder, Exception exception) {
            this.exception = exception;
            this.type = type;
            this.holder = holder;
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
        public final Object SHUT_DOWN_LOCK = new Object();

        public MasterWriter(String filePath) throws FileNotFoundException {
            this.filePath = filePath;
            randomAccessFile = new RandomAccessFile(filePath, "rw");
            writerThread = new Thread(worker);
            writerThread.start();
        }

        public boolean addRequest(WriteRequest request) throws InterruptedException {
            synchronized (QUEUE_LOCK) {
                // Keep it inside for best result with the cancel or interrupt situations.
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
                            reportErr(new ErrorReport(ErrorReport.ERR_MASTER_WRITER, null, e));
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
                synchronized (SHUT_DOWN_LOCK) {
                    SHUT_DOWN_LOCK.notify();
                }
            }
        };

    }


}

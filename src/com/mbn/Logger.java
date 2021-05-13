package com.mbn;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Logger {

    public interface LogHeaderWriter {

        void writeHeader(OutputStream outputStream) throws IOException;

    }

    public interface LogBodyWriter {

        void writeBody(OutputStream outputStream) throws IOException;

    }

    public interface LogReader<I, T> {
        Map<I, T> readLog(InputStream inputStream) throws IOException;
    }

    private final String logFilePath;
    private final File logFile;
    private final LogHeaderWriter headerWriter;
    private final LogBodyWriter bodyWriter;

    // TODO: 5/6/21 Make it run on another thread...

    public Logger(String logFilePath, LogHeaderWriter headerWriter, LogBodyWriter bodyWriter) {
        this.logFilePath = logFilePath;
        logFile = new File(logFilePath);
        this.headerWriter = headerWriter;
        this.bodyWriter = bodyWriter;
        if (!logFile.getParentFile().exists()) {
            logFile.getParentFile().mkdirs();
        }
        if (logFile.exists()) {
            logFile.delete();
        }
    }

    public LogBodyWriter getBodyWriter() {
        return bodyWriter;
    }

    public LogHeaderWriter getHeaderWriter() {
        return headerWriter;
    }

    public static <I, T> Map<I, T> readLog(String filePath, LogReader<I, T> logReader) throws IOException {
        return logReader.readLog(new FileInputStream(filePath));
    }


    public static class DefaultHeaderWriter implements LogHeaderWriter {

        private Downloader.DlRequest dlRequest;

        public DefaultHeaderWriter(Downloader.DlRequest dlRequest) {
            this.dlRequest = dlRequest;
        }

        public DefaultHeaderWriter() {
        }

        public void setDlRequest(Downloader.DlRequest dlRequest) {
            this.dlRequest = dlRequest;
        }

        public Downloader.DlRequest getDlRequest() {
            return dlRequest;
        }

        @Override
        public void writeHeader(OutputStream outputStream) throws IOException {
            InfoCatcher.DL_info dl_info = dlRequest.getDlInfo();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

            writer.write(dl_info.url);
            writer.newLine();

            writer.write(String.valueOf(dl_info.contentLength));
            writer.newLine();

            writer.write(String.valueOf(dl_info.acceptsRanges));
            writer.newLine();

            writer.write(dlRequest.getDownloadPath());
            writer.newLine();

            writer.write(String.valueOf(dlRequest.getThreadCount()));
            writer.newLine();

            writer.flush();
        }

    }

    public static class DefaultBodyWriter implements LogBodyWriter {

        private Downloader.DlRequest dlRequest;

        public DefaultBodyWriter(Downloader.DlRequest dlRequest) {
            this.dlRequest = dlRequest;
        }

        public DefaultBodyWriter() {
        }

        public void setDlRequest(Downloader.DlRequest dlRequest) {
            this.dlRequest = dlRequest;
        }

        public Downloader.DlRequest getDlRequest() {
            return dlRequest;
        }

        @Override
        public void writeBody(OutputStream outputStream) throws IOException {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

            for (Downloader.ThreadInfo info : dlRequest.getThreads()) {
                writer.write(info.toString());
                writer.newLine();
            }

            writer.flush();
        }

    }

    public static class DefaultReader implements LogReader<String, Downloader.DlRequest> {
        public static final String DL_REQ = "dl_req";

        @Override
        public Map<String, Downloader.DlRequest> readLog(InputStream inputStream) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            Downloader.DlRequest dlRequest = new Downloader.DlRequest();
            InfoCatcher.DL_info dl_info = new InfoCatcher.DL_info();

            dl_info.url = reader.readLine();
            dl_info.contentLength = Integer.parseInt(reader.readLine());
            dl_info.acceptsRanges = Boolean.parseBoolean(reader.readLine());

            dlRequest.setDlInfo(dl_info);
            dlRequest.setDownloadPath(reader.readLine());
            dlRequest.setThreadCount(Integer.parseInt(reader.readLine()));

            ArrayList<Downloader.ThreadInfo> threadInfoList = new ArrayList<>();
            String threadStr;
            while ((threadStr = reader.readLine()) != null) {
                threadInfoList.add(new Downloader.ThreadInfo(threadStr));
            }

            dlRequest.setThreads(threadInfoList.toArray(new Downloader.ThreadInfo[0]));
            HashMap<String, Downloader.DlRequest> map = new HashMap<>();
            map.put(DL_REQ, dlRequest);

            reader.close();
            inputStream.close();

            return map;
        }
    }

}

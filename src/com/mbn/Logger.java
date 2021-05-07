package com.mbn;

import java.io.*;
import java.util.Map;

public class Logger {

    public interface LogHeaderWriter {

        void writeHeader(OutputStream outputStream);

    }

    public interface LogBodyWriter {

        void writeBody(OutputStream outputStream);

    }

    public interface LogReader<I, T> {
        Map<I, T> readLog(InputStream inputStream);
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

}

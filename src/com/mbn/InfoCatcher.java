package com.mbn;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class InfoCatcher {

    public static class DL_info {
        public int statusCode, contentLength;
        public String statusMsg, url, remoteFileName;
        public boolean hadErr = true;
        public boolean acceptsRanges;
        public Map<String, List<String>> headers;
        public Exception exception;

        @Override
        public String toString() {
            return "DL_info{" +
                    "statusCode=" + statusCode +
                    ", contentLength=" + contentLength +
                    ", statusMsg='" + statusMsg + '\'' +
                    ", hadErr=" + hadErr +
                    ", acceptsRanges=" + acceptsRanges +
                    ", headers=" + headers +
                    '}';
        }
    }


    public static DL_info catchInfo(String url) {
        HttpURLConnection client = null;
        DL_info info = new DL_info();
        try {
            URL connectionURL = new URL(URLEncoder.encode(url, StandardCharsets.UTF_8));
            info.url = connectionURL.toString();
            String path = connectionURL.getPath();
            info.remoteFileName = URLDecoder.decode(path.substring(path.lastIndexOf('/') + 1), StandardCharsets.UTF_8);
            client = (HttpURLConnection) connectionURL.openConnection();
            client.setRequestMethod("HEAD");
            client.setInstanceFollowRedirects(true);
            client.connect();
            info.statusCode = client.getResponseCode();
            info.statusMsg = client.getResponseMessage();
            info.headers = client.getHeaderFields();
            if (client.getResponseCode() == 200) {
                info.hadErr = false;
                info.contentLength = client.getHeaderFieldInt("content-length", 0);
                String range = client.getHeaderField("accept-ranges");
                info.acceptsRanges = range != null && range.equalsIgnoreCase("bytes");
            }
        } catch (IOException e) {
            e.printStackTrace();
            info.exception = e;
        } finally {
            if (client != null) {
                client.disconnect();
            }
        }
        return info;
    }
}

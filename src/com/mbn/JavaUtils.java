package com.mbn;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class JavaUtils {

    private final static char[] MULTIPART_CHARS =
            "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                    .toCharArray();

    @SuppressWarnings("unchecked")
    public static Method getMethod(Class c, String methodName, Class<?>... prams) {
        Method method = null;
        try {
            method = c.getMethod(methodName, prams);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return method;
    }

    @SuppressWarnings("unchecked")
    public static Method getDeclaredMethod(Class c, String methodName, Class<?>... prams) {
        Method method = null;
        try {
            method = c.getDeclaredMethod(methodName, prams);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return method;
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static String generateRandomString(int size) {
        StringBuilder buffer = new StringBuilder(size);
//        Random rand = new Random();
//        int count = rand.nextInt(11) + 30; // a random size from 30 to 40
        for (int i = 0; i < size; i++) {
//            buffer.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
            buffer.append(MULTIPART_CHARS[(int) (Math.random() * MULTIPART_CHARS.length)]);
        }
        return buffer.toString();
    }

    /**
     * Example of use:
     * <p>
     * System.out.println( String.format( "Number of words in the string \"%s\" is %s, words are:\n%s" , test, countWords( test ),
     * Arrays.toString( findWords( test ))));
     */
    public static String[] findWords(CharSequence input) {
        Pattern pattern = Pattern.compile("[a-z]*[a-z]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        ArrayList<String> words = new ArrayList<>();
        while (matcher.find()) {
            words.add(matcher.group());
        }
        return words.toArray(new String[0]);
    }

    public static long countWords(CharSequence input) {
        Pattern pattern = Pattern.compile("[a-z]*[a-z]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        long count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * @param input      The String to be reversed.
     * @param startIndex Inclusive.
     * @param endIndex   Exclusive.
     * @return The reversed StringBuilder.
     */
    public static StringBuilder reverse(CharSequence input, int startIndex, int endIndex) {
        if (startIndex >= endIndex) {
            return new StringBuilder();
        } else {
            StringBuilder stringBuilder = reverse(input, startIndex + 1, endIndex);
            stringBuilder.append(input.charAt(startIndex));
            return stringBuilder;
        }
    }

    public static char[] reverse(char[] input) {
        char[] output = new char[input.length];
        for (int i = input.length - 1; i >= 0; i--) {
            output[input.length - i - 1] = input[i];
        }
        return output;
    }

    public static StringBuilder reverseWords(String input) {
        Pattern pattern = Pattern.compile("[a-z]*[a-z]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        StringBuilder stringBuilder = new StringBuilder();
        int endIndex = 0;
        while (matcher.find()) {
            if (endIndex != matcher.start()) {
                stringBuilder.append(input, endIndex, matcher.start());
            }
            stringBuilder.append(reverse(matcher.group().toCharArray()));
            endIndex = matcher.end();
        }

        if (endIndex != input.length()) {
            stringBuilder.append(input, endIndex, input.length());
        }
        return stringBuilder;
    }

    public static StringBuilder reverseWords_old(String input) {
        Pattern pattern = Pattern.compile("[a-z]*[a-z]", Pattern.CASE_INSENSITIVE);
        String[] slices = input.split(" ");
        StringBuilder stringBuilder = new StringBuilder();
        for (String slice : slices) {
            Matcher matcher = pattern.matcher(slice);
            if (matcher.matches()) {
                stringBuilder.append(reverse(slice.toCharArray()));
            } else {
                matcher.reset();
                if (matcher.find()) {
                    stringBuilder.append(reverse(matcher.group().toCharArray()));
                    stringBuilder.append(slice.substring(matcher.end()));
                } else {
                    stringBuilder.append(slice);
                }
            }
            stringBuilder.append(' ');
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder;
    }


    public static long factorialRecursive(long n) {
        if (n <= 0) return 1;
        else return n * factorialRecursive(n - 1);
    }

    public static BigInteger factorialLoop(long n) {
        BigInteger fac = BigInteger.valueOf(1);
        for (int i = 1; i <= n; i++) {
            fac = fac.multiply(BigInteger.valueOf(i));
        }
        return fac;
    }

    public static double rangeChange(double value, double minRange, double maxRange, double destMinRange, double destMaxRange) {
        return (((value - minRange) / (maxRange - minRange)) * (destMaxRange - destMinRange)) + destMinRange;
    }

    public static double max(double... doubles) {
        double max = doubles[0];
        for (int i = 1; i < doubles.length; i++) {
            max = Math.max(max, doubles[i]);
        }
        return max;
    }

    public static double min(double... doubles) {
        double min = doubles[0];
        for (int i = 1; i < doubles.length; i++) {
            min = Math.min(min, doubles[i]);
        }
        return min;
    }

    public static double[] bringTo_1_Range(double... values) {
        if (values.length < 2) {
            throw new RuntimeException("Values are less then 2.");
        }
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        double[] outVs = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            outVs[i] = values[i] / sum;
        }
        return outVs;
    }

    public static void getIntBytes(byte[] buffer, int integer) {
        for (int i = 0; i < 4; i++) {
            int b = 0xff;
            int shifter = (3 - i) * 8;
            b <<= shifter;
            b &= integer;
            b >>>= shifter;
            buffer[i] = (byte) b;
        }
    }

    public static void getLongBytes(byte[] buffer, long longV) {
        for (int i = 0; i < 8; i++) {
            long b = 0xff;
            int shifter = (7 - i) * 8;
            b <<= shifter;
            b &= longV;
            b >>>= shifter;
            buffer[i] = (byte) b;
        }
    }

    public static int byteArrayToInt(byte[] bytes) {
        int _int = 0;
        int b;
        for (int i = 3; i >= 0; i--) {
            b = bytes[3 - i] & 0xff;
            int shifter = (i) * 8;
            _int |= (b << shifter);
        }
        return _int;
    }

    public static long byteArrayToLong(byte[] bytes) {
        long _long = 0;
        long b;
        for (int i = 7; i >= 0; i--) {
            b = bytes[7 - i] & 0xff;
            int shifter = (i) * 8;
            _long |= (b << shifter);
        }
        return _long;
    }

    public static void copyStream(InputStream inputStream, OutputStream outputStream, long length, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        long bytesRead = 0;
        long bytesRemained;
        int currentRead = 0;
        while (bytesRead < length) {
            bytesRemained = length - bytesRead;
            currentRead = buffer.length < bytesRemained ? buffer.length : (int) bytesRemained;
            currentRead = inputStream.read(buffer, 0, currentRead);
            outputStream.write(buffer, 0, currentRead);
            bytesRead += currentRead;
        }
        outputStream.flush();
    }

    public static void readFully(InputStream inputStream, byte[] b, int len) throws IOException {
        int n = 0;
        do {
            int count = inputStream.read(b, n, len - n);
            if (count < 0)
                throw new EOFException();
            n += count;
        } while (n < len);
    }

    public static void copyStream(InputStream inputStream, OutputStream outputStream, long length, byte[] buffer) throws IOException {
        long bytesRead = 0;
        long bytesRemained;
        int currentRead = 0;
        while (bytesRead < length) {
            bytesRemained = length - bytesRead;
            currentRead = buffer.length < bytesRemained ? buffer.length : (int) bytesRemained;
            currentRead = inputStream.read(buffer, 0, currentRead);
            outputStream.write(buffer, 0, currentRead);
            bytesRead += currentRead;
        }
        outputStream.flush();
    }

    public static void copyStream(InputStream inputStream, OutputStream outputStream, long length, int bufferSize, ProgressUpdate progressUpdate, int updateInterval) throws IOException {
        byte[] buffer = new byte[bufferSize];
        long bytesRead = 0;
        long bytesRemained;
        int currentRead = 0;
        int updateCount = 0;
        while (bytesRead < length) {
            bytesRemained = length - bytesRead;
            currentRead = buffer.length < bytesRemained ? buffer.length : (int) bytesRemained;
            currentRead = inputStream.read(buffer, 0, currentRead);
            outputStream.write(buffer, 0, currentRead);
            bytesRead += currentRead;
            updateCount += currentRead;
            if (updateCount >= updateInterval) {
                updateCount = 0;
                progressUpdate.onUpdate((float) bytesRead / length);
            }
        }
        outputStream.flush();
    }

    public static void zip(OutputStream outputStream, String baseDir, String... inputs) throws IOException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        zip_internal(zipOutputStream, baseDir, inputs);
        zipOutputStream.finish();
        zipOutputStream.close();
    }

    public static void zip_internal(ZipOutputStream zipOutputStream, String baseDir, String... inputs) throws IOException {
        if (baseDir == null) {
            baseDir = "";
        } else if (!baseDir.endsWith("/")) {
            baseDir = baseDir + "/";
        }
        byte[] buffer = new byte[1024 * 8];
        for (String input : inputs) {
            File file = new File(input);
            if (file.exists()) {
                if (file.isFile()) {
                    ZipEntry zipEntry = new ZipEntry(baseDir + file.getName());
                    zipEntry.setSize(file.length());
                    zipOutputStream.putNextEntry(zipEntry);
                    FileInputStream fileInputStream = new FileInputStream(file);
                    copyStream(fileInputStream, zipOutputStream, file.length(), buffer);
                    fileInputStream.close();
                } else if (file.isDirectory()) {
                    ZipEntry zipEntry = new ZipEntry(baseDir + file.getName() + "/");
                    zipOutputStream.putNextEntry(zipEntry);
                    File[] files = file.listFiles();
                    String[] paths = new String[files.length];
                    for (int i = 0; i < files.length; i++) {
                        paths[i] = files[i].getPath();
                    }
                    zip_internal(zipOutputStream, zipEntry.getName(), paths);
                }
            }
        }
    }

    public static void unzip(InputStream inputStream, String baseDir, boolean closeStream) throws IOException {
        if (baseDir == null) {
            baseDir = "";
        } else if (!baseDir.endsWith("/")) {
            baseDir = baseDir + "/";
        }
        byte[] buffer = new byte[1024 * 8];
        int len;
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            File file = new File(baseDir + zipEntry.getName());
            if (zipEntry.isDirectory()) {
                if (!file.exists()) {
                    file.mkdirs();
                }
            } else {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                while ((len = zipInputStream.read(buffer, 0, buffer.length)) > 0) {
                    fileOutputStream.write(buffer, 0, len);
                }
                fileOutputStream.flush();
                fileOutputStream.close();
            }
            zipInputStream.closeEntry();
        }
        if (closeStream) {
            zipInputStream.close();
            inputStream.close();
        }
    }

    public static void writeIntToStream(OutputStream outputStream, int... ints) throws IOException {
        byte[] buffer = new byte[4];
        for (int i : ints) {
            getIntBytes(buffer, i);
            outputStream.write(buffer);
            Arrays.fill(buffer, (byte) 0);
        }
        outputStream.flush();
    }

    public static void writeLongToStream(OutputStream outputStream, long... longs) throws IOException {
        byte[] buffer = new byte[8];
        for (long i : longs) {
            getLongBytes(buffer, i);
            outputStream.write(buffer);
            Arrays.fill(buffer, (byte) 0);
        }
        outputStream.flush();
    }

    public static int readIntFromStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(4);
        copyStream(inputStream, byteArrayOutputStream, 4, 4);
        return byteArrayToInt(byteArrayOutputStream.toByteArray());
    }

    public static long readLongFromStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8);
        copyStream(inputStream, byteArrayOutputStream, 8, 8);
        return byteArrayToLong(byteArrayOutputStream.toByteArray());
    }

    public static byte[] readByteArrayFromStream(InputStream inputStream, int length) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(4);
        copyStream(inputStream, byteArrayOutputStream, length, 1024);
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] encodeTo_UTF_8(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    public static String decode_UTF_8(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    public static <E> E[] putTogether(E[][] arrays) {
        if (arrays.length <= 0) {
            throw new RuntimeException("Empty array.");
        }
        int total_size = 0;
        for (E[] ea : arrays) {
            total_size += ea.length;
        }
        E[] outArray = (E[]) Array.newInstance(Objects.requireNonNull(arrays[0].getClass().getComponentType()), total_size);
        int count = 0;
        for (E[] ea : arrays) {
            System.arraycopy(ea, 0, outArray, count, ea.length);
            count += ea.length;
//            for (E e : ea) {
//                outArray[count++] = e;
//            }
        }
        return outArray;
    }

    public static int[] putTogether(int[][] arrays) {
        int total_size = 0;
        for (int[] ea : arrays) {
            total_size += ea.length;
        }
        int[] outArray = new int[total_size];
        int count = 0;
        for (int[] ea : arrays) {
            System.arraycopy(ea, 0, outArray, count, ea.length);
            count += ea.length;
//            for (E e : ea) {
//                outArray[count++] = e;
//            }
        }
        return outArray;
    }

    public static boolean isInt(Class<?> c) {
        return c == Integer.TYPE;
    }

    public static boolean isByte(Class<?> c) {
        return c == Byte.TYPE;
    }

    public static boolean isShort(Class<?> c) {
        return c == Short.TYPE;
    }

    public static boolean isLong(Class<?> c) {
        return c == Long.TYPE;
    }

    public static boolean isNonDecimal(Class<?> c) {
        return isByte(c) || isInt(c) || isShort(c) || isLong(c);
    }

    public static boolean isFloat(Class<?> c) {
        return c == Float.TYPE;
    }

    public static boolean isDouble(Class<?> c) {
        return c == Double.TYPE;
    }

    public static boolean isDecimal(Class<?> c) {
        return isFloat(c) || isDouble(c);
    }

    public static <T> Object getNonDecimalNumber(Long num, Class<T> cl) {
        if (isNonDecimal(cl)) {
            if (isByte(cl)) {
                return num.byteValue();
            }
            if (isInt(cl)) {
                return num.intValue();
            }
            if (isShort(cl)) {
                return num.shortValue();
            }
            if (isLong(cl)) {
                return num;
            }
        }
        return null;
    }

    public static <T> Object getDecimalNumber(Double num, Class<T> cl) {
        if (isDecimal(cl)) {
            if (isFloat(cl)) {
                return num.floatValue();
            }
            if (isDouble(cl)) {
                return num;
            }
        }
        return null;
    }

    public interface ProgressUpdate {
        void onUpdate(float pr);
    }

    public interface GetTypeArray<E> {
        E[] get(int size);
    }

    public static class FixedLengthInputStream extends InputStream {
        private final InputStream mInputStream;
        private final long length;
        /**
         * Should close the internal stream on close called.
         */
        private final boolean closeStream;
        private long position;
        private long markPosition;
        private long buffLong;
        private ProgressUpdate updateListener;
        private long updateInterval;
        private long previousPos;

        public FixedLengthInputStream(InputStream mInputStream, long length, boolean closeStream) {
            this.mInputStream = mInputStream;
            this.length = length;
            this.closeStream = closeStream;
        }

        public FixedLengthInputStream(InputStream mInputStream, long length, boolean closeStream, ProgressUpdate updateListener, long updateInterval) {
            this.mInputStream = mInputStream;
            this.length = length;
            this.closeStream = closeStream;
            this.updateListener = updateListener;
            this.updateInterval = updateInterval;
        }

        private void checkUpdate() {
            if (updateListener != null && Math.abs(position - previousPos) >= updateInterval) {
                previousPos = position;
                updateListener.onUpdate((float) position / length);
            }
        }

        @Override
        public int read() throws IOException {
            if (position < length) {
                position++;
                checkUpdate();
                return mInputStream.read();
            } else {
                return -1;
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (position < length) {
                buffLong = mInputStream.read(b, off, (int) Math.min(len, (length - position)));
                position += buffLong;
                checkUpdate();
                return (int) buffLong;
            } else {
                return 0;
            }
        }

        @Override
        public long skip(long n) throws IOException {
            if (position < length) {
                buffLong = mInputStream.skip(Math.min(n, (length - position)));
                position += buffLong;
                checkUpdate();
                return buffLong;
            } else return 0;
        }

        @Override
        public int available() throws IOException {
            return mInputStream.available();
        }

        @Override
        public void close() throws IOException {
            if (closeStream)
                mInputStream.close();
        }

        @Override
        public void mark(int readlimit) {
            markPosition = position;
            mInputStream.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            position = markPosition;
            checkUpdate();
            mInputStream.reset();
        }

        @Override
        public boolean markSupported() {
            return mInputStream.markSupported();
        }
    }

    public static class Blocker {

        private final Object LOCK = new Object();
        private volatile boolean done = false;

        public static Blocker getInstance() {
            return new Blocker();
        }

        public void block() {
            synchronized (LOCK) {
                if (!done) {
                    try {
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void unblock() {
            synchronized (LOCK) {
                done = true;
                LOCK.notify();
            }
        }

    }

}

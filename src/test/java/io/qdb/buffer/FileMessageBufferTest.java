package io.qdb.buffer;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static junit.framework.Assert.assertEquals;

public class FileMessageBufferTest {

    private static File dir = new File("build/test-data");

    @BeforeClass
    public static void beforeClass() throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Unable to create [" + dir + "]");
        }
    }

    @Test
    public void testAppend() throws IOException {
        FileMessageBuffer b = new FileMessageBuffer(mkdir("append"));
        b.setMaxFileSize(10000 + MessageFile.FILE_HEADER_SIZE);
        assertEquals(0, b.getFileCount());
        assertEquals(0L, b.getLength());
        assertEquals(10000 + MessageFile.FILE_HEADER_SIZE, b.getMaxFileSize());

        long ts = System.currentTimeMillis();
        assertEquals(0L, append(b, ts, "", 5000));
        assertEquals(5000L, append(b, ts, "", 5000));
        assertEquals(1, b.getFileCount());
        assertEquals(10000L + MessageFile.FILE_HEADER_SIZE, b.getLength());

        assertEquals(10000L, append(b, ts, "", 5000));
        assertEquals(2, b.getFileCount());
        assertEquals(15000L + MessageFile.FILE_HEADER_SIZE * 2, b.getLength());

        assertEquals(15000L, append(b, ts, "", 5000));
        assertEquals(2, b.getFileCount());
        assertEquals(20000L + MessageFile.FILE_HEADER_SIZE * 2, b.getLength());

        b.close();
    }

    @Test
    public void testOpenAndRead() throws IOException {
        File bd = mkdir("read");

        FileMessageBuffer b = new FileMessageBuffer(bd);
        b.setMaxFileSize(8192 + MessageFile.FILE_HEADER_SIZE);
        long ts = System.currentTimeMillis();
        append(b, ts, "", 4096);
        append(b, ts, "", 4096);
        b.close();

        expect(bd.list(), "0000000000000000.qdb");

        b = new FileMessageBuffer(bd);
        b.setMaxFileSize(8192 + MessageFile.FILE_HEADER_SIZE);
        append(b, ts, "", 4096);
        b.close();

        expect(bd.list(), "0000000000000000.qdb", "0000000000002000.qdb");
    }

    @Test
    public void testMoreThan512Files() throws IOException {
        File bd = mkdir("files512");

        FileMessageBuffer b = new FileMessageBuffer(bd);
        b.setMaxFileSize(8192 + MessageFile.FILE_HEADER_SIZE);
        long ts = System.currentTimeMillis();
        int n = 513;
        String[] expect = new String[n];
        for (int i = 0; i < n; i++) {
            append(b, ts, "", 8192);
            expect[i] = "00000000" + String.format("%08x", i * 8192) + ".qdb";
        }
        b.close();

        expect(bd.list(), expect);
    }

    private void expect(String[] actual, String... expected) {
        Arrays.sort(actual);
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("[" + i + "]", expected[i], actual[i]);
        }
    }

    private long append(FileMessageBuffer b, long timestamp, String key, int len) throws IOException {
        byte[] payload = new byte[len - 15 - key.length()];
        return b.append(timestamp, key, ByteBuffer.wrap(payload));
    }

    @SuppressWarnings("ConstantConditions")
    private File mkdir(String name) throws IOException {
        File f = new File(dir, name);
        if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                if (!file.delete()) throw new IOException("Unable to delete [" + file + "]");
            }
        }
        return f;
    }

}
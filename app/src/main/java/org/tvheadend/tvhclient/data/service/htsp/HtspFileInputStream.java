package org.tvheadend.tvhclient.data.service.htsp;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;

public class HtspFileInputStream extends InputStream {
    private final HtspConnection connection;
    private final String path;

    private long fileId;
    private long fileSize;

    private byte[] buf;
    private int bufPos;
    private long offset;

    public HtspFileInputStream(HtspConnection conn, String path)
            throws IOException {
        this.connection = conn;
        this.path = path;

        this.fileId = -1;
        this.fileSize = -1;

        this.reset();
        this.open();
    }

    public int available() {
        return this.buf.length;
    }

    public boolean markSupported() {
        return false;
    }

    public void reset() {
        buf = new byte[0];
        bufPos = 0;
        offset = 0;
    }

    class FileOpenResponse implements HtspResponseListener {
        int id;
        long size;
        long mtime;

        @Override
        public void handleResponse(HtspMessage response) {
            id = response.getInteger("id", 0);
            size = response.getLong("size", 0);
            mtime = response.getLong("mtime", 0);
            notifyAll();
        }
    }

    class FileReadResponse implements HtspResponseListener {
        byte[] data;

        @Override
        public void handleResponse(HtspMessage response) {
            data = response.getByteArray("data");
            notifyAll();
        }
    }

    class FileCloseResponse implements HtspResponseListener {

        @Override
        public void handleResponse(HtspMessage response) {
            notifyAll();
        }
    }

    private void open() throws IOException {
        HtspMessage request = new HtspMessage();
        FileOpenResponse response = new FileOpenResponse();

        request.setMethod("fileOpen");
        request.put("file", path);

        synchronized (response) {
            try {
                connection.sendMessage(request, response);
                response.wait();
                fileId = response.id;
                fileSize = response.size;
            } catch (Throwable e) {
                Log.e("TVHGuide", "Timeout waiting for fileOpen", e);
            }
        }

        if (fileId < 0) {
            throw new IOException("Failed to open remote file");
        } else if (fileId == 0) {
            throw new IOException("Remote file is missing");
        }
    }

    public void close() {
        HtspMessage request = new HtspMessage();
        FileCloseResponse response = new FileCloseResponse();

        request.setMethod("fileClose");
        request.put("id", fileId);

        synchronized (response) {
            try {
                connection.sendMessage(request, response);
                response.wait();
                fileId = -1;
                fileSize = -1;
            } catch (Throwable e) {
                Log.e("TVHGuide", "Timeout waiting for fileClose", e);
            }
        }
    }

    public int read(@NonNull byte[] outBuf, int outOffset, int outLength) {
        fillBuffer();

        int ret = Math.min(buf.length - bufPos, outLength - outOffset);
        if(ret > 0) {
            System.arraycopy(buf, bufPos, outBuf, outOffset, ret);
            bufPos += ret;
            return ret;
        }

        return -1;
    }

    @Override
    public int read() {
        fillBuffer();

        if (bufPos < buf.length) {
            return buf[bufPos++] & 0xff;
        }

        return -1;
    }

    private void fillBuffer() {
        if(bufPos < buf.length) {
            return;
        }

        HtspMessage request = new HtspMessage();
        FileReadResponse response = new FileReadResponse();

        request.setMethod("fileRead");
        request.put("id", fileId);
        request.put("size", Math.min(fileSize, 1024 * 1024 * 8));
        request.put("offset", offset);

        synchronized (response) {
            try {
                connection.sendMessage(request, response);
                response.wait();

                offset += buf.length;
                buf = response.data;
                bufPos = 0;
            } catch (Throwable e) {
                Log.e("TVHGuide", "Timeout waiting for fileRead", e);
            }
        }
    }
}

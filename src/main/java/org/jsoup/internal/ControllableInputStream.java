package org.jsoup.internal;

import org.jsoup.Progress;
import org.jsoup.helper.Validate;
import org.jspecify.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import static org.jsoup.internal.SharedConstants.DefaultBufferSize;

/**
 * A jsoup internal class (so don't use it as there is no contract API) that enables controls on a Buffered Input Stream,
 * namely a maximum read size, and the ability to Thread.interrupt() the read.
 */
// reimplemented from ConstrainableInputStream for JDK21 - extending BufferedInputStream will pin threads during read
public class ControllableInputStream extends FilterInputStream {
    private final InputStream buff;
    private final boolean capped;
    private final int maxSize;
    private long startTime;
    private long timeout = 0; // optional max time of request
    private int remaining;
    private int markPos;
    private boolean interrupted;

    // if we are tracking progress, will have the expected content length, progress callback, connection
    private @Nullable Progress<?> progress;
    private @Nullable Object progressContext;
    private int contentLength = -1;
    private int readPos = 0; // amount read; can be reset()

    private ControllableInputStream(InputStream in, int maxSize) {
        super(in);
        Validate.isTrue(maxSize >= 0);
        buff = in;
        capped = maxSize != 0;
        this.maxSize = maxSize;
        remaining = maxSize;
        markPos = -1;
        startTime = System.nanoTime();
    }

    /**
     * If this InputStream is not already a ControllableInputStream, let it be one.
     * @param in the input stream to (maybe) wrap
     * @param bufferSize the buffer size to use when reading
     * @param maxSize the maximum size to allow to be read. 0 == infinite.
     * @return a controllable input stream
     */
    public static ControllableInputStream wrap(InputStream in, int bufferSize, int maxSize) {
        // bufferSize currently unused; consider implementing as a min size in the SoftPool recycler
        if (in instanceof ControllableInputStream)
            return (ControllableInputStream) in;
        else if (in instanceof BufferedInputStream)
            return new ControllableInputStream(in, maxSize);
        else
            return new ControllableInputStream(new SimpleBufferedInput(in), maxSize);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (readPos == 0) emitProgress(); // emits a progress
        
        if (interrupted || capped && remaining <= 0)
            return -1;
        if (Thread.currentThread().isInterrupted()) {
            // interrupted latches, because parse() may call twice
            interrupted = true;
            return -1;
        }
        if (expired())
            throw new SocketTimeoutException("Read timeout");

        if (capped && len > remaining)
            len = remaining; // don't read more than desired, even if available

        try {
            final int read = super.read(b, off, len);
            if (read == -1) { // completed
                contentLength = readPos;
            } else {
                remaining -= read;
                readPos += read;
            }
            emitProgress();

            return read;
        } catch (SocketTimeoutException e) {
            if (expired())
                throw e;
            return 0;
        }
    }

    /**
     * Reads this inputstream to a ByteBuffer. The supplied max may be less than the inputstream's max, to support
     * reading just the first bytes.
     */
    public static ByteBuffer readToByteBuffer(InputStream in, int max) throws IOException {
        Validate.isTrue(max >= 0, "maxSize must be 0 (unlimited) or larger");
        Validate.notNull(in);
        final boolean capped = max > 0;
        final byte[] readBuf = SimpleBufferedInput.BufferPool.borrow(); // Share the same byte[] pool as SBI
        final int outSize = capped ? Math.min(max, DefaultBufferSize) : DefaultBufferSize;
        ByteBuffer outBuf = ByteBuffer.allocate(outSize);

        try {
            int remaining = max;
            int read;
            while ((read = in.read(readBuf, 0, capped ? Math.min(remaining, DefaultBufferSize) : DefaultBufferSize)) != -1) {
                if (outBuf.remaining() < read) { // needs to grow
                    int newCapacity = (int) Math.max(outBuf.capacity() * 1.5, outBuf.capacity() + read);
                    ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
                    outBuf.flip();
                    newBuffer.put(outBuf);
                    outBuf = newBuffer;
                }
                outBuf.put(readBuf, 0, read);
                if (capped) {
                    remaining -= read;
                    if (remaining <= 0) break;
                }
            }
            outBuf.flip(); // Prepare the buffer for reading
            return outBuf;
        } finally {
            SimpleBufferedInput.BufferPool.release(readBuf);
        }
    }

    @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod") // not synchronized in later JDKs
    @Override public void reset() throws IOException {
        super.reset();
        remaining = maxSize - markPos;
        readPos = markPos; // readPos is used for progress emits
    }

    @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod") // not synchronized in later JDKs
    @Override public void mark(int readlimit) {
        super.mark(readlimit);
        markPos = maxSize - remaining;
    }

    public ControllableInputStream timeout(long startTimeNanos, long timeoutMillis) {
        this.startTime = startTimeNanos;
        this.timeout = timeoutMillis * 1000000;
        return this;
    }

    private void emitProgress() {
        if (progress == null) return;
        // calculate percent complete if contentLength > 0 (and cap to 100.0 if totalRead > contentLength):
        float percent = contentLength > 0 ? Math.min(100f, readPos * 100f / contentLength) : 0;
        //noinspection unchecked
        ((Progress<Object>) progress).onProgress(readPos, contentLength, percent, progressContext); // (not actually unchecked - verified when set)
        if (percent == 100.0f) progress = null; // detach once we reach 100%, so that any subsequent buffer hits don't report 100 again
    }

    public <ProgressContext> ControllableInputStream onProgress(int contentLength, Progress<ProgressContext> callback, ProgressContext context) {
        Validate.notNull(callback);
        Validate.notNull(context);
        this.contentLength = contentLength;
        this.progress = callback;
        this.progressContext = context;
        return this;
    }

    private boolean expired() {
        if (timeout == 0)
            return false;

        final long now = System.nanoTime();
        final long dur = now - startTime;
        return (dur > timeout);
    }

    public BufferedInputStream inputStream() {
        // called via HttpConnection.Response.bodyStream(), needs an OG BufferedInputStream
        if (buff instanceof BufferedInputStream)
            return (BufferedInputStream) buff; // if originally supplied a BIS in .wrap()
        else return new BufferedInputStream(buff);
    }
}

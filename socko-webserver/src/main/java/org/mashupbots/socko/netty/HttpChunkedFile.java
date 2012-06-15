/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.mashupbots.socko.netty;

import static org.jboss.netty.buffer.ChannelBuffers.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.jboss.netty.channel.FileRegion;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.DefaultHttpChunkTrailer;
import org.jboss.netty.handler.stream.ChunkedInput;

/**
 * A {@link ChunkedInput} that fetches data from a file chunk by chunk and send it as a HttpChunk
 * <p>
 * If your operating system supports
 * <a href="http://en.wikipedia.org/wiki/Zero-copy">zero-copy file transfer</a>
 * such as {@code sendfile()}, you might want to use {@link FileRegion} instead.
 */
public class HttpChunkedFile implements ChunkedInput {

    private final RandomAccessFile file;
    private final long startOffset;
    private final long endOffset;
    private final int chunkSize;
    private volatile long offset;
    private volatile boolean sentLastChunk = false;
    static final int DEFAULT_CHUNK_SIZE = 8192;

    /**
     * Creates a new instance that fetches data from the specified file.
     */
    public HttpChunkedFile(File file) throws IOException {
        this(file, DEFAULT_CHUNK_SIZE);
    }

    /**
     * Creates a new instance that fetches data from the specified file.
     *
     * @param chunkSize the number of bytes to fetch on each
     *                  {@link #nextChunk()} call
     */
    public HttpChunkedFile(File file, int chunkSize) throws IOException {
        this(new RandomAccessFile(file, "r"), chunkSize);
    }

    /**
     * Creates a new instance that fetches data from the specified file.
     */
    public HttpChunkedFile(RandomAccessFile file) throws IOException {
        this(file, DEFAULT_CHUNK_SIZE);
    }

    /**
     * Creates a new instance that fetches data from the specified file.
     *
     * @param chunkSize the number of bytes to fetch on each
     *                  {@link #nextChunk()} call
     */
    public HttpChunkedFile(RandomAccessFile file, int chunkSize) throws IOException {
        this(file, 0, file.length(), chunkSize);
    }

    /**
     * Creates a new instance that fetches data from the specified file.
     *
     * @param offset the offset of the file where the transfer begins
     * @param length the number of bytes to transfer
     * @param chunkSize the number of bytes to fetch on each
     *                  {@link #nextChunk()} call
     */
    public HttpChunkedFile(RandomAccessFile file, long offset, long length, int chunkSize) throws IOException {
        if (file == null) {
            throw new NullPointerException("file");
        }
        if (offset < 0) {
            throw new IllegalArgumentException(
                    "offset: " + offset + " (expected: 0 or greater)");
        }
        if (length < 0) {
            throw new IllegalArgumentException(
                    "length: " + length + " (expected: 0 or greater)");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException(
                    "chunkSize: " + chunkSize +
                    " (expected: a positive integer)");
        }

        this.file = file;
        this.offset = startOffset = offset;
        endOffset = offset + length;
        this.chunkSize = chunkSize;

        file.seek(offset);
    }

    /**
     * Returns the offset in the file where the transfer began.
     */
    public long getStartOffset() {
        return startOffset;
    }

    /**
     * Returns the offset in the file where the transfer will end.
     */
    public long getEndOffset() {
        return endOffset;
    }

    /**
     * Returns the offset in the file where the transfer is happening currently.
     */
    public long getCurrentOffset() {
        return offset;
    }

    public boolean hasNextChunk() throws Exception {
        if (offset < endOffset && file.getChannel().isOpen()) {
        	return true;
        } else {
        	return !sentLastChunk;
        }
    }

    public boolean isEndOfInput() throws Exception {
        return !hasNextChunk();
    }

    public void close() throws Exception {
        file.close();
    }

    public Object nextChunk() throws Exception {
        long offset = this.offset;
        if (offset >= endOffset) {
        	if (sentLastChunk){
                return null;        		
        	} else {
        		sentLastChunk = true;
        		return new DefaultHttpChunkTrailer();
        	}
        }

        int chunkSize = (int) Math.min(this.chunkSize, endOffset - offset);
        byte[] chunk = new byte[chunkSize];
        file.readFully(chunk);
        this.offset = offset + chunkSize;
        return new DefaultHttpChunk(wrappedBuffer(chunk));
    }
}

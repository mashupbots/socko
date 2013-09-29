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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.stream.ChunkedInput;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A {@link ChunkedInput} that fetches data from a file chunk by chunk.
 * <p>
 * This is a copy of ChunkedFile - but returns a HTTP Chunk instead of a buffer.
 * This is used for streaming HTTP responses in chunks. 
 * <p>
 * Just using ChunkedFile will not work because it does not format the chunk into 
 * HTTP chunks and it does not send DefaultLastHttpContent. 
 * <p>
 * See http://en.wikipedia.org/wiki/Chunked_transfer_encoding
 * <p>
 * If your operating system supports
 * <a href="http://en.wikipedia.org/wiki/Zero-copy">zero-copy file transfer</a>
 * such as {@code sendfile()}, you might want to use {@link FileRegion} instead.
 */
public class HttpChunkedFile implements ChunkedInput<HttpContent> {

	static final int DEFAULT_CHUNK_SIZE = 8192;
	
    private final RandomAccessFile file;
    private final long startOffset;
    private final long endOffset;
    private final int chunkSize;
    private long offset;
    private volatile boolean sentLastChunk = false;

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
     *                  {@link #readChunk(ChannelHandlerContext)} call
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
     *                  {@link #readChunk(ChannelHandlerContext)} call
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
     *                  {@link #readChunk(ChannelHandlerContext)} call
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
    public long startOffset() {
        return startOffset;
    }

    /**
     * Returns the offset in the file where the transfer will end.
     */
    public long endOffset() {
        return endOffset;
    }

    /**
     * Returns the offset in the file where the transfer is happening currently.
     */
    public long currentOffset() {
        return offset;
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        if (offset < endOffset && file.getChannel().isOpen()) {
        	return false;
        } else {
        	// Only end of input after last HTTP chunk has been sent
        	return sentLastChunk;
        }        
    }

    @Override
    public void close() throws Exception {
        file.close();
    }

    @Override
    public HttpContent readChunk(ChannelHandlerContext ctx) throws Exception {
        long offset = this.offset;
        if (offset >= endOffset) {
        	if (sentLastChunk){
                return null;        		
        	} else {
        		// Send last chunk for this file
        		sentLastChunk = true;
        		return new DefaultLastHttpContent();
        	}
        }

        int chunkSize = (int) Math.min(this.chunkSize, endOffset - offset);
        // Check if the buffer is backed by an byte array. If so we can optimize it a bit an safe a copy

        ByteBuf buf = ctx.alloc().heapBuffer(chunkSize);
        boolean release = true;
        try {
            file.readFully(buf.array(), buf.arrayOffset(), chunkSize);
            buf.writerIndex(chunkSize);
            this.offset = offset + chunkSize;
            release = false;
            return new DefaultHttpContent(buf);
        } finally {
            if (release) {
                buf.release();
            }
        }
        
    }
}

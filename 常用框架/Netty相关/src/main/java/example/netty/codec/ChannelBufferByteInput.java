package example.netty.codec;

import io.netty.buffer.ByteBuf;
import org.jboss.marshalling.ByteInput;

import java.io.IOException;
/**
 * {@link ByteInput} implementation which reads its data from a {@link ByteBuf}
 */
public class ChannelBufferByteInput implements ByteInput {
    private final ByteBuf buffer;

    public ChannelBufferByteInput(ByteBuf buffer) {
        this.buffer = buffer;
    }

    @Override
    public int read() throws IOException {
        if (buffer.isReadable()) {
            return buffer.readByte() & 0xff;
        }
        return -1;
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        return read(bytes, 0, bytes.length);
    }

    @Override
    public int read(byte[] dst, int dstIndex, int length) throws IOException {
        int available = available();
        if (available == 0) return -1;

        length = Math.min(length, available);
        buffer.readBytes(dst, dstIndex, length);
        return length;
    }

    @Override
    public int available() throws IOException {
        return buffer.readableBytes();
    }

    @Override
    public long skip(long skip) throws IOException {
        int readable = buffer.readableBytes();
        if (readable < skip) {
            skip = readable;
        }
        buffer.readerIndex((int) (buffer.readerIndex() + skip));
        return skip;
    }

    @Override
    public void close() throws IOException {

    }
}

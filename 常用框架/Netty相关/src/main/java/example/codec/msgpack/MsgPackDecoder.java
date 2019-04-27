package example.codec.msgpack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.msgpack.MessagePack;

import java.util.List;

public class MsgPackDecoder extends MessageToMessageDecoder<ByteBuf> {

    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf msg, List<Object> list) throws Exception {
        int len = msg.readableBytes();
        byte[] array = new byte[len];
        msg.getBytes(msg.readerIndex(), array, 0, len);
        MessagePack messagePack = new MessagePack();
        list.add(messagePack.read(array));
    }
}

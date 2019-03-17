package example.netty.server;

import example.netty.NettyConstant;
import example.netty.codec.NettyMessageDecoder;
import example.netty.codec.NettyMessageEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class NettyServer {
    public static void main(String[] args) throws Exception {
        new NettyServer().bind();
    }

    private void bind() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new NettyMessageDecoder(1024 * 1024, 4, 4))
                                    .addLast(new NettyMessageEncoder())
                                    .addLast("readTimeoutHandler", new ReadTimeoutHandler(50))
                                    .addLast(new LoginAuthRespHandler())
                                    .addLast("HeartBeatHandler", new HeartBeatRespHandler());
                        }
                    });

            // 绑定端口， 同步等待成功
            ChannelFuture future = b.bind(NettyConstant.REMOTEIP, NettyConstant.REMOTEPORT).sync();
            System.out.println("Netty server start ok : " + (NettyConstant.REMOTEIP + " : " + NettyConstant.REMOTEIP));
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}

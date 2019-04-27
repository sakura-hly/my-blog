package example.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
import static io.netty.handler.codec.http.HttpUtil.setContentLength;
import static io.netty.handler.codec.rtsp.RtspResponseStatuses.*;

public class HttpFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final String url;
    // 非法uri正则
    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
    // 文件是否呗允许访问下载
    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9.]*");

    public HttpFileServerHandler(String url) {
        this.url = url;
    }

//    @Override
//    protected void messageReceived(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
//
//    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest request) throws Exception {
        // 对请求的解码结果进行判断
        if (!request.decoderResult().isSuccess()) {
            // 400
            sendError(channelHandlerContext, BAD_REQUEST);
            return;
        }

        // 对请求方式进行判断：如果不是get方式，则返回异常
        if (request.method() != GET) {
            // 405
            sendError(channelHandlerContext, METHOD_NOT_ALLOWED);
            return;
        }

        // 获取请求uri路径
        final String uri = request.uri();
        // 对uri进行分析， 返回本地系统
        final String path = sanitizeUri(uri);
        // 如果路径构造不合法，
        if (path == null) {
            // 403
            sendError(channelHandlerContext, FORBIDDEN);
            return;
        }

        // 创建file对象
        File file = new File(path);
        // 判断文件是否隐藏或者不存在
        if (file.isHidden() || !file.exists()) {
            // 404
            sendError(channelHandlerContext, NOT_FOUND);
            return;
        }

        // 如果是文件夹
        if (file.isDirectory()) {
            if (uri.endsWith("/")) {
                sendListing(channelHandlerContext, file);
            } else {
                sendRedirect(channelHandlerContext, uri + "/");
            }
        }

        // 如果所创建的file对象不是文件类型
        if (!file.isFile()) {
            // 403
            sendError(channelHandlerContext, FORBIDDEN);
            return;
        }

        // 随机文件读写类
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException fnfe) {
            // 404
            sendError(channelHandlerContext, NOT_FOUND);
            return;
        }

        // 获取文件长度
        long fileLength = randomAccessFile.length();
        // 建立响应对象
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, OK);
        // 设置响应信息
        setContentLength(response, fileLength);
        setContentTypeHeader(response, file);
        // 如果一直保持连接则设置响应头信息为: HttpHeaders.Values.KEEP_ALIVE
        if (isKeepAlive(request)) {
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        // 进行写入
        channelHandlerContext.write(response);

        // 构造发送文件线程，将文件写入到Chunked缓冲区
        ChannelFuture sendFileFuture;
        // 写出ChunkedFile
        sendFileFuture = channelHandlerContext.write(
                new ChunkedFile(randomAccessFile, 0, fileLength, 8192),
                channelHandlerContext.newProgressivePromise());
        // 添加传输监听
        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
                if (total < 0) {
                    System.err.println("Transfer progress: " + progress);
                } else {
                    System.err.println("Transfer progress: " + progress + " / " + total);
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture channelProgressiveFuture) throws Exception {
                System.err.println("Transfer complete.");
            }
        });

        // 如果使用Chunked编码，最后需要发送一个编码结束的看空消息体，进行标记，表示所有消息体已经成功发送完成
        ChannelFuture lastContentFuture = channelHandlerContext.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        // 如果当前连接请求非Keep-Alive，最后一包消息发送完成后，服务器主动关闭连接
        if (!HttpUtil.isKeepAlive(request)) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void setContentTypeHeader(HttpResponse response, File file) {
        // 使用mime对象获取文件类型
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        response.headers().set(CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
            ctx.close();
        }
    }

    private void sendListing(ChannelHandlerContext channelHandlerContext, File dir) {
        // 建立响应对象
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, OK);
        // 响应头
        response.headers().set(CONTENT_TYPE, "text/html, charset=UTF-8");
        // 追加文本内容
        StringBuilder sb = new StringBuilder();
        String dirPath = dir.getPath();
        sb.append("<!DOCTYPE html>\r\n");
        sb.append("<html><head><meta charset=\"UTF-8\"><title>");
        sb.append(dirPath);
        sb.append(" -> Directory: ");
        sb.append("</title></head><body>\r\n");
        sb.append("<h3>");
        sb.append(dirPath).append(" -> Directory: ");
        sb.append("</h3>\r\n");
        sb.append("<ul>");
        sb.append("<li>link: <a href=\"../\">..</a></li>\r\n");

        for (File f : dir.listFiles()) {
            //1. 跳过隐藏文件或不可读文件
            if (f.isHidden() || !f.canRead()) {
                continue;
            }
            String name = f.getName();
            // 2. 如果不被允许，跳过此文件
            if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
                continue;
            }

            sb.append("<li>link: <a href=\"");
            sb.append(name);
            sb.append("\">");
            sb.append(name);
            sb.append("</a></li>\r\n");
        }

        sb.append("</ul></body></html>\r\n");

        // 构造结构，写入缓冲区
        ByteBuf byteBuf = Unpooled.copiedBuffer(sb, CharsetUtil.UTF_8);
        // 进行写出操作
        response.content().writeBytes(byteBuf);
        // 重置写出操作
//        response.release();
        channelHandlerContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void sendRedirect(ChannelHandlerContext channelHandlerContext, String newUri) {
        // 建立响应对象
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, FOUND);
        // 设置新的请求地址放入响应对象中去
        response.headers().set(LOCATION, newUri);
        // 使用ctx对象写出并且刷新
        channelHandlerContext.write(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * @param uri
     * @return
     */
    private String sanitizeUri(String uri) {
        try {
            // 使用UTF-8字符集
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            try {
                // 尝试ISO-8859-1
                uri = URLDecoder.decode(uri, "ISO-8859-1");
            } catch (UnsupportedEncodingException e1) {
                throw new Error();
            }
        }

        // 对uri进行细粒度判断
        // 1.基础验证
        if (!uri.startsWith(url)) return null;
        // 2.基础验证
        if (!uri.startsWith("/")) return null;
        // 3. 将文件分隔符替换为本地操作系统的文件路径分隔符
        uri = uri.replace('/', File.separatorChar);
        // 4. 二次验证合法性
        if (uri.contains(File.separator + '.')
                || uri.contains('.' + File.separator) || uri.startsWith(".")
                || uri.endsWith(".") || INSECURE_URI.matcher(uri).matches()) {
            return null;
        }
        // 当前工程所在目录 + URI构造绝对路径
        return System.getProperty("user.dir") + File.separator + uri;
    }

    private void sendError(ChannelHandlerContext channelHandlerContext, HttpResponseStatus status) {
        // 建立响应对象
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
        // 设置响应头信息
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        // 使用ctx对象写出并且刷新到socketChannel中去，并主动关闭连接（这里是指关闭处理发送数据的线程连接）
        channelHandlerContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}

# ChannelHandler及相关类学习
ChannelHandler类似于Servlet的Filter过滤器，负责对I/O事件或者I/O操作进行拦截和处理，它可以选择性地
拦截和处理自己感兴趣的事件，也可以透传和终止事件的传递。

基于ChannelHandler接口，用户可以方便地进行业务逻辑定制，例如打印日志、统一封装异常信息、性能统计和消息编解码等。
ChannelHandler支持注解，目前有两个
* Sharable：多个ChannelPipeline共用一个ChannelHandler
* Skip：被Skip注解的方法不会被调用，直接被忽略

## ChannelHandler功能说明
   基于ChannelHandler接口，Netty提供了ChannelHandlerAdapter基类，它的所有接口实现都是方法透传，用户只需要实现自己关心的方法便可。
   
   1. ByteToMessageDecoder功能说明
      
      利用NIO进行网络编程时，往往需要将读取到的字节数组或字节缓冲区解码为业务可用的POJO对象。为了方便业务将ByteBuf解码成业务POJO对象，
      Netty提供了ByteToMessageDecoder抽象工具解码类。
      
      用户的解码类继承ByteToMessageDecoder，只需要实现protected abstract void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
      抽象方法即可完成ByteBuf到POJO对象的编码。
      
      由于ByteToMessageDecoder并没有考虑TCP粘包和组包等场景，读半包需要用户解码器自己负责处理。正因如此，大多数场景不会直接继承ByteToMessageDecoder，而是
      继承另外一些更高级的解码器来屏蔽半包的处理。
   
   2. MessageToMessageDecoder功能说明
    
      MessageToMessageDecoder实际上是Netty的二次解码器，它的职责是将一个对象二次解码为其它对象。
      
      为什么叫二次解码器呢？我们知道，从SocketChannel读取到的TCP数据报是ByteBuffer，实际就是字节数组，我们首先需要将ByteBuffer缓冲区中的数据报读取出来，
      并将其解码为Java对象，然后将Java对象根据某些规则做二次编码，将其解码为一个POJO对象，因为MessageToMessageDecoder在ByteToMessageDecoder之后，
      所以叫二次解码器。
      
   3. LengthFieldBasedFrameDecoder功能说明
      
      LengthFieldBasedFrameDecoder是一种通用的半包处理器。
      如何区分一个整包消息：
      * 固定长度，例如每120个字节代表一个整包消息，不足的前面补零。解码器在处理这类定长消息时比较简单，每次读到指定的长度在解码。
      * 通过回车换行符区分消息，例如FTP协议。这类区分消息的方式多用于文本协议。
      * 通过分隔符区分整包消息
      * 通过指定长度来标识整包消息
      
      如果消息时通过长度进行区分的，LengthFieldBasedFrameDecoder都可以自动处理粘包和半包问题。只需要传入正确的参数，即可轻松搞定“读半包”问题。
      
      通过lengthFieldOffset、lengthFieldLength、lengthAdjustment和initialBytesToStrip四个参数的不同组合，可用达到不同的解码效果。
      
   4. MessageToByteEncoder功能说明
   
      MessageToByteEncoder负责将POJO对象编码成ByteBuf，用户的编码器继承MessageToByteEncoder，实现protected abstract void encode
      (ChannelHandlerContext ctx, I msg, ByteBuf out) throws Exception接口，
      
   5. MessageToMessageEncoder功能说明
   
      将一个POJO对象编码成另一个对象，用户的编码器继承MessageToMessageEncoder，protected abstract void encode(ChannelHandlerContext ctx, I msg, List<Object> out) throws Exception
      方法即可。
      
   6. LengthFieldPrepender功能说明
   
      如果协议中的第一个字段为长度字段，Netty通过了LengthFieldPrepender编码器，它可以计算当前待发送的二进制字节长度，将该长度添加到ByteBuf的缓冲区头。
      
      通过设置LengthFieldPrepender为true，消息长度将包含长度字段本身占用的字节数。
      

## ChannelHandler源码分析
   
   1. ChannelHandler类关系继承图
      
      由于ChannelHandler是Netty框架和用户代码的主要扩展和定制点，所以它的子类种类繁多，功能各异，系统ChannelHandler主要分类如下：
      * ChannelPipeline的系统ChannelHandler，用于处理I/O操作和对事件预处理，对于用户不可见，这类ChannelHandler主要包括HeadHandler和TailHandler
      * 解编码Handler，包括ByteToMessageCodec、MessageToMessageDecoder等
      * 其它系统功能性ChannelHandler，包括流量整形Handler、读写超时Handler、日志Handler等。
      
   2. ByteToMessageDecoder源码解析
      
      该类主要将ByteBuf解码成POJO对象，来看channelRead方法
      ```
      public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
          if (msg instanceof ByteBuf) { // 如果传入的对象不是ByteBuf，直接透传
              CodecOutputList out = CodecOutputList.newInstance();
              try {
                  ByteBuf data = (ByteBuf) msg;
                  first = cumulation == null;
                  if (first) {
                      cumulation = data;
                  } else { 
                      // cumulation不为空说明解码器缓存了没有解码完成的半包消息
                      // 将需要解码的ByteBuf复制到cumulation
                      cumulation = cumulator.cumulate(ctx.alloc(), cumulation, data);
                  }
                  callDecode(ctx, cumulation, out);
              } catch (DecoderException e) {
                  throw e;
              } catch (Exception e) {
                  throw new DecoderException(e);
              } finally {
                  if (cumulation != null && !cumulation.isReadable()) {
                      numReads = 0;
                      cumulation.release();
                      cumulation = null;
                  } else if (++ numReads >= discardAfterReads) {
                      // We did enough reads already try to discard some bytes so we not risk to see a OOME.
                      // See https://github.com/netty/netty/issues/4275
                      numReads = 0;
                      discardSomeReadBytes();
                  }
  
                  int size = out.size();
                  decodeWasNull = !out.insertSinceRecycled();
                  fireChannelRead(ctx, out, size);
                  out.recycle();
              }
          } else {
              ctx.fireChannelRead(msg);
          }
      }
      
      protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
          try {
              while (in.isReadable()) {
                  int outSize = out.size();
  
                  if (outSize > 0) {
                      fireChannelRead(ctx, out, outSize);
                      out.clear();
  
                      // Check if this handler was removed before continuing with decoding.
                      // If it was removed, it is not safe to continue to operate on the buffer.
                      if (ctx.isRemoved()) {
                          break;
                      }
                      outSize = 0;
                  }
  
                  int oldInputLength = in.readableBytes();
                  decodeRemovalReentryProtection(ctx, in, out);
  
                  if (ctx.isRemoved()) {
                      break;
                  }
  
                  // 如果输出的out列表长度没有变化，说明解码没有成功
                  if (outSize == out.size()) {
                      if (oldInputLength == in.readableBytes()) {
                          // 用户解码器没有消费ByteBuf，说明是个半包消息，需要I/O线程继续读取后续的数据报
                          break;
                      } else {
                          continue;
                      }
                  }
  
                  if (oldInputLength == in.readableBytes()) {
                      throw new DecoderException(
                              StringUtil.simpleClassName(getClass()) +
                                      ".decode() did not read anything but decoded a message.");
                  }
  
                  if (isSingleDecode()) {
                      // 单条消息编码器，第一次吃解码完成之后就退出循环
                      break;
                  }
              }
          } catch (DecoderException e) {
              throw e;
          } catch (Exception cause) {
              throw new DecoderException(cause);
          }
      }
      ```
      
   2. MessageToMessageDecoder源码解析
   
      MessageToMessageDecoder负责将一个POJO转换成另一个POJO对象。
      首先看channelRead方法，
      ```
      public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
          CodecOutputList out = CodecOutputList.newInstance();
          try {
              if (acceptInboundMessage(msg)) {// 对解码的类型进行判断
                  @SuppressWarnings("unchecked")
                  I cast = (I) msg;
                  try {
                      // 抽象方法，由具体实现子类进行消息解码
                      decode(ctx, cast, out);
                  } finally {
                      ReferenceCountUtil.release(cast);
                  }
              } else {
                  out.add(msg);
              }
          } catch (DecoderException e) {
              throw e;
          } catch (Exception e) {
              throw new DecoderException(e);
          } finally {
              int size = out.size();
              for (int i = 0; i < size; i ++) {
                  ctx.fireChannelRead(out.getUnsafe(i));
              }
              out.recycle();
          }
      }
      ```
   3. LengthFieldBasedFrameDecoder源码分析
   
      基于消息长度的半包解码器，
      ```
      protected final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
          Object decoded = decode(ctx, in);
          if (decoded != null) {
              // 编码成功，加入到out输出列表
              out.add(decoded);
          }
      }
      
      protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
          if (discardingTooLongFrame) {
              // 判断discardingTooLongFrame标识，是否可丢弃当前可读的字节缓冲区
              discardingTooLongFrame(in);
          }
  
          if (in.readableBytes() < lengthFieldEndOffset) {
              return null;
          }
  
          int actualLengthFieldOffset = in.readerIndex() + lengthFieldOffset;// 实际的长度字段索引
          long frameLength = getUnadjustedFrameLength(in, actualLengthFieldOffset, lengthFieldLength, byteOrder); // 获取消息报文的长度字段
  
          // 对长度进行合法性判断，同时根据其它解码参数进行长度调整
          if (frameLength < 0) {
              failOnNegativeLengthField(in, frameLength, lengthFieldEndOffset);
          }
  
          frameLength += lengthAdjustment + lengthFieldEndOffset;
  
          if (frameLength < lengthFieldEndOffset) {
              failOnFrameLengthLessThanLengthFieldEndOffset(in, frameLength, lengthFieldEndOffset);
          }
  
          if (frameLength > maxFrameLength) {
              exceededFrameLength(in, frameLength);
              return null;
          }
  
          // never overflows because it's less than maxFrameLength
          int frameLengthInt = (int) frameLength;
          if (in.readableBytes() < frameLengthInt) { // 半包消息
              return null;
          }
  
          if (initialBytesToStrip > frameLengthInt) {
              failOnFrameLengthLessThanInitialBytesToStrip(in, frameLength, initialBytesToStrip);
          }
          in.skipBytes(initialBytesToStrip);
  
          // extract frame
          int readerIndex = in.readerIndex();
          int actualFrameLength = frameLengthInt - initialBytesToStrip;
          ByteBuf frame = extractFrame(ctx, in, readerIndex, actualFrameLength);
          in.readerIndex(readerIndex + actualFrameLength); // 更新读索引
          return frame;
      }
      
      protected long getUnadjustedFrameLength(ByteBuf buf, int offset, int length, ByteOrder order) {
          buf = buf.order(order);
          long frameLength;
          switch (length) { // 长度字段自身的字节长度
          case 1:
              frameLength = buf.getUnsignedByte(offset);
              break;
          case 2:
              frameLength = buf.getUnsignedShort(offset);
              break;
          case 3:
              frameLength = buf.getUnsignedMedium(offset);
              break;
          case 4:
              frameLength = buf.getUnsignedInt(offset);
              break;
          case 8:
              frameLength = buf.getLong(offset);
              break;
          default:
              throw new DecoderException(
                      "unsupported lengthFieldLength: " + lengthFieldLength + " (expected: 1, 2, 3, 4, or 8)");
          }
          return frameLength;
      }
      ```
      
   4. MessageToByteEncoder源码分析
   
      MessageToByteEncoder负责将用户的POJO对象编码成ByteBuf，以便进行网络传输。
      ```
      public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
          ByteBuf buf = null;
          try {
              // 判断当前编码器是否支持发送的消息
              if (acceptOutboundMessage(msg)) {
                  @SuppressWarnings("unchecked")
                  I cast = (I) msg;
                  // 根据缓冲区类型分配缓冲区（堆外内存和堆内存）
                  buf = allocateBuffer(ctx, cast, preferDirect);
                  try {
                      // 抽象方法，由子类实现
                      encode(ctx, cast, buf);
                  } finally {
                      ReferenceCountUtil.release(cast);
                  }
  
                  if (buf.isReadable()) {
                      // 缓冲区包含可发送的字节
                      ctx.write(buf, promise);
                  } else {
                      buf.release();
                      ctx.write(Unpooled.EMPTY_BUFFER, promise);
                  }
                  buf = null;
              } else {
                  ctx.write(msg, promise);
              }
          } catch (EncoderException e) {
              throw e;
          } catch (Throwable e) {
              throw new EncoderException(e);
          } finally {
              if (buf != null) {
                  buf.release();
              }
          }
      }
      ```
   
   5. MessageToMessageEncoder源码分析
   
      MessageToMessageEncoder负责将一个POJO对象编码成另一个POJO对象，例如将XML Document对象编码成XML格式的字符串。
      ```
      public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
          CodecOutputList out = null;
          try {
              if (acceptOutboundMessage(msg)) {
                  out = CodecOutputList.newInstance();
                  @SuppressWarnings("unchecked")
                  I cast = (I) msg;
                  try {
                      encode(ctx, cast, out);
                  } finally {
                      ReferenceCountUtil.release(cast);
                  }
  
                  if (out.isEmpty()) {
                      out.recycle();
                      out = null;
  
                      throw new EncoderException(
                              StringUtil.simpleClassName(this) + " must produce at least one message.");
                  }
              } else {
                  ctx.write(msg, promise);
              }
          } catch (EncoderException e) {
              throw e;
          } catch (Throwable t) {
              throw new EncoderException(t);
          } finally {
              if (out != null) {
                  final int sizeMinusOne = out.size() - 1;
                  if (sizeMinusOne == 0) {
                      ctx.write(out.get(0), promise);
                  } else if (sizeMinusOne > 0) {
                      // Check if we can use a voidPromise for our extra writes to reduce GC-Pressure
                      // See https://github.com/netty/netty/issues/2525
                      ChannelPromise voidPromise = ctx.voidPromise();
                      boolean isVoidPromise = promise == voidPromise;
                      for (int i = 0; i < sizeMinusOne; i ++) {
                          ChannelPromise p;
                          if (isVoidPromise) {
                              p = voidPromise;
                          } else {
                              p = ctx.newPromise();
                          }
                          ctx.write(out.getUnsafe(i), p);
                      }
                      ctx.write(out.getUnsafe(sizeMinusOne), promise);
                  }
                  out.recycle();
              }
          }
      }
      ```
      跟前一个类的步骤类似。
      
   6. LengthFieldPrepender源码分析
   
      LengthFieldPrepender负责在待发送的ByteBuf消息头中增加一个长度字段来标识消息的长度，它简化了用户的编码器开发，使用户不需要额外去设置这个字段。
      ```
      protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
          int length = msg.readableBytes() + lengthAdjustment;
          if (lengthIncludesLengthFieldLength) { // 判断是否需要包含长度字段自身
              length += lengthFieldLength;
          }
  
          if (length < 0) {
              throw new IllegalArgumentException(
                      "Adjusted frame length (" + length + ") is less than zero");
          }
  
          switch (lengthFieldLength) {
          case 1: // 长度字段所占字节为1
              if (length >= 256) {
                  throw new IllegalArgumentException(
                          "length does not fit into a byte: " + length);
              }
              out.add(ctx.alloc().buffer(1).order(byteOrder).writeByte((byte) length));
              break;
          case 2:
              if (length >= 65536) {
                  throw new IllegalArgumentException(
                          "length does not fit into a short integer: " + length);
              }
              out.add(ctx.alloc().buffer(2).order(byteOrder).writeShort((short) length));
              break;
          case 3:
              if (length >= 16777216) {
                  throw new IllegalArgumentException(
                          "length does not fit into a medium integer: " + length);
              }
              out.add(ctx.alloc().buffer(3).order(byteOrder).writeMedium(length));
              break;
          case 4:
              out.add(ctx.alloc().buffer(4).order(byteOrder).writeInt(length));
              break;
          case 8:
              out.add(ctx.alloc().buffer(8).order(byteOrder).writeLong(length));
              break;
          default:
              throw new Error("should not reach here");
          }
          out.add(msg.retain());
      }
      ```
      最后将原需要发送的ByteBuf复制到List<Object> out中，完成编码。 
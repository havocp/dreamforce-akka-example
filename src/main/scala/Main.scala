/* Based on the Snoop server example Copyright 2012 The Netty Project */

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.channel.Channels._
import org.jboss.netty.channel._
import org.jboss.netty.buffer._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.util._
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import scala.collection.JavaConverters._
import scala.util.Properties

object Main extends App {
  // Heroku gives us a port in the PORT environment variable
  val port = Integer.parseInt(Properties.envOrElse("PORT", "9000"))
  println("Starting demo web server on port " + port)
  val server = new DemoServer(port)
  server.run()
}


// Everything below here is more about Netty than Heroku or Scala


class DemoServer(val port: Int) {
  def run(): Unit = {
    val bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
      Executors.newCachedThreadPool(),
      Executors.newCachedThreadPool()));
    bootstrap.setPipelineFactory(new DemoPipelineFactory);
    bootstrap.bind(new InetSocketAddress(port));
  }
}

class DemoPipelineFactory extends ChannelPipelineFactory {
  override def getPipeline(): ChannelPipeline = {
    val pipe = pipeline()

    pipe.addLast("decoder", new HttpRequestDecoder)
    pipe.addLast("encoder", new HttpResponseEncoder)
    pipe.addLast("deflater", new HttpContentCompressor)
    pipe.addLast("handler", new DemoServerHandler)

    pipe
  }
}

class DemoServerHandler extends SimpleChannelUpstreamHandler {

  var requestOption: Option[HttpRequest] = None
  var readingChunks = false
  val buf = new StringBuilder()

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
    e.getCause.printStackTrace()
    e.getChannel.close()
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
    if (readingChunks) {
      val chunk = e.getMessage.asInstanceOf[HttpChunk]
      if (chunk.isLast) {
        readingChunks = false
        buf.append("END OF CONTENT\r\n");

        writeResponse(e);
      } else {
        buf.append("CHUNK: " + chunk.getContent().toString(CharsetUtil.UTF_8) + "\r\n");
      }
    } else {
      val request = e.getMessage.asInstanceOf[HttpRequest]
      requestOption = Some(request)

      if (HttpHeaders.is100ContinueExpected(request)) {
        send100Continue(e);
      }

      buf.setLength(0);
      buf.append("HELLO WORLD\r\n");
      buf.append("===================================\r\n");

      for (entry <- request.getHeaders.asScala) {
        buf.append("HEADER: " + entry.getKey + " = " + entry.getValue + "\r\n");
      }

      buf.append("\r\n");

      if (request.isChunked()) {
        readingChunks = true;
      } else {
        val content = request.getContent()
        if (content.readable()) {
          buf.append("CONTENT: " + content.toString(CharsetUtil.UTF_8) + "\r\n");
        }

        writeResponse(e);
      }
    }
  }

  private def writeResponse(e: MessageEvent): Unit = {
    val keepAlive = requestOption map HttpHeaders.isKeepAlive getOrElse false
    requestOption = None

    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
    response.setContent(ChannelBuffers.copiedBuffer(buf.toString, CharsetUtil.UTF_8))
    response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8")

    if (keepAlive) {
      response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, response.getContent.readableBytes)
      response.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }

    val future = e.getChannel.write(response)

    if (!keepAlive) {
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }

  private def send100Continue(e: MessageEvent) {
    e.getChannel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE))
  }
}

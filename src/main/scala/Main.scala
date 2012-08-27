/* Based on the Snoop server example Copyright 2012 The Netty Project */

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.channel.Channels._
import org.jboss.netty.channel._
import org.jboss.netty.buffer._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.util.{ Timeout => _, _ }
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import scala.collection.JavaConverters._
import scala.util.Properties

import akka.actor._
import akka.dispatch._
import akka.util._
import akka.util.duration._
import akka.pattern.ask

case object GetActorInfo

class ToyActor extends Actor with ActorLogging {
  var counter = 0

  override def receive: Receive = {
    case GetActorInfo =>
      log.info("Received GetActorInfo message with counter=" + counter)
      sender ! ("This is from actor " + self.path + " and is request " + counter)
      counter += 1
  }
}

object Main extends App {
  // Heroku gives us a port in the PORT environment variable
  val port = Integer.parseInt(Properties.envOrElse("PORT", "9000"))

  // create actor system and actor
  val system = ActorSystem("demo")
  val actor = system.actorOf(Props[ToyActor], name="Demo" + (new java.security.SecureRandom()).nextInt())

  println("Starting demo web server on port " + port + " with actor " + actor)
  val server = new DemoServer(port, actor)
  server.run()
}


// Everything below here is more about Netty than Heroku or Scala


class DemoServer(val port: Int, val actor: ActorRef) {
  def run(): Unit = {
    val bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
      Executors.newCachedThreadPool(),
      Executors.newCachedThreadPool()));
    bootstrap.setPipelineFactory(new DemoPipelineFactory(actor));
    bootstrap.bind(new InetSocketAddress(port));
  }
}

class DemoPipelineFactory(val actor: ActorRef) extends ChannelPipelineFactory {
  override def getPipeline(): ChannelPipeline = {
    val pipe = pipeline()

    pipe.addLast("decoder", new HttpRequestDecoder)
    pipe.addLast("encoder", new HttpResponseEncoder)
    pipe.addLast("deflater", new HttpContentCompressor)
    pipe.addLast("handler", new DemoServerHandler(actor))

    pipe
  }
}

class DemoServerHandler(val actor: ActorRef) extends SimpleChannelUpstreamHandler {

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

      implicit val timeout = Timeout(2 seconds)

      val blurb = Await.result(actor ? GetActorInfo, 1 second)

      buf.append(blurb)

      buf.append("\r\n");

      if (request.isChunked()) {
        readingChunks = true;
      } else {
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

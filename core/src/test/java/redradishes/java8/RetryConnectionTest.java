package redradishes.java8;

import org.junit.Test;
import redradishes.commands.Command;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.concurrent.CompletionException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static redradishes.commands.CommandBuilder.command;
import static redradishes.decoder.Replies.simpleStringReply;

public class RetryConnectionTest {
  private static final Command<CharSequence> PING = command("PING").returning(simpleStringReply());

  @Test(timeout = 1000)
  public void retriesToConnect() throws IOException, InterruptedException {
    SocketAddress address = getFreeAddress();
    RedisClientFactory factory = new RedisClientFactory(UTF_8, 1);
    RedisClient redisClient = factory.connect(address);
    try {
      redisClient.send(PING).join();
    } catch (CompletionException e) {
      assertThat(e.getCause(), instanceOf(ConnectException.class));
    }
  }

  private static SocketAddress getFreeAddress() throws IOException {
    SocketAddress address;
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      address = serverSocket.getLocalSocketAddress();
    }
    return address;
  }
}

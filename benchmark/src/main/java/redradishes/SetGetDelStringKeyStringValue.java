package redradishes;

import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import redradishes.commands.Command1;
import redradishes.commands.Command2;
import redradishes.java8.RedisClient;
import redradishes.java8.RedisClientFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;
import static redradishes.commands.CommandBuilder.command;
import static redradishes.decoder.BulkStringBuilders.string;
import static redradishes.decoder.Replies.bulkStringReply;
import static redradishes.decoder.Replies.integerReply;
import static redradishes.decoder.Replies.simpleStringReply;
import static redradishes.encoder.Encoders.arrayArg;
import static redradishes.encoder.Encoders.strArg;

@Threads(16)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
public class SetGetDelStringKeyStringValue {
  public static final Command2<CharSequence, CharSequence, CharSequence> SET =
      command("SET").withArg(strArg(UTF_8)).withArg(strArg(UTF_8)).returning(simpleStringReply());
  public static final Command1<CharSequence, String> GET =
      command("GET").withArg(strArg(UTF_8)).returning(bulkStringReply(string()));
  public static final Command1<CharSequence[], Integer> DEL =
      command("DEL").withArg(arrayArg(strArg(UTF_8))).returning(integerReply());

  @State(Scope.Thread)
  public static class ThreadName {
    private final String threadName = Thread.currentThread().getName();
  }

  @State(Scope.Benchmark)
  public static class RedRadishesClient {
    private final RedisClientFactory factory;
    private final RedisClient client;

    public RedRadishesClient() {
      try {
        factory = new RedisClientFactory(UTF_8, 16);
        client = factory.connect(new InetSocketAddress("localhost", 6379));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @TearDown
    public void tearDown() {
      client.close();
      factory.close();
    }
  }

  @Benchmark
  public void redRadishes(RedRadishesClient redRadishesClient, ThreadName threadName) throws Exception {
    String thread = threadName.threadName;
    RedisClient client = redRadishesClient.client;
    int num = 100;
    List<CompletableFuture<CharSequence>> setResults = new ArrayList<>(num);
    List<CompletableFuture<Boolean>> getResults = new ArrayList<>(num);
    List<CompletableFuture<Integer>> delResults = new ArrayList<>(num);
    for (int i = 0; i < num; i++) {
      String key = "key" + i + thread;
      String value = "value" + i;
      setResults.add(client.send(SET, key, value));
      getResults.add(client.send(GET, key).thenApply(getResult -> getResult != null && getResult.equals(value)));
      delResults.add(client.send(DEL, key));
    }
    for (CompletableFuture<CharSequence> setResult : setResults) {
      setResult.get();
    }
    for (CompletableFuture<Boolean> getResult : getResults) {
      if (!getResult.get()) throw new Exception("GET error");
    }
    for (CompletableFuture<Integer> delResult : delResults) {
      if (delResult.get() != 1) throw new Exception("DEL error");
    }
  }

  @Benchmark
  public void redRadishesPairs(RedRadishesClient redRadishesClient, ThreadName threadName) throws Exception {
    String thread = threadName.threadName;
    RedisClient client = redRadishesClient.client;
    int num = 100;
    List<CompletableFuture<Boolean>> getResults = new ArrayList<>(num);
    for (int i = 0; i < num; i++) {
      String key = "key" + i + thread;
      String value = "value" + i;
      CompletableFuture<Boolean> getResult = client.send(SET.apply(key, value)
          .combine(GET.apply(key), (setResult, getResult1) -> getResult1 != null && getResult1.equals(value))
          .combine(DEL.apply(new CharSequence[]{key}), (prevResult, delResult) -> prevResult && delResult == 1));
      getResults.add(getResult);
    }
    for (CompletableFuture<Boolean> getResult : getResults) {
      if (!getResult.get()) throw new Exception("error");
    }
  }

  @State(Scope.Benchmark)
  public static class LettuceClient {
    private final com.lambdaworks.redis.RedisClient client =
        com.lambdaworks.redis.RedisClient.create("redis://localhost:6379");
    private final RedisAsyncCommands<String, String> connection = client.connect().async();

    @TearDown
    public void tearDown() {
      connection.close();
      client.shutdown();
    }
  }

  @Benchmark
  public void lettuce(LettuceClient lettuceClient, ThreadName threadName) throws Exception {
    String thread = threadName.threadName;
    RedisAsyncCommands<String, String> connection = lettuceClient.connection;
    int num = 100;
    List<CompletableFuture<String>> setResults = new ArrayList<>(num);
    List<CompletableFuture<Boolean>> getResults = new ArrayList<>(num);
    List<CompletableFuture<Long>> delResults = new ArrayList<>(num);
    for (int i = 0; i < num; i++) {
      String key = "key" + i + thread;
      String value = "value" + i;
      setResults.add(connection.set(key, value).toCompletableFuture());
      getResults.add(connection.get(key).toCompletableFuture()
          .thenApply(getResult -> getResult != null && getResult.equals(value)));
      delResults.add(connection.del(key).toCompletableFuture());
    }
    for (CompletableFuture<String> setResult : setResults) {
      setResult.get();
    }
    for (CompletableFuture<Boolean> getResult : getResults) {
      if (!getResult.get()) throw new Exception("GET error");
    }
    for (CompletableFuture<Long> delResult : delResults) {
      if (delResult.get() != 1) throw new Exception("DEL error");
    }
  }
}

package redradishes.guava;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redradishes.CommandList;
import redradishes.CommandPair;
import redradishes.RedisException;
import redradishes.Request;
import redradishes.encoder.Command;
import redradishes.encoder.Command1;
import redradishes.encoder.Command2;
import redradishes.encoder.Command3;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static redradishes.decoder.ArrayBuilders.array;
import static redradishes.decoder.ArrayBuilders.collection;
import static redradishes.decoder.BulkStringBuilders._long;
import static redradishes.decoder.BulkStringBuilders.byteArray;
import static redradishes.decoder.BulkStringBuilders.charSequence;
import static redradishes.decoder.BulkStringBuilders.integer;
import static redradishes.decoder.BulkStringBuilders.string;
import static redradishes.decoder.MapBuilders.map;
import static redradishes.decoder.Replies.arrayReply;
import static redradishes.decoder.Replies.bulkStringReply;
import static redradishes.decoder.Replies.integerReply;
import static redradishes.decoder.Replies.longReply;
import static redradishes.decoder.Replies.mapReply;
import static redradishes.decoder.Replies.simpleStringReply;
import static redradishes.encoder.Commands.define;
import static redradishes.encoder.Encoders.arrayArg;
import static redradishes.encoder.Encoders.bytesArg;
import static redradishes.encoder.Encoders.collArg;
import static redradishes.encoder.Encoders.intArrayArg;
import static redradishes.encoder.Encoders.longArg;
import static redradishes.encoder.Encoders.longArrayArg;
import static redradishes.encoder.Encoders.mapArg;
import static redradishes.encoder.Encoders.strArg;
import static redradishes.hamcrest.HasSameContentAs.hasSameContentAs;

public class RedisClientTest {
  private RedisClientFactory factory;
  private RedisClient redisClient;
  public static final Command1<CharSequence[], Integer> DEL = define(integerReply(), "DEL", arrayArg(strArg(UTF_8)));
  public static final Command<CharSequence> PING = define(simpleStringReply(), "PING");
  public static final Command<CharSequence> FLUSHDB = define(simpleStringReply(), "FLUSHDB");
  public static final Command1<CharSequence, CharSequence> ECHO =
      define(bulkStringReply(charSequence()), "ECHO", strArg(UTF_8));
  public static final Command1<CharSequence, byte[]> GET = define(bulkStringReply(byteArray()), "GET", strArg(UTF_8));
  public static final Command1<CharSequence, List<CharSequence>> HKEYS =
      define(arrayReply(collection(ArrayList::new), charSequence()), "HKEYS", strArg(UTF_8));
  public static final Command1<CharSequence, CharSequence[]> HKEYS_A =
      define(arrayReply(array(CharSequence[]::new), charSequence()), "HKEYS", strArg(UTF_8));
  public static final Command1<CharSequence, Map<String, CharSequence>> HGETALL =
      define(mapReply(map(HashMap::new), string(), charSequence()), "HGETALL", strArg(UTF_8));
  public static final Command1<CharSequence, Integer> HLEN = define(integerReply(), "HLEN", strArg(UTF_8));
  public static final Command1<CharSequence, Set<Long>> SMEMBERS =
      define(arrayReply(collection(HashSet::new), _long()), "SMEMBERS", strArg(UTF_8));
  public static final Command1<CharSequence, List<Integer>> SMEMBERS_INTEGER_LIST =
      define(arrayReply(collection(ArrayList::new), integer()), "SMEMBERS", strArg(UTF_8));
  public static final Command2<CharSequence, CharSequence[], Integer> HDEL =
      define(integerReply(), "HDEL", strArg(UTF_8), arrayArg(strArg(UTF_8)));
  public static final Command2<CharSequence, CharSequence, CharSequence> HGET =
      define(bulkStringReply(charSequence()), "HGET", strArg(UTF_8), strArg(UTF_8));
  public static final Command2<CharSequence, CharSequence, byte[]> HGET_BYTES =
      define(bulkStringReply(byteArray()), "HGET", strArg(UTF_8), strArg(UTF_8));
  public static final Command2<CharSequence, CharSequence, Long> HGET_LONG =
      define(bulkStringReply(_long()), "HGET", strArg(UTF_8), strArg(UTF_8));
  public static final Command2<CharSequence, CharSequence[], List<CharSequence>> HMGET =
      define(arrayReply(collection(ArrayList::new), charSequence()), "HMGET", strArg(UTF_8), arrayArg(strArg(UTF_8)));
  public static final Command2<CharSequence, Collection<? extends CharSequence>, List<CharSequence>> HMGET2 =
      define(arrayReply(collection(ArrayList::new), charSequence()), "HMGET", strArg(UTF_8), collArg(strArg(UTF_8)));
  public static final Command2<CharSequence, Map<? extends String, ? extends CharSequence>, CharSequence> HMSET =
      define(simpleStringReply(), "HMSET", strArg(UTF_8), mapArg(strArg(UTF_8), strArg(UTF_8)));
  public static final Command2<CharSequence, Collection<? extends Long>, Integer> SADD =
      define(integerReply(), "SADD", strArg(UTF_8), collArg(longArg()));
  public static final Command2<CharSequence, long[], Integer> SADD_LONG_ARR =
      define(integerReply(), "SADD", strArg(UTF_8), longArrayArg());
  public static final Command2<CharSequence, int[], Integer> SADD_INT_ARR =
      define(integerReply(), "SADD", strArg(UTF_8), intArrayArg());
  public static final Command2<CharSequence, CharSequence, CharSequence> SET =
      define(simpleStringReply(), "SET", strArg(UTF_8), strArg(UTF_8));
  public static final Command2<CharSequence, byte[], CharSequence> SET_BYTES =
      define(simpleStringReply(), "SET", strArg(UTF_8), bytesArg());
  public static final Command2<CharSequence, Long, CharSequence> SET_LONG =
      define(simpleStringReply(), "SET", strArg(UTF_8), longArg());
  public static final Command2<CharSequence, byte[], Integer> SETNX =
      define(integerReply(), "SETNX", strArg(UTF_8), bytesArg());
  public static final Command3<CharSequence, CharSequence, CharSequence, Integer> HSET =
      define(integerReply(), "HSET", strArg(UTF_8), strArg(UTF_8), strArg(UTF_8));
  public static final Command3<CharSequence, CharSequence, byte[], Integer> HSET_BYTES =
      define(integerReply(), "HSET", strArg(UTF_8), strArg(UTF_8), bytesArg());
  public static final Command3<CharSequence, CharSequence, Long, Integer> HSET_LONG =
      define(integerReply(), "HSET", strArg(UTF_8), strArg(UTF_8), longArg());
  public static final Command3<CharSequence, CharSequence, Long, Long> HINCRBY =
      define(longReply(), "HINCRBY", strArg(UTF_8), strArg(UTF_8), longArg());

  @Before
  public void openConnection() throws Exception {
    factory = new RedisClientFactory(UTF_8, 1);
    redisClient = factory.connect(new InetSocketAddress("localhost", 6379));
    redisClient.send(FLUSHDB).get();
  }

  @After
  public void closeConnection() throws Exception {
    // Check that connection still works
    String message = "12345";
    assertThat(redisClient.send(ECHO, message).get(), hasSameContentAs(message));

    redisClient.close();
    factory.close();
  }

  @Test
  public void doNothing() {
  }

  @Test
  public void canSendSingleCommand() throws Exception {
    ListenableFuture<CharSequence> replyFuture = redisClient.send(PING);
    assertThat(replyFuture.get(), hasSameContentAs("PONG"));
  }

  @Test
  public void canSendManyCommands() throws Exception {
    ListenableFuture<CharSequence> replyFuture1 = redisClient.send(PING);
    ListenableFuture<CharSequence> replyFuture2 = redisClient.send(PING);
    ListenableFuture<CharSequence> replyFuture3 = redisClient.send(PING);
    assertThat(replyFuture1.get(), hasSameContentAs("PONG"));
    assertThat(replyFuture2.get(), hasSameContentAs("PONG"));
    assertThat(replyFuture3.get(), hasSameContentAs("PONG"));
  }

  @Test
  public void returnsError() throws Exception {
    String key = "KEY_1";
    assertThat(redisClient.send(HSET, key, "FIELD", "VAL").get(), equalTo(1));

    ListenableFuture<byte[]> future = redisClient.send(GET, key);
    try {
      future.get();
      fail();
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      assertThat(cause, instanceOf(RedisException.class));
      assertThat(cause.getMessage(), equalTo("WRONGTYPE Operation against a key holding the wrong kind of value"));
    }
  }

  @Test
  @SuppressWarnings({"unchecked", "varargs"})
  public void canSendListOfCommands() throws Exception {
    assertThat(redisClient.send(new CommandList<>(Arrays.<Request<CharSequence>>asList(PING, PING, PING))).get(),
        contains(hasSameContentAs("PONG"), hasSameContentAs("PONG"), hasSameContentAs("PONG")));
  }

  @Test
  public void canSendPairOfCommands() throws Exception {
    assertThat(redisClient.send(new CommandPair<>(PING, PING,
        (BiFunction<CharSequence, CharSequence, CharSequence>) (str1, str2) -> new StringBuilder(
            str1.length() + str2.length()).append(str1).append(str2))).get(), hasSameContentAs("PONGPONG"));
  }

  @Test
  public void echo() throws Exception {
    String message = "message";
    assertThat(redisClient.send(ECHO.apply(message)).get(), hasSameContentAs(message));
  }

  @Test
  public void hlenNoKey() throws Exception {
    assertThat(redisClient.send(HLEN, "NO_SUCH_KEY").get(), equalTo(0));
  }

  @Test
  public void hgetNoKey() throws Exception {
    assertThat(redisClient.send(HGET, "NO_SUCH_KEY", "FIELD").get(), nullValue());
  }

  @Test
  public void hgetByteArrayNoKey() throws Exception {
    assertThat(redisClient.send(HGET_BYTES, "NO_SUCH_KEY", "FIELD").get(), nullValue());
  }

  @Test
  public void hkeysNoKey() throws Exception {
    assertThat(redisClient.send(HKEYS, "NO_SUCH_KEY").get(), empty());
//    assertThat(redisClient.send(HKEYS_G, "NO_SUCH_KEY").get(), empty());
    assertThat(redisClient.send(HKEYS_A, "NO_SUCH_KEY").get(), emptyArray());
  }

  @Test
  public void hincrby() throws Exception {
    String key = "H_KEY_1";
    String field = "FIELD_1";
    long a = 123L;
    long b = 9876420L;
    assertThat(redisClient.send(HSET_LONG, key, field, a).get(), equalTo(1));
    assertThat(redisClient.send(HINCRBY, key, field, b).get(), equalTo(a + b));
    assertThat(redisClient.send(HGET_LONG, key, field).get(), equalTo(a + b));
  }

  @Test
  public void hGetSetBytes() throws Exception {
    String key = "H_KEY_1";
    String field = "FIELD_1";
    byte[] val = {'1', '2', '3'};
    assertThat(redisClient.send(HSET_BYTES, key, field, val).get(), equalTo(1));
    assertArrayEquals(redisClient.send(HGET_BYTES, key, field).get(), val);
  }

  @Test
  public void getSetBytes() throws Exception {
    String key = "KEY_1";
    byte[] val1 = {'1', '2', '3'};
    byte[] val2 = {'4', '5', '6'};
    assertThat(redisClient.send(GET, key).get(), nullValue());
    assertThat(redisClient.send(SET_BYTES, key, val1).get(), hasSameContentAs("OK"));
    assertThat(redisClient.send(SET_BYTES, key, val2).get(), hasSameContentAs("OK"));
    assertArrayEquals(redisClient.send(GET, key).get(), val2);
  }

//  @Test
//  public void getSetExBytes() throws Exception {
//    String key = "KEY_1";
//    byte[] val1 = {'1', '2', '3'};
//    byte[] val2 = {'4', '5', '6'};
//    assertThat(redisClient.send(GET, key).get(), nullValue());
//    assertThat(redisClient.send(SET_BYTES, key, val1, EX, 10).get(), hasSameContentAs("OK"));
//    assertThat(redisClient.send(SET_BYTES, key, val2).get(), hasSameContentAs("OK"));
//    assertArrayEquals(redisClient.send(GET, key).get(), val2);
//  }

  @Test
  public void del() throws Exception {
    String key1 = "KEY_1";
    String key2 = "KEY_2";
    byte[] val1 = {'1', '2', '3'};
    byte[] val2 = {'4', '5', '6'};
    assertThat(redisClient.send(SET_BYTES, key1, val1).get(), hasSameContentAs("OK"));
    assertThat(redisClient.send(SET_BYTES, key2, val2).get(), hasSameContentAs("OK"));
    assertThat(redisClient.send(DEL, key1, key2).get(), equalTo(2));
  }

  @Test
  public void getSetnxBytes() throws Exception {
    String key = "KEY_1";
    byte[] val1 = {'1', '2', '3'};
    byte[] val2 = {'4', '5', '6'};
    assertThat(redisClient.send(GET, key).get(), nullValue());
    assertThat(redisClient.send(SETNX, key, val1).get(), equalTo(1));
    assertThat(redisClient.send(SETNX, key, val2).get(), equalTo(0));
    assertArrayEquals(redisClient.send(GET, key).get(), val1);
  }

  @SuppressWarnings({"unchecked", "varargs"})
  @Test
  public void hash() throws Exception {
    String key = "H_KEY_1";
    String field1 = "FIELD_1";
    String val1 = "VAL_1";
    String field2 = "FIELD_2";
    String val2 = "VAL_2";
    assertThat(redisClient.send(HSET, key, field1, val1).get(), equalTo(1));
    assertThat(redisClient.send(HSET, key, field2, val2).get(), equalTo(1));
    assertThat(redisClient.send(HLEN, key).get(), equalTo(2));
    assertThat(redisClient.send(HGET, key, field1).get(), hasSameContentAs(val1));
    assertThat(redisClient.send(HGET, key, field2).get(), hasSameContentAs(val2));
    assertThat(redisClient.send(HGET, key, "NO_SUCH_FIELD").get(), nullValue());
  }

  @SuppressWarnings({"unchecked", "varargs"})
  @Test
  public void hashBulk() throws Exception {
    String key = "H_KEY_1";
    String field1 = "FIELD_1";
    String val1 = "VAL_1";
    String field2 = "FIELD_2";
    String val2 = "VAL_2";
    assertThat(redisClient.send(HGETALL, key).get().entrySet(), empty());
    assertThat(redisClient.send(HMSET, key, ImmutableMap.of(field1, val1, field2, val2)).get(), hasSameContentAs("OK"));
    assertThat(redisClient.send(HMGET, key, field1, field2, "NO_SUCH_FIELD").get(),
        contains(hasSameContentAs(val1), hasSameContentAs(val2), nullValue()));
    assertThat(redisClient.send(HMGET2, key, Arrays.asList(field1, field2, "NO_SUCH_FIELD")).get(),
        contains(hasSameContentAs(val1), hasSameContentAs(val2), nullValue()));
    assertThat(redisClient.send(HGETALL, key).get(),
        allOf(hasEntry(equalTo(field1), hasSameContentAs(val1)), hasEntry(equalTo(field2), hasSameContentAs(val2))));
    assertThat(redisClient.send(HDEL, key, field1, field2, "NO_SUCH_FIELD").get(), equalTo(2));
    assertThat(redisClient.send(HGETALL, key).get().entrySet(), empty());
  }

//  @SuppressWarnings({"unchecked", "varargs"})
//  @Test
//  public void hashBulkGuava() throws Exception {
//    String key = "H_KEY_1";
//    String field1 = "FIELD_1";
//    String val1 = "VAL_1";
//    String field2 = "FIELD_2";
//    String val2 = "VAL_2";
//    assertThat(redisClient.send(HGETALL_G, key).get().entrySet(), empty());
//    assertThat(redisClient.send(HMSET, key, ImmutableMap.of(field1, val1, field2, val2)).get(), hasSameContentAs
// ("OK"));
//    assertThat(redisClient.send(HMGET_G, key, Arrays.asList(field1, field2)).get(),
//        contains(hasSameContentAs(val1), hasSameContentAs(val2)));
//    assertThat(redisClient.send(HGETALL_G, key).get(),
//        allOf(hasEntry(equalTo(field1), hasSameContentAs(val1)), hasEntry(equalTo(field2), hasSameContentAs(val2))));
//    assertThat(redisClient.send(HDEL, key, field1, field2, "NO_SUCH_FIELD").get(), equalTo(2));
//    assertThat(redisClient.send(HGETALL_G, key).get().entrySet(), empty());
//  }

  @Test
  public void set() throws Exception {
    String key = "S_KEY_1";
    long val1 = 10;
    long val2 = 20;
    assertThat(redisClient.send(SADD, key, Arrays.asList(val1, val2)).get(), equalTo(2));
    assertThat(redisClient.send(SMEMBERS, key).get(), containsInAnyOrder(val1, val2));
  }

  @Test
  public void setLongArr() throws Exception {
    String key = "S_KEY_2";
    long val1 = 10;
    long val2 = 20;
    assertThat(redisClient.send(SADD_LONG_ARR, key, new long[]{val1, val2}).get(), equalTo(2));
    assertThat(redisClient.send(SMEMBERS, key).get(), containsInAnyOrder(val1, val2));
  }

  @Test
  public void setIntArr() throws Exception {
    String key = "S_KEY_3";
    int val1 = 10;
    int val2 = 20;
    assertThat(redisClient.send(SADD_INT_ARR, key, new int[]{val1, val2}).get(), equalTo(2));
    assertThat(redisClient.send(SMEMBERS_INTEGER_LIST, key).get(), containsInAnyOrder(val1, val2));
  }
}

## Introduction

This example requires the following imports:

```java
import redradishes.commands.Command1;
import redradishes.java8.RedisClient;
import redradishes.java8.RedisClientFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;
import static redradishes.commands.CommandBuilder.command;
import static redradishes.decoder.BulkStringBuilders.string;
import static redradishes.decoder.Replies.bulkStringReply;
import static redradishes.encoder.Encoders.strArg;
```

Command object defines command arguments and return type.
See [Redis Command Reference](http://redis.io/commands) to look up command arguments and return type.
Command objects are immutable and thread safe. It is a good practice to create them only once in a program.

```java
private static final Command1<CharSequence, String> GET =
        command("GET").withArg(strArg(UTF_8)).returning(bulkStringReply(string()));
```

`RedisClientFactory` contains a pool of IO threads. It is thread safe and can be shared.

```java
RedisClientFactory factory = new RedisClientFactory(UTF_8, 16);
```

Connection to Redis is established asynchronously. The following call will not throw exceptions.

```java
RedisClient client = factory.connect(new InetSocketAddress("localhost", 6379));
```

Then we can send a command and get an asynchronous result as a future:

```java
CompletableFuture<String> future = client.send(GET, "key");
```

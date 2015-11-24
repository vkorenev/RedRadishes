package redradishes.decoder;

import com.pholser.junit.quickcheck.ForAll;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.java.lang.Encoded;
import org.junit.Rule;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.contrib.theories.suppliers.TestedOn;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import redradishes.decoder.parser.ReplyParser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static redradishes.decoder.parser.TestUtil.parseReply;
import static redradishes.decoder.parser.TestUtil.split;

@RunWith(Theories.class)
public class RepliesTest {
  @Rule
  public final MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  private CharsetDecoder charsetDecoder;

  @Theory
  public void parsesIntegerReply(@ForAll int num, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    Iterator<ByteBuffer> chunks = split((":" + num + "\r\n").getBytes(US_ASCII), bufferSize);
    assertThat(parseReply(chunks, Replies.integerReply(), Function.identity(), message -> {
      throw new RuntimeException(message.toString());
    }, charsetDecoder), equalTo(num));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesErrorIntegerReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    parsesError(s, bufferSize, Replies.integerReply());
  }

  @Theory
  public void parsesLongReply(@ForAll long num, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    Iterator<ByteBuffer> chunks = split((":" + num + "\r\n").getBytes(US_ASCII), bufferSize);
    assertThat(parseReply(chunks, Replies.longReply(), Function.identity(), message -> {
      throw new RuntimeException(message.toString());
    }, charsetDecoder), equalTo(num));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesErrorLongReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    parsesError(s, bufferSize, Replies.longReply());
  }

  @Theory
  public void parsesSimpleStringReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    String value = s.replace('\r', ' ').replace('\n', ' ');
    Iterator<ByteBuffer> chunks = split(("+" + value + "\r\n").getBytes(US_ASCII), bufferSize);
    assertThat(parseReply(chunks, Replies.simpleStringReply(), Function.identity(), message -> {
      throw new RuntimeException(message.toString());
    }, charsetDecoder).toString(), equalTo(value));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesErrorSimpleStringReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    parsesError(s, bufferSize, Replies.simpleStringReply());
  }

  @Theory
  public void parsesBulkStringReply(@ForAll byte[] bytes, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    byte[] header = ("$" + Integer.toString(bytes.length) + "\r\n").getBytes(US_ASCII);
    byte[] target = Arrays.copyOf(header, header.length + bytes.length + 2);
    System.arraycopy(bytes, 0, target, header.length, bytes.length);
    target[target.length - 2] = '\r';
    target[target.length - 1] = '\n';

    Iterator<ByteBuffer> chunks = split(target, bufferSize);
    assertThat(parseReply(chunks, Replies.bulkStringReply(new TestBulkStringBuilderFactory()), Function.identity(),
        message -> {
          throw new RuntimeException(message.toString());
        }, charsetDecoder), equalTo(bytes));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesNullBulkStringReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    byte[] bytes = "$-1\r\n".getBytes(US_ASCII);
    Iterator<ByteBuffer> chunks = split(bytes, bufferSize);
    BulkStringBuilderFactory<?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    assertThat(parseReply(chunks, Replies.bulkStringReply(bulkStringBuilderFactory), Function.identity(), message -> {
      throw new RuntimeException(message.toString());
    }, charsetDecoder), nullValue());
    verifyZeroInteractions(charsetDecoder);
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public void parsesErrorBulkStringReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    BulkStringBuilderFactory<?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    parsesError(s, bufferSize, Replies.bulkStringReply(bulkStringBuilderFactory));
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  private void parsesError(String error, int bufferSize, ReplyParser<?> parser) {
    String value = error.replace('\r', ' ').replace('\n', ' ');
    Iterator<ByteBuffer> chunks = split(("-" + value + "\r\n").getBytes(US_ASCII), bufferSize);
    assertThat(parseReply(chunks, parser, result -> {
      throw new RuntimeException("Unexpected result: " + result);
    }, message -> message, charsetDecoder).toString(), equalTo(value));
    verifyZeroInteractions(charsetDecoder);
  }
}

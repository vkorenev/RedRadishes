package redradishes.decoder;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.pholser.junit.quickcheck.ForAll;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.generator.java.lang.Encoded;
import org.junit.Rule;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.contrib.theories.suppliers.TestedOn;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import redradishes.RedisException;
import redradishes.ScanResult;
import redradishes.decoder.parser.ReplyParser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static redradishes.decoder.ArrayBuilders.array;
import static redradishes.decoder.Replies.arrayReply;
import static redradishes.decoder.Replies.bulkStringReply;
import static redradishes.decoder.Replies.integerReply;
import static redradishes.decoder.Replies.longReply;
import static redradishes.decoder.Replies.scanReply;
import static redradishes.decoder.Replies.simpleStringReply;
import static redradishes.decoder.parser.TestUtil.getByteString;
import static redradishes.decoder.parser.TestUtil.parseReply;
import static redradishes.decoder.parser.TestUtil.split;
import static redradishes.decoder.parser.TestUtil.throwingFailureHandler;
import static redradishes.decoder.parser.TestUtil.writeByteString;
import static redradishes.decoder.parser.TestUtil.writeLenPrefix;
import static redradishes.hamcrest.HasSameContentAs.hasSameContentAs;
import static redradishes.hamcrest.ThrowableMessageMatcher.hasMessage;

@RunWith(Theories.class)
public class RepliesTest {
  @Rule
  public final MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  private CharsetDecoder charsetDecoder;

  @Theory
  public void parsesIntegerReply(@ForAll int num, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    Iterator<ByteBuffer> chunks = split((":" + num + "\r\n").getBytes(US_ASCII), bufferSize);
    assertThat(parseReply(chunks, integerReply(), Function.identity(), throwingFailureHandler(), charsetDecoder),
        equalTo(num));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesNullIntegerReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    byte[] bytes = "$-1\r\n".getBytes(US_ASCII);
    Iterator<ByteBuffer> chunks = split(bytes, bufferSize);
    assertThat(parseReply(chunks, integerReply(), Function.identity(), throwingFailureHandler(), charsetDecoder),
        nullValue());
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesErrorIntegerReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    parsesError(s, bufferSize, integerReply());
  }

  @Theory
  public void parsesLongReply(@ForAll long num, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    Iterator<ByteBuffer> chunks = split((":" + num + "\r\n").getBytes(US_ASCII), bufferSize);
    assertThat(parseReply(chunks, longReply(), Function.identity(), throwingFailureHandler(), charsetDecoder),
        equalTo(num));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesNullLongReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    byte[] bytes = "$-1\r\n".getBytes(US_ASCII);
    Iterator<ByteBuffer> chunks = split(bytes, bufferSize);
    assertThat(parseReply(chunks, longReply(), Function.identity(), throwingFailureHandler(), charsetDecoder),
        nullValue());
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesErrorLongReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    parsesError(s, bufferSize, longReply());
  }

  @Theory
  public void parsesSimpleStringReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    String value = s.replace('\r', ' ').replace('\n', ' ');
    Iterator<ByteBuffer> chunks = split(("+" + value + "\r\n").getBytes(US_ASCII), bufferSize);
    assertThat(parseReply(chunks, simpleStringReply(), Function.identity(), throwingFailureHandler(), charsetDecoder),
        hasSameContentAs(value));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesNullStringReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    byte[] bytes = "$-1\r\n".getBytes(US_ASCII);
    Iterator<ByteBuffer> chunks = split(bytes, bufferSize);
    assertThat(parseReply(chunks, simpleStringReply(), Function.identity(), throwingFailureHandler(), charsetDecoder),
        nullValue());
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesErrorSimpleStringReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    parsesError(s, bufferSize, simpleStringReply());
  }

  @Theory
  public void parsesBulkStringReply(@ForAll byte[] bytes, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    Iterator<ByteBuffer> chunks = split(getByteString(bytes), bufferSize);
    assertThat(parseReply(chunks, bulkStringReply(new TestBulkStringBuilderFactory()), Function.identity(),
        throwingFailureHandler(), charsetDecoder), equalTo(bytes));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesNullBulkStringReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    byte[] bytes = "$-1\r\n".getBytes(US_ASCII);
    Iterator<ByteBuffer> chunks = split(bytes, bufferSize);
    BulkStringBuilderFactory<?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    assertThat(
        parseReply(chunks, bulkStringReply(bulkStringBuilderFactory), Function.identity(), throwingFailureHandler(),
            charsetDecoder), nullValue());
    verifyZeroInteractions(charsetDecoder);
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public void parsesErrorBulkStringReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    BulkStringBuilderFactory<?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    parsesError(s, bufferSize, bulkStringReply(bulkStringBuilderFactory));
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public void parsesArrayReply(@ForAll byte[][] arrays, @TestedOn(ints = {10, 100, 1000}) int bufferSize) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    writeLenPrefix('*', arrays.length, out);
    for (byte[] bytes : arrays) {
      writeByteString(bytes, out);
    }
    Iterator<ByteBuffer> chunks = split(out.toByteArray(), bufferSize);
    assertThat(
        parseReply(chunks, arrayReply(array(byte[][]::new), new TestBulkStringBuilderFactory()), Function.identity(),
            throwingFailureHandler(), charsetDecoder), equalTo(arrays));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public <E> void parsesNullArrayReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    byte[] bytes = "*-1\r\n".getBytes(US_ASCII);
    Iterator<ByteBuffer> chunks = split(bytes, bufferSize);
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, ?> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    assertThat(parseReply(chunks, arrayReply(arrayBuilderFactory, bulkStringBuilderFactory), Function.identity(),
        throwingFailureHandler(), charsetDecoder), nullValue());
    verifyZeroInteractions(charsetDecoder);
    verifyZeroInteractions(arrayBuilderFactory);
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public <E> void parsesErrorArrayReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, ?> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    parsesError(s, bufferSize, arrayReply(arrayBuilderFactory, bulkStringBuilderFactory));
    verifyZeroInteractions(arrayBuilderFactory);
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public void parsesScanReply(@ForAll(sampleSize = 10) @InRange(minLong = 0) long cursor, @ForAll byte[][] elements,
      @TestedOn(ints = {100, 1000}) int bufferSize) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    writeLenPrefix('*', 2, out);
    writeByteString(Long.toString(cursor).getBytes(US_ASCII), out);
    writeLenPrefix('*', elements.length, out);
    for (byte[] element : elements) {
      writeByteString(element, out);
    }
    Iterator<ByteBuffer> chunks = split(out.toByteArray(), bufferSize);
    ScanResult<byte[][]> scanResult =
        parseReply(chunks, scanReply(array(byte[][]::new), new TestBulkStringBuilderFactory()), Function.identity(),
            throwingFailureHandler(), charsetDecoder);
    assertThat(scanResult.cursor, equalTo(cursor));
    assertThat(scanResult.elements, equalTo(elements));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public <E> void parsesErrorScanReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, ?> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    parsesError(s, bufferSize, scanReply(arrayBuilderFactory, bulkStringBuilderFactory));
    verifyZeroInteractions(arrayBuilderFactory);
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  private void parsesError(String error, int bufferSize, ReplyParser<?> parser) {
    String value = error.replace('\r', ' ').replace('\n', ' ');
    Iterator<ByteBuffer> chunks = split(("-" + value + "\r\n").getBytes(US_ASCII), bufferSize);
    assertThat(parseReply(chunks, parser, result -> {
      throw new RuntimeException("Unexpected result: " + result);
    }, e -> e, charsetDecoder), allOf(instanceOf(RedisException.class), hasMessage(equalTo(value))));
    verifyZeroInteractions(charsetDecoder);
  }
}

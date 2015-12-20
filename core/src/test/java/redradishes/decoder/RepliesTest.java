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
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static redradishes.decoder.ArrayBuilders.array;
import static redradishes.decoder.Replies.arrayReply;
import static redradishes.decoder.Replies.bulkStringReply;
import static redradishes.decoder.Replies.integerReply;
import static redradishes.decoder.Replies.longReply;
import static redradishes.decoder.Replies.scanReply;
import static redradishes.decoder.Replies.simpleStringReply;
import static redradishes.decoder.parser.TestUtil.assertNoFailure;
import static redradishes.decoder.parser.TestUtil.assertNoResult;
import static redradishes.decoder.parser.TestUtil.getByteString;
import static redradishes.decoder.parser.TestUtil.parseReply;
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
    ByteBuffer src = ByteBuffer.wrap(encodeIntegerReply(num));
    assertThat(parseReply(src, bufferSize, integerReply(), Function.identity(), assertNoFailure(), charsetDecoder),
        equalTo(num));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesNullIntegerReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeNilBulkStringReply());
    assertThat(parseReply(src, bufferSize, integerReply(), Function.identity(), assertNoFailure(), charsetDecoder),
        nullValue());
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesErrorIntegerReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    parsesError(s, bufferSize, integerReply());
  }

  @Theory
  public void failsToParseIntegerReplyIfSimpleStringReplyIsFound(
      @ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeAsSimpleString(s));
    failsToParseReply(src, bufferSize, integerReply(), "Unexpected simple string reply");
  }

  @Theory
  public void parsesLongReply(@ForAll long num, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeIntegerReply(num));
    assertThat(parseReply(src, bufferSize, longReply(), Function.identity(), assertNoFailure(), charsetDecoder),
        equalTo(num));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesNullLongReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeNilBulkStringReply());
    assertThat(parseReply(src, bufferSize, longReply(), Function.identity(), assertNoFailure(), charsetDecoder),
        nullValue());
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesErrorLongReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    parsesError(s, bufferSize, longReply());
  }

  @Theory
  public void failsToParseLongReplyIfSimpleStringReplyIsFound(
      @ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeAsSimpleString(s));
    failsToParseReply(src, bufferSize, longReply(), "Unexpected simple string reply");
  }

  @Theory
  public void parsesSimpleStringReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeAsSimpleString(s));
    assertThat(parseReply(src, bufferSize, simpleStringReply(), Function.identity(), assertNoFailure(), charsetDecoder),
        hasSameContentAs(s));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesNullStringReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeNilBulkStringReply());
    assertThat(parseReply(src, bufferSize, simpleStringReply(), Function.identity(), assertNoFailure(), charsetDecoder),
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
    ByteBuffer src = ByteBuffer.wrap(getByteString(bytes));
    assertThat(parseReply(src, bufferSize, bulkStringReply(new TestBulkStringBuilderFactory()), Function.identity(),
        assertNoFailure(), charsetDecoder), equalTo(bytes));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesNullBulkStringReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeNilBulkStringReply());
    BulkStringBuilderFactory<?, ?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    assertThat(
        parseReply(src, bufferSize, bulkStringReply(bulkStringBuilderFactory), Function.identity(), assertNoFailure(),
            charsetDecoder), nullValue());
    verifyZeroInteractions(charsetDecoder);
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public void parsesErrorBulkStringReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    BulkStringBuilderFactory<?, ?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    parsesError(s, bufferSize, bulkStringReply(bulkStringBuilderFactory));
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public void failsToParseBulkStringReplyIfIntegerReplyIsFound(@ForAll long num,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeIntegerReply(num));
    BulkStringBuilderFactory<?, ?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, bulkStringReply(bulkStringBuilderFactory), "Unexpected integer reply");
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public void failsToParseBulkStringReplyIfSimpleStringReplyIsFound(
      @ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeAsSimpleString(s));
    BulkStringBuilderFactory<?, ?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, bulkStringReply(bulkStringBuilderFactory), "Unexpected simple string reply");
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public void parsesArrayReply(@ForAll byte[][] arrays, @TestedOn(ints = {10, 100, 1000}) int bufferSize) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    writeLenPrefix('*', arrays.length, out);
    for (byte[] bytes : arrays) {
      writeByteString(bytes, out);
    }
    ByteBuffer src = ByteBuffer.wrap(out.toByteArray());
    assertThat(parseReply(src, bufferSize, arrayReply(array(byte[][]::new), new TestBulkStringBuilderFactory()),
        Function.identity(), assertNoFailure(), charsetDecoder), equalTo(arrays));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public <E> void parsesNullArrayReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    byte[] bytes = "*-1\r\n".getBytes(US_ASCII);
    ByteBuffer src = ByteBuffer.wrap(bytes);
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, ?> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<?, E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    assertThat(
        parseReply(src, bufferSize, arrayReply(arrayBuilderFactory, bulkStringBuilderFactory), Function.identity(),
            assertNoFailure(), charsetDecoder), nullValue());
    verifyZeroInteractions(charsetDecoder);
    verifyZeroInteractions(arrayBuilderFactory);
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public <E> void parsesErrorArrayReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, ?> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<?, E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    parsesError(s, bufferSize, arrayReply(arrayBuilderFactory, bulkStringBuilderFactory));
    verifyZeroInteractions(arrayBuilderFactory);
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public <E> void failsToParseArrayReplyIfIntegerReplyIsFound(@ForAll long num,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeIntegerReply(num));
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, ?> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<?, E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, arrayReply(arrayBuilderFactory, bulkStringBuilderFactory),
        "Unexpected integer reply");
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public <E> void failsToParseArrayReplyIfSimpleStringReplyIsFound(
      @ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeAsSimpleString(s));
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, ?> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<?, E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, arrayReply(arrayBuilderFactory, bulkStringBuilderFactory),
        "Unexpected simple string reply");
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
    ByteBuffer src = ByteBuffer.wrap(out.toByteArray());
    ScanResult<byte[][]> scanResult =
        parseReply(src, bufferSize, scanReply(array(byte[][]::new), new TestBulkStringBuilderFactory()),
            Function.identity(), assertNoFailure(), charsetDecoder);
    assertThat(scanResult.cursor, equalTo(cursor));
    assertThat(scanResult.elements, equalTo(elements));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public <E> void parsesErrorScanReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, ?> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<?, E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    parsesError(s, bufferSize, scanReply(arrayBuilderFactory, bulkStringBuilderFactory));
    verifyZeroInteractions(arrayBuilderFactory);
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  private void parsesError(String error, int bufferSize, ReplyParser<?> parser) {
    ByteBuffer src = ByteBuffer.wrap(encodeAsError(error));
    assertThat(parseReply(src, bufferSize, parser, assertNoResult(), e -> e, charsetDecoder),
        allOf(instanceOf(RedisException.class), hasMessage(equalTo(error))));
    verifyZeroInteractions(charsetDecoder);
  }

  private <T> void failsToParseReply(ByteBuffer src, int bufferSize, ReplyParser<T> parser, String message) {
    assertThat(parseReply(src, bufferSize, parser, assertNoResult(), e -> e, charsetDecoder),
        allOf(instanceOf(ReplyParseException.class), hasMessage(equalTo(message))));
    verifyZeroInteractions(charsetDecoder);
  }

  private static byte[] encodeIntegerReply(long num) {
    return (":" + num + "\r\n").getBytes(US_ASCII);
  }

  private static byte[] encodeAsSimpleString(String value) {
    assumeTrue(value.indexOf('\r') == -1 && value.indexOf('\n') == -1);
    return ("+" + value + "\r\n").getBytes(US_ASCII);
  }

  private static byte[] encodeAsError(String value) {
    assumeTrue(value.indexOf('\r') == -1 && value.indexOf('\n') == -1);
    return ("-" + value + "\r\n").getBytes(US_ASCII);
  }

  private static byte[] encodeNilBulkStringReply() {
    return "$-1\r\n".getBytes(US_ASCII);
  }
}

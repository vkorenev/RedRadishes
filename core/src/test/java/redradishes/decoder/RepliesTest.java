package redradishes.decoder;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.generator.java.lang.Encoded;
import org.junit.Rule;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.pholser.junit.quickcheck.generator.Only;
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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static redradishes.decoder.ArrayBuilders.array;
import static redradishes.decoder.Replies.arrayReply;
import static redradishes.decoder.Replies.bulkStringReply;
import static redradishes.decoder.Replies.integerReply;
import static redradishes.decoder.Replies.longReply;
import static redradishes.decoder.Replies.scanReply;
import static redradishes.decoder.Replies.simpleStringReply;
import static redradishes.decoder.parser.TestUtil.assertNoFailure;
import static redradishes.decoder.parser.TestUtil.assertNoResult;
import static redradishes.decoder.parser.TestUtil.encodeArray;
import static redradishes.decoder.parser.TestUtil.encodeArrayOfArrays;
import static redradishes.decoder.parser.TestUtil.encodeBulkString;
import static redradishes.decoder.parser.TestUtil.encodeError;
import static redradishes.decoder.parser.TestUtil.encodeInteger;
import static redradishes.decoder.parser.TestUtil.encodeNilArray;
import static redradishes.decoder.parser.TestUtil.encodeNilBulkString;
import static redradishes.decoder.parser.TestUtil.encodeScanReply;
import static redradishes.decoder.parser.TestUtil.encodeSimpleString;
import static redradishes.decoder.parser.TestUtil.parseReply;
import static redradishes.hamcrest.HasSameContentAs.hasSameContentAs;
import static redradishes.hamcrest.ThrowableMessageMatcher.hasMessage;

@RunWith(JUnitQuickcheck.class)
public class RepliesTest {
  @Rule
  public final MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  private CharsetDecoder charsetDecoder;

  @Property
  public void parsesIntegerReply(int num, @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeInteger(num));
    assertThat(parseReply(src, bufferSize, integerReply(), Function.identity(), assertNoFailure(), charsetDecoder),
        equalTo(num));
    verifyZeroInteractions(charsetDecoder);
  }

  @Property
  public void parsesNullIntegerReply(@Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeNilBulkString());
    assertThat(parseReply(src, bufferSize, integerReply(), Function.identity(), assertNoFailure(), charsetDecoder),
        nullValue());
    verifyZeroInteractions(charsetDecoder);
  }

  @Property
  public void parsesErrorIntegerReply(@From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    parsesError(s, bufferSize, integerReply());
  }

  @Property
  public void failsToParseIntegerReplyIfSimpleStringReplyIsFound(
      @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeSimpleString(s));
    failsToParseReply(src, bufferSize, integerReply(),
        "Command returned simple string reply while integer reply was expected");
  }

  @Property
  public void failsToParseIntegerReplyIfBulkStringReplyIsFound(byte[] bytes,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeBulkString(bytes));
    failsToParseReply(src, bufferSize, integerReply(),
        "Command returned bulk string reply while integer reply was expected");
  }

  @Property(trials = 10)
  public void failsToParseIntegerReplyIfArrayReplyIsFound(byte[][][] arrays,
      @Only({"3", "5", "10", "100", "1000"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeArrayOfArrays(arrays));
    failsToParseReply(src, bufferSize, integerReply(), "Command returned array reply while integer reply was expected");
  }

  @Property
  public void parsesLongReply(long num, @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeInteger(num));
    assertThat(parseReply(src, bufferSize, longReply(), Function.identity(), assertNoFailure(), charsetDecoder),
        equalTo(num));
    verifyZeroInteractions(charsetDecoder);
  }

  @Property
  public void parsesNullLongReply(@Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeNilBulkString());
    assertThat(parseReply(src, bufferSize, longReply(), Function.identity(), assertNoFailure(), charsetDecoder),
        nullValue());
    verifyZeroInteractions(charsetDecoder);
  }

  @Property
  public void parsesErrorLongReply(@From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    parsesError(s, bufferSize, longReply());
  }

  @Property
  public void failsToParseLongReplyIfSimpleStringReplyIsFound(
      @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeSimpleString(s));
    failsToParseReply(src, bufferSize, longReply(),
        "Command returned simple string reply while integer reply was expected");
  }

  @Property
  public void failsToParseLongReplyIfBulkStringReplyIsFound(byte[] bytes,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeBulkString(bytes));
    failsToParseReply(src, bufferSize, longReply(),
        "Command returned bulk string reply while integer reply was expected");
  }

  @Property(trials = 10)
  public void failsToParseLongReplyIfArrayReplyIsFound(byte[][][] arrays,
      @Only({"3", "5", "10", "100", "1000"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeArrayOfArrays(arrays));
    failsToParseReply(src, bufferSize, longReply(), "Command returned array reply while integer reply was expected");
  }

  @Property
  public void parsesSimpleStringReply(@From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeSimpleString(s));
    assertThat(parseReply(src, bufferSize, simpleStringReply(), Function.identity(), assertNoFailure(), charsetDecoder),
        hasSameContentAs(s));
    verifyZeroInteractions(charsetDecoder);
  }

  @Property
  public void parsesNullStringReply(@Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeNilBulkString());
    assertThat(parseReply(src, bufferSize, simpleStringReply(), Function.identity(), assertNoFailure(), charsetDecoder),
        nullValue());
    verifyZeroInteractions(charsetDecoder);
  }

  @Property
  public void parsesErrorSimpleStringReply(@From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    parsesError(s, bufferSize, simpleStringReply());
  }

  @Property
  public void failsToParseSimpleStringReplyIfIntegerReplyIsFound(long num,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeInteger(num));
    failsToParseReply(src, bufferSize, simpleStringReply(),
        "Command returned integer reply while simple string reply was expected");
  }

  @Property
  public void failsToParseSimpleStringReplyIfBulkStringReplyIsFound(byte[] bytes,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeBulkString(bytes));
    failsToParseReply(src, bufferSize, simpleStringReply(),
        "Command returned bulk string reply while simple string reply was expected");
  }

  @Property(trials = 10)
  public void failsToParseSimpleStringReplyIfArrayReplyIsFound(byte[][][] arrays,
      @Only({"3", "5", "10", "100", "1000"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeArrayOfArrays(arrays));
    failsToParseReply(src, bufferSize, simpleStringReply(),
        "Command returned array reply while simple string reply was expected");
  }

  @Property
  public void parsesBulkStringReply(byte[] bytes, @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeBulkString(bytes));
    assertThat(parseReply(src, bufferSize, bulkStringReply(new TestBulkStringBuilderFactory()), Function.identity(),
        assertNoFailure(), charsetDecoder), equalTo(bytes));
    verifyZeroInteractions(charsetDecoder);
  }

  @Property
  public void parsesNullBulkStringReply(@Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeNilBulkString());
    BulkStringBuilderFactory<?, ?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    assertThat(
        parseReply(src, bufferSize, bulkStringReply(bulkStringBuilderFactory), Function.identity(), assertNoFailure(),
            charsetDecoder), nullValue());
    verifyZeroInteractions(charsetDecoder);
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Property
  public void parsesErrorBulkStringReply(@From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    BulkStringBuilderFactory<?, ?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    parsesError(s, bufferSize, bulkStringReply(bulkStringBuilderFactory));
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Property
  public void failsToParseBulkStringReplyIfIntegerReplyIsFound(long num,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeInteger(num));
    BulkStringBuilderFactory<?, ?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, bulkStringReply(bulkStringBuilderFactory),
        "Command returned integer reply while bulk string reply was expected");
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Property
  public void failsToParseBulkStringReplyIfSimpleStringReplyIsFound(
      @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeSimpleString(s));
    BulkStringBuilderFactory<?, ?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, bulkStringReply(bulkStringBuilderFactory),
        "Command returned simple string reply while bulk string reply was expected");
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Property(trials = 10)
  public void failsToParseBulkStringReplyIfArrayReplyIsFound(byte[][][] arrays,
      @Only({"3", "5", "10", "100", "1000"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeArrayOfArrays(arrays));
    BulkStringBuilderFactory<?, ?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, bulkStringReply(bulkStringBuilderFactory),
        "Command returned array reply while bulk string reply was expected");
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Property(trials = 40)
  public void parsesArrayReply(byte[][] arrays,
      @Only({"1", "3", "5", "10", "100", "1000"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeArray(arrays));
    assertThat(parseReply(src, bufferSize, arrayReply(array(byte[][]::new), new TestBulkStringBuilderFactory()),
        Function.identity(), assertNoFailure(), charsetDecoder), equalTo(arrays));
    verifyZeroInteractions(charsetDecoder);
  }

  @Property
  public <E> void parsesNullArrayReply(@Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeNilArray());
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

  @Property
  public <E> void parsesErrorArrayReply(@From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, ?> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<?, E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    parsesError(s, bufferSize, arrayReply(arrayBuilderFactory, bulkStringBuilderFactory));
    verifyZeroInteractions(arrayBuilderFactory);
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Property
  public <E> void failsToParseArrayReplyIfIntegerReplyIsFound(long num,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeInteger(num));
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, ?> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<?, E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, arrayReply(arrayBuilderFactory, bulkStringBuilderFactory),
        "Command returned integer reply while array reply was expected");
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Property
  public <E> void failsToParseArrayReplyIfSimpleStringReplyIsFound(
      @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeSimpleString(s));
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, ?> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<?, E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, arrayReply(arrayBuilderFactory, bulkStringBuilderFactory),
        "Command returned simple string reply while array reply was expected");
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Property
  public <E> void failsToParseArrayReplyIfBulkStringReplyIsFound(byte[] bytes,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeBulkString(bytes));
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, ?> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<?, E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, arrayReply(arrayBuilderFactory, bulkStringBuilderFactory),
        "Command returned bulk string reply while array reply was expected");
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Property(trials = 10)
  public <E, T> void failsToParseArrayOfBulkStringsReplyIfArrayOfArraysReplyIsFound(
      byte[][][] arrays, @Only({"3", "5", "10", "100", "1000"}) int bufferSize) {
    assumeThat(arrays, arrayWithSize(greaterThan(0)));
    ByteBuffer src = ByteBuffer.wrap(encodeArrayOfArrays(arrays));
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, T> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") ArrayBuilderFactory.Builder<E, T> builder = mock(ArrayBuilderFactory.Builder.class);
    when(arrayBuilderFactory.create(anyInt())).thenReturn(builder);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<?, E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, arrayReply(arrayBuilderFactory, bulkStringBuilderFactory),
        "Command returned array reply while bulk string reply was expected");
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Property(trials = 50)
  public void parsesScanReply(@InRange(minLong = 0) long cursor,
      byte[][] elements, @Only({"3", "5", "10", "100", "1000"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeScanReply(cursor, elements));
    ScanResult<byte[][]> scanResult =
        parseReply(src, bufferSize, scanReply(array(byte[][]::new), new TestBulkStringBuilderFactory()),
            Function.identity(), assertNoFailure(), charsetDecoder);
    assertThat(scanResult.cursor, equalTo(cursor));
    assertThat(scanResult.elements, equalTo(elements));
    verifyZeroInteractions(charsetDecoder);
  }

  @Property
  public <E> void parsesErrorScanReply(@From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, ?> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<?, E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    parsesError(s, bufferSize, scanReply(arrayBuilderFactory, bulkStringBuilderFactory));
    verifyZeroInteractions(arrayBuilderFactory);
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  private void parsesError(String error, int bufferSize, ReplyParser<?> parser) {
    ByteBuffer src = ByteBuffer.wrap(encodeError(error));
    assertThat(parseReply(src, bufferSize, parser, assertNoResult(), e -> e, charsetDecoder),
        allOf(instanceOf(RedisException.class), hasMessage(equalTo(error))));
    verifyZeroInteractions(charsetDecoder);
  }

  private <T> void failsToParseReply(ByteBuffer src, int bufferSize, ReplyParser<T> parser, String message) {
    assertThat(parseReply(src, bufferSize, parser, assertNoResult(), e -> e, charsetDecoder),
        allOf(instanceOf(ReplyParseException.class), hasMessage(equalTo(message))));
    verifyZeroInteractions(charsetDecoder);
  }
}

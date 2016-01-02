package redradishes.decoder;

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
import static redradishes.decoder.parser.TestUtil.assertNoFailure;
import static redradishes.decoder.parser.TestUtil.assertNoResult;
import static redradishes.decoder.parser.TestUtil.encodeArray;
import static redradishes.decoder.parser.TestUtil.encodeArrayOfArrays;
import static redradishes.decoder.parser.TestUtil.encodeByteString;
import static redradishes.decoder.parser.TestUtil.encodeError;
import static redradishes.decoder.parser.TestUtil.encodeInteger;
import static redradishes.decoder.parser.TestUtil.encodeNilArray;
import static redradishes.decoder.parser.TestUtil.encodeNilBulkString;
import static redradishes.decoder.parser.TestUtil.encodeScanReply;
import static redradishes.decoder.parser.TestUtil.encodeSimpleString;
import static redradishes.decoder.parser.TestUtil.parseReply;
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
    ByteBuffer src = ByteBuffer.wrap(encodeInteger(num));
    assertThat(parseReply(src, bufferSize, integerReply(), Function.identity(), assertNoFailure(), charsetDecoder),
        equalTo(num));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesNullIntegerReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeNilBulkString());
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
    ByteBuffer src = ByteBuffer.wrap(encodeSimpleString(s));
    failsToParseReply(src, bufferSize, integerReply(),
        "Command returned simple string reply while integer reply reply was expected");
  }

  @Theory
  public void failsToParseIntegerReplyIfArrayReplyIsFound(@ForAll(sampleSize = 10) byte[][][] arrays,
      @TestedOn(ints = {2, 3, 5, 10, 100, 1000}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeArrayOfArrays(arrays));
    failsToParseReply(src, bufferSize, integerReply(),
        "Command returned array reply while integer reply reply was expected");
  }

  @Theory
  public void parsesLongReply(@ForAll long num, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeInteger(num));
    assertThat(parseReply(src, bufferSize, longReply(), Function.identity(), assertNoFailure(), charsetDecoder),
        equalTo(num));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesNullLongReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeNilBulkString());
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
    ByteBuffer src = ByteBuffer.wrap(encodeSimpleString(s));
    failsToParseReply(src, bufferSize, longReply(),
        "Command returned simple string reply while integer reply reply was expected");
  }

  @Theory
  public void failsToParseLongReplyIfArrayReplyIsFound(@ForAll(sampleSize = 10) byte[][][] arrays,
      @TestedOn(ints = {2, 3, 5, 10, 100, 1000}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeArrayOfArrays(arrays));
    failsToParseReply(src, bufferSize, longReply(),
        "Command returned array reply while integer reply reply was expected");
  }

  @Theory
  public void parsesSimpleStringReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeSimpleString(s));
    assertThat(parseReply(src, bufferSize, simpleStringReply(), Function.identity(), assertNoFailure(), charsetDecoder),
        hasSameContentAs(s));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesNullStringReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeNilBulkString());
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
  public void failsToParseSimpleStringReplyIfArrayReplyIsFound(@ForAll(sampleSize = 10) byte[][][] arrays,
      @TestedOn(ints = {2, 3, 5, 10, 100, 1000}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeArrayOfArrays(arrays));
    failsToParseReply(src, bufferSize, simpleStringReply(),
        "Command returned array reply while simple string reply reply was expected");
  }

  @Theory
  public void parsesBulkStringReply(@ForAll byte[] bytes, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeByteString(bytes));
    assertThat(parseReply(src, bufferSize, bulkStringReply(new TestBulkStringBuilderFactory()), Function.identity(),
        assertNoFailure(), charsetDecoder), equalTo(bytes));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesNullBulkStringReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeNilBulkString());
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
    ByteBuffer src = ByteBuffer.wrap(encodeInteger(num));
    BulkStringBuilderFactory<?, ?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, bulkStringReply(bulkStringBuilderFactory),
        "Command returned integer reply while bulk string reply reply was expected");
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public void failsToParseBulkStringReplyIfSimpleStringReplyIsFound(
      @ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeSimpleString(s));
    BulkStringBuilderFactory<?, ?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, bulkStringReply(bulkStringBuilderFactory),
        "Command returned simple string reply while bulk string reply reply was expected");
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public void failsToParseBulkStringReplyIfArrayReplyIsFound(@ForAll(sampleSize = 10) byte[][][] arrays,
      @TestedOn(ints = {2, 3, 5, 10, 100, 1000}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeArrayOfArrays(arrays));
    BulkStringBuilderFactory<?, ?> bulkStringBuilderFactory = mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, bulkStringReply(bulkStringBuilderFactory),
        "Command returned array reply while bulk string reply reply was expected");
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public void parsesArrayReply(@ForAll byte[][] arrays, @TestedOn(ints = {2, 3, 5, 10, 100, 1000}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeArray(arrays));
    assertThat(parseReply(src, bufferSize, arrayReply(array(byte[][]::new), new TestBulkStringBuilderFactory()),
        Function.identity(), assertNoFailure(), charsetDecoder), equalTo(arrays));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public <E> void parsesNullArrayReply(@TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
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
    ByteBuffer src = ByteBuffer.wrap(encodeInteger(num));
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, ?> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<?, E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, arrayReply(arrayBuilderFactory, bulkStringBuilderFactory),
        "Command returned integer reply while array reply reply was expected");
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public <E> void failsToParseArrayReplyIfSimpleStringReplyIsFound(
      @ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeSimpleString(s));
    @SuppressWarnings("unchecked") ArrayBuilderFactory<E, ?> arrayBuilderFactory = mock(ArrayBuilderFactory.class);
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<?, E> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    failsToParseReply(src, bufferSize, arrayReply(arrayBuilderFactory, bulkStringBuilderFactory),
        "Command returned simple string reply while array reply reply was expected");
    verifyZeroInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public void parsesScanReply(@ForAll(sampleSize = 10) @InRange(minLong = 0) long cursor, @ForAll byte[][] elements,
      @TestedOn(ints = {2, 3, 5, 10, 100, 1000}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeScanReply(cursor, elements));
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

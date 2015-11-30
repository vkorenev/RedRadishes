package redradishes.decoder;

import com.pholser.junit.quickcheck.ForAll;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.java.lang.Encoded;
import org.junit.Rule;
import org.junit.contrib.theories.DataPoints;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.contrib.theories.suppliers.TestedOn;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static redradishes.decoder.BulkStringBuilders._long;
import static redradishes.decoder.BulkStringBuilders.byteArray;
import static redradishes.decoder.BulkStringBuilders.charSequence;
import static redradishes.decoder.BulkStringBuilders.integer;
import static redradishes.decoder.BulkStringBuilders.string;
import static redradishes.decoder.Replies.bulkStringReply;
import static redradishes.decoder.parser.TestUtil.getByteString;
import static redradishes.decoder.parser.TestUtil.parseReply;
import static redradishes.decoder.parser.TestUtil.split;
import static redradishes.decoder.parser.TestUtil.throwingFailureHandler;
import static redradishes.hamcrest.HasSameContentAs.hasSameContentAs;

@RunWith(Theories.class)
public class BulkStringBuildersTest {
  @Rule
  public final MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  private CharsetDecoder charsetDecoder;

  @DataPoints
  public static final Charset[] CHARSETS = {UTF_8, UTF_16BE, UTF_16LE};

  @Theory
  public void parsesCharSequences(@ForAll @From(Encoded.class) @Encoded.InCharset("ISO-8859-1") String value,
      @TestedOn(ints = {1, 2, 3, 5, 10, 100, 1000}) int bufferSize) {
    parsesCharSequences(value, bufferSize, ISO_8859_1);
  }

  @Theory
  public void parsesCharSequences(@ForAll String value, @TestedOn(ints = {1, 2, 3, 5, 10, 100, 1000}) int bufferSize,
      Charset charset) {
    Iterator<ByteBuffer> chunks = split(getByteString(value.getBytes(charset)), bufferSize);
    CharSequence actual =
        parseReply(chunks, bulkStringReply(charSequence()), Function.identity(), throwingFailureHandler(),
            charset.newDecoder());
    assertThat(actual, hasSameContentAs(value));
  }

  @Theory
  public void parsesStrings(@ForAll @From(Encoded.class) @Encoded.InCharset("ISO-8859-1") String value,
      @TestedOn(ints = {1, 2, 3, 5, 10, 100, 1000}) int bufferSize) {
    parsesStrings(value, bufferSize, ISO_8859_1);
  }

  @Theory
  public void parsesStrings(@ForAll String value, @TestedOn(ints = {1, 2, 3, 5, 10, 100, 1000}) int bufferSize,
      Charset charset) {
    Iterator<ByteBuffer> chunks = split(getByteString(value.getBytes(charset)), bufferSize);
    CharSequence actual = parseReply(chunks, bulkStringReply(string()), Function.identity(), throwingFailureHandler(),
        charset.newDecoder());
    assertThat(actual, equalTo(value));
  }

  @Theory
  public void parsesIntegers(@ForAll int value, @TestedOn(ints = {1, 2, 3, 5, 10, 100, 1000}) int bufferSize) {
    Iterator<ByteBuffer> chunks = split(getByteString(Integer.toString(value).getBytes(US_ASCII)), bufferSize);
    assertThat(
        parseReply(chunks, bulkStringReply(integer()), Function.identity(), throwingFailureHandler(), charsetDecoder),
        equalTo(value));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesLongs(@ForAll long value, @TestedOn(ints = {1, 2, 3, 5, 10, 100, 1000}) int bufferSize) {
    Iterator<ByteBuffer> chunks = split(getByteString(Long.toString(value).getBytes(US_ASCII)), bufferSize);
    assertThat(
        parseReply(chunks, bulkStringReply(_long()), Function.identity(), throwingFailureHandler(), charsetDecoder),
        equalTo(value));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesByteArrays(@ForAll byte[] value, @TestedOn(ints = {1, 2, 3, 5, 10, 100, 1000}) int bufferSize) {
    Iterator<ByteBuffer> chunks = split(getByteString(value), bufferSize);
    assertThat(
        parseReply(chunks, bulkStringReply(byteArray()), Function.identity(), throwingFailureHandler(), charsetDecoder),
        equalTo(value));
    verifyZeroInteractions(charsetDecoder);
  }
}

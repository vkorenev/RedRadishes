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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
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
import static redradishes.decoder.parser.TestUtil.assertNoFailure;
import static redradishes.decoder.parser.TestUtil.encodeBulkString;
import static redradishes.decoder.parser.TestUtil.parseReply;
import static redradishes.hamcrest.HasSameContentAs.hasSameContentAs;

@RunWith(JUnitQuickcheck.class)
public class BulkStringBuildersTest {
  @Rule
  public final MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  private CharsetDecoder charsetDecoder;

  public static final Charset[] CHARSETS = {UTF_8, UTF_16BE, UTF_16LE};

  @Property
  public void parsesCharSequences(@From(Encoded.class) @Encoded.InCharset("ISO-8859-1") String value,
      @Only({"1", "2", "3", "5", "10", "100", "1000"}) int bufferSize) {
    assertParsesCharSequences(value, bufferSize, ISO_8859_1);
  }

  @Property
  public void parsesCharSequences(String value, @Only({"4", "5", "6", "7", "10", "100", "1000"}) int bufferSize,
      @Only({"0", "1", "2"}) int charsetIndex) {
    Charset charset = CHARSETS[charsetIndex];
    assertParsesCharSequences(value, bufferSize, charset);
  }

  private void assertParsesCharSequences(String value, int bufferSize, Charset charset) {
    ByteBuffer src = ByteBuffer.wrap(encodeBulkString(value.getBytes(charset)));
    CharSequence actual =
        parseReply(src, bufferSize, bulkStringReply(charSequence()), Function.identity(), assertNoFailure(),
            charset.newDecoder());
    assertThat(actual, hasSameContentAs(value));
  }

  @Property
  public void parsesStrings(@From(Encoded.class) @Encoded.InCharset("ISO-8859-1") String value,
      @Only({"1", "2", "3", "5", "10", "100", "1000"}) int bufferSize) {
    assertParsesStrings(value, bufferSize, ISO_8859_1);
  }

  @Property
  public void parsesStrings(String value, @Only({"4", "5", "6", "7", "10", "100", "1000"}) int bufferSize,
      @Only({"0", "1", "2"}) int charsetIndex) {
    Charset charset = CHARSETS[charsetIndex];
    assertParsesStrings(value, bufferSize, charset);
  }

  private void assertParsesStrings(String value, int bufferSize, Charset charset) {
    ByteBuffer src = ByteBuffer.wrap(encodeBulkString(value.getBytes(charset)));
    CharSequence actual = parseReply(src, bufferSize, bulkStringReply(string()), Function.identity(), assertNoFailure(),
        charset.newDecoder());
    assertThat(actual, equalTo(value));
  }

  @Property
  public void parsesIntegers(int value, @Only({"1", "2", "3", "5", "10", "100", "1000"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeBulkString(Integer.toString(value).getBytes(US_ASCII)));
    assertThat(
        parseReply(src, bufferSize, bulkStringReply(integer()), Function.identity(), assertNoFailure(), charsetDecoder),
        equalTo(value));
    verifyZeroInteractions(charsetDecoder);
  }

  @Property
  public void parsesLongs(long value, @Only({"1", "2", "3", "5", "10", "100", "1000"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeBulkString(Long.toString(value).getBytes(US_ASCII)));
    assertThat(
        parseReply(src, bufferSize, bulkStringReply(_long()), Function.identity(), assertNoFailure(), charsetDecoder),
        equalTo(value));
    verifyZeroInteractions(charsetDecoder);
  }

  @Property
  public void parsesByteArrays(byte[] value, @Only({"1", "2", "3", "5", "10", "100", "1000"}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(encodeBulkString(value));
    assertThat(parseReply(src, bufferSize, bulkStringReply(byteArray()), Function.identity(), assertNoFailure(),
        charsetDecoder), equalTo(value));
    verifyZeroInteractions(charsetDecoder);
  }

  @Property
  public void allocatesCharBufferOfTheRightSize(@InRange(minInt = 0, maxInt = 100_000_000) int length) {
    CharBuffer charBuffer = (CharBuffer) charSequence().create(length, UTF_8.newDecoder());
    assertThat(charBuffer.length(), equalTo(length));
  }
}

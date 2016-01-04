package redradishes.decoder.parser;

import com.pholser.junit.quickcheck.ForAll;
import org.junit.Rule;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.contrib.theories.suppliers.TestedOn;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static redradishes.decoder.parser.TestUtil.assertNoFailure;
import static redradishes.decoder.parser.TestUtil.parseReply;

@RunWith(Theories.class)
public class LongParserTest {
  @Rule
  public final MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  private CharsetDecoder charsetDecoder;

  @Theory
  public void parsesIntegers(@ForAll int num, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap((num + "\r\n").getBytes(US_ASCII));
    assertThat(
        parseReply(src, bufferSize, LongParser.INTEGER_PARSER, Function.identity(), assertNoFailure(), charsetDecoder),
        equalTo(num));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesLongs(@ForAll long num, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap((num + "\r\n").getBytes(US_ASCII));
    assertThat(
        parseReply(src, bufferSize, LongParser.LONG_PARSER, Function.identity(), assertNoFailure(), charsetDecoder),
        equalTo(num));
    verifyZeroInteractions(charsetDecoder);
  }
}

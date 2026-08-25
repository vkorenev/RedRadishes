package redradishes.decoder.parser;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.java.lang.Encoded;
import org.junit.Rule;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import com.pholser.junit.quickcheck.generator.Only;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static redradishes.decoder.parser.CharAppendingParser.CHAR_SEQUENCE_PARSER;
import static redradishes.decoder.parser.TestUtil.assertNoFailure;
import static redradishes.decoder.parser.TestUtil.assertNoResult;
import static redradishes.decoder.parser.TestUtil.parseReply;
import static redradishes.hamcrest.HasSameContentAs.hasSameContentAs;

@RunWith(JUnitQuickcheck.class)
public class CharAppendingParserTest {
  @Rule
  public final MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  private CharsetDecoder charsetDecoder;

  @Property
  public void parsesStrings(@From(Encoded.class) @Encoded.InCharset("US-ASCII") String value,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) {
    assumeTrue(value.indexOf('\r') == -1 && value.indexOf('\n') == -1);

    ByteBuffer src = ByteBuffer.wrap((value + "\r\n").getBytes(US_ASCII));
    assertThat(
        parseReply(src, bufferSize, CHAR_SEQUENCE_PARSER, Function.identity(), assertNoFailure(), charsetDecoder),
        hasSameContentAs(value));
    verifyZeroInteractions(charsetDecoder);
  }

  @Property
  public void reportsFailure(@From(Encoded.class) @Encoded.InCharset("US-ASCII") String value,
      @Only({"1", "2", "3", "5", "100"}) int bufferSize) throws Exception {
    assumeTrue(value.indexOf('\r') == -1 && value.indexOf('\n') == -1);
    assumeThat(value.length(), greaterThan(0));

    Appendable appendable = mock(Appendable.class);
    RuntimeException e1 = new RuntimeException();
    RuntimeException e2 = new RuntimeException();
    when(appendable.append(anyChar())).thenThrow(e1, e2);

    ByteBuffer src = ByteBuffer.wrap((value + "\r\n").getBytes(US_ASCII));
    assertThat(parseReply(src, bufferSize, new CharAppendingParser<>(() -> appendable), assertNoResult(), e -> e,
        charsetDecoder), equalTo(e1));
    verifyZeroInteractions(charsetDecoder);
    verify(appendable, times(value.length())).append(anyChar());
    verifyNoMoreInteractions(appendable);
  }
}

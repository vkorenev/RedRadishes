package redradishes.decoder.parser;

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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static redradishes.decoder.parser.TestParser.failingParser;
import static redradishes.decoder.parser.TestParser.parsableInput;
import static redradishes.decoder.parser.TestParser.succeedingParser;
import static redradishes.decoder.parser.TestUtil.assertNoFailure;
import static redradishes.decoder.parser.TestUtil.assertNoResult;
import static redradishes.decoder.parser.TestUtil.parseReply;

@RunWith(Theories.class)
public class TestParserTest {
  @Rule
  public final MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  private CharsetDecoder charsetDecoder;

  @Theory
  public void succeeds(@TestedOn(ints = {1, 2, 3, 5, 7}) int replyLen,
      @TestedOn(ints = {1, 2, 3, 5, 7, 10, 100}) int bufferSize) {
    String reply = "1";
    ReplyParser<String> parser = succeedingParser(reply, replyLen);

    ByteBuffer src = ByteBuffer.wrap(parsableInput(replyLen));
    assertThat(parseReply(src, bufferSize, parser, Function.identity(), assertNoFailure(), charsetDecoder),
        equalTo(reply));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void fails(@TestedOn(ints = {1, 2, 3, 5, 7}) int replyLen,
      @TestedOn(ints = {1, 2, 3, 5, 7, 10, 100}) int bufferSize) {
    Exception exception1 = new Exception("1");
    ReplyParser<String> parser = failingParser(exception1, replyLen);

    ByteBuffer src = ByteBuffer.wrap(parsableInput(replyLen));
    assertThat(parseReply(src, bufferSize, parser, assertNoResult(), e -> e, charsetDecoder), sameInstance(exception1));
    verifyZeroInteractions(charsetDecoder);
  }
}

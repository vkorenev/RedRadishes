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
public class CombiningReplyParserTest {
  @Rule
  public final MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  private CharsetDecoder charsetDecoder;

  @Theory
  public void bothSucceed(@TestedOn(ints = {1, 2, 3, 5, 7}) int replyLen1,
      @TestedOn(ints = {1, 2, 3, 5, 7}) int replyLen2, @TestedOn(ints = {1, 2, 3, 5, 7, 10, 100}) int bufferSize)
      throws Exception {
    String reply1 = "1";
    String reply2 = "2";
    ReplyParser<String> parser =
        CombiningReplyParser.combine(succeedingParser(reply1, replyLen1), succeedingParser(reply2, replyLen2))
            .mapToParser((a, b) -> a + b);

    ByteBuffer src = ByteBuffer.wrap(parsableInput(replyLen1, replyLen2));
    assertThat(parseReply(src, bufferSize, parser, Function.identity(), assertNoFailure(), charsetDecoder),
        equalTo(reply1 + reply2));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void firstFails(@TestedOn(ints = {1, 2, 3, 5, 7}) int replyLen1,
      @TestedOn(ints = {1, 2, 3, 5, 7}) int replyLen2, @TestedOn(ints = {1, 2, 3, 5, 7, 10, 100}) int bufferSize)
      throws Exception {
    Exception exception1 = new Exception("1");
    String reply2 = "2";
    ReplyParser<String> parser =
        CombiningReplyParser.combine(failingParser(exception1, replyLen1), succeedingParser(reply2, replyLen2))
            .mapToParser((a, b) -> a + b);

    ByteBuffer src = ByteBuffer.wrap(parsableInput(replyLen1, replyLen2));
    assertThat(parseReply(src, bufferSize, parser, assertNoResult(), e -> e, charsetDecoder), sameInstance(exception1));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void secondFails(@TestedOn(ints = {1, 2, 3, 5, 7}) int replyLen1,
      @TestedOn(ints = {1, 2, 3, 5, 7}) int replyLen2, @TestedOn(ints = {1, 2, 3, 5, 7, 10, 100}) int bufferSize)
      throws Exception {
    String reply1 = "1";
    Exception exception2 = new Exception("2");
    ReplyParser<String> parser =
        CombiningReplyParser.combine(succeedingParser(reply1, replyLen1), failingParser(exception2, replyLen2))
            .mapToParser((a, b) -> a + b);

    ByteBuffer src = ByteBuffer.wrap(parsableInput(replyLen1, replyLen2));
    assertThat(parseReply(src, bufferSize, parser, assertNoResult(), e -> e, charsetDecoder), sameInstance(exception2));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void bothFail(@TestedOn(ints = {1, 2, 3, 5, 7}) int replyLen1, @TestedOn(ints = {1, 2, 3, 5, 7}) int replyLen2,
      @TestedOn(ints = {1, 2, 3, 5, 7, 10, 100}) int bufferSize) throws Exception {
    Exception exception1 = new Exception("1");
    Exception exception2 = new Exception("2");
    ReplyParser<String> parser = CombiningReplyParser.<String, String>combine(failingParser(exception1, replyLen1),
        failingParser(exception2, replyLen2)).mapToParser((a, b) -> a + b);

    ByteBuffer src = ByteBuffer.wrap(parsableInput(replyLen1, replyLen2));
    assertThat(parseReply(src, bufferSize, parser, assertNoResult(), e -> e, charsetDecoder), sameInstance(exception1));
    verifyZeroInteractions(charsetDecoder);
  }
}

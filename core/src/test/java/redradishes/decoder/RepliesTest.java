package redradishes.decoder;

import com.pholser.junit.quickcheck.ForAll;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.java.lang.Encoded;
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
import java.util.Iterator;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static redradishes.decoder.parser.TestUtil.parseReply;
import static redradishes.decoder.parser.TestUtil.split;

@RunWith(Theories.class)
public class RepliesTest {
  @Rule
  public final MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  private CharsetDecoder charsetDecoder;

  @Theory
  public void parsesIntegerReply(@ForAll int num, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    Iterator<ByteBuffer> chunks = split((":" + num + "\r\n").getBytes(US_ASCII), bufferSize);
    assertThat(parseReply(chunks, Replies.integerReply(), Function.identity(), message -> {
      throw new RuntimeException(message.toString());
    }, charsetDecoder), equalTo(num));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesLongReply(@ForAll long num, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    Iterator<ByteBuffer> chunks = split((":" + num + "\r\n").getBytes(US_ASCII), bufferSize);
    assertThat(parseReply(chunks, Replies.longReply(), Function.identity(), message -> {
      throw new RuntimeException(message.toString());
    }, charsetDecoder), equalTo(num));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public void parsesSimpleStringReply(@ForAll @From(Encoded.class) @Encoded.InCharset("US-ASCII") String s,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    String value = s.replace('\r', ' ').replace('\n', ' ');
    Iterator<ByteBuffer> chunks = split(("+" + value + "\r\n").getBytes(US_ASCII), bufferSize);
    assertThat(parseReply(chunks, Replies.simpleStringReply(), Function.identity(), message -> {
      throw new RuntimeException(message.toString());
    }, charsetDecoder).toString(), equalTo(value));
    verifyZeroInteractions(charsetDecoder);
  }
}

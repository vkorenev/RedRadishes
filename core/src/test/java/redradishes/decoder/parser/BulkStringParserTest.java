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
import redradishes.decoder.BulkStringBuilderFactory;
import redradishes.decoder.ReplyParseException;
import redradishes.decoder.TestBulkStringBuilderFactory;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.function.Function;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static redradishes.decoder.parser.TestUtil.assertNoFailure;
import static redradishes.decoder.parser.TestUtil.assertNoResult;
import static redradishes.decoder.parser.TestUtil.parseReply;
import static redradishes.hamcrest.ThrowableMessageMatcher.hasMessage;

@RunWith(Theories.class)
public class BulkStringParserTest {
  @Rule
  public final MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  private CharsetDecoder charsetDecoder;

  @Theory
  public void parses(@ForAll byte[] bytes, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ByteBuffer src = ByteBuffer.wrap(appendCRLF(bytes));
    assertThat(parseReply(src, bufferSize, new BulkStringParser<>(bytes.length, new TestBulkStringBuilderFactory()),
        Function.identity(), assertNoFailure(), charsetDecoder), equalTo(bytes));
    verifyZeroInteractions(charsetDecoder);
  }

  @Theory
  public <B> void returnsFailureIfBulkStringBuilderThrowsException(@ForAll byte[] bytes,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) throws Exception {
    @SuppressWarnings("unchecked") BulkStringBuilderFactory<B, ?> bulkStringBuilderFactory =
        mock(BulkStringBuilderFactory.class);
    RuntimeException e1 = new RuntimeException();
    RuntimeException e2 = new RuntimeException();
    when(bulkStringBuilderFactory.append(any(), any(), any())).thenThrow(e1, e2);
    boolean oneChunk = bytes.length <= bufferSize;
    when(bulkStringBuilderFactory.appendLast(any(), any(), any())).thenThrow(oneChunk ? e1 : e2);

    ByteBuffer src = ByteBuffer.wrap(appendCRLF(bytes));
    assertThat(
        parseReply(src, bufferSize, new BulkStringParser<>(bytes.length, bulkStringBuilderFactory), assertNoResult(),
            e -> e, charsetDecoder), equalTo(e1));
    verifyZeroInteractions(charsetDecoder);
    verify(bulkStringBuilderFactory).create(bytes.length, charsetDecoder);
    if (oneChunk) {
      verify(bulkStringBuilderFactory).appendLast(any(), any(), eq(charsetDecoder));
    } else {
      verify(bulkStringBuilderFactory).append(any(), any(), eq(charsetDecoder));
    }
    verifyNoMoreInteractions(bulkStringBuilderFactory);
  }

  @Theory
  public <B, R> void returnsFailureIfNotAllBytesAreConsumedInAppendLast(@ForAll byte[] bytes,
      @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) throws Exception {
    assumeThat(bytes.length, greaterThan(0));
    BulkStringBuilderFactory<B, R> bulkStringBuilderFactory = new BulkStringBuilderFactory<B, R>() {
      @Override
      public B create(int length, CharsetDecoder charsetDecoder) {
        return null;
      }

      @Override
      public B append(B builder, ByteBuffer buffer, CharsetDecoder charsetDecoder) throws Exception {
        buffer.get();
        return null;
      }

      @Override
      public R appendLast(B builder, ByteBuffer buffer, CharsetDecoder charsetDecoder) throws Exception {
        int remaining = buffer.remaining();
        buffer.position(buffer.position() + remaining - 1);
        return null;
      }
    };

    ByteBuffer src = ByteBuffer.wrap(appendCRLF(bytes));
    assertThat(
        parseReply(src, bufferSize, new BulkStringParser<>(bytes.length, bulkStringBuilderFactory), assertNoResult(),
            e -> e, charsetDecoder), allOf(instanceOf(ReplyParseException.class),
            hasMessage(equalTo("Bulk string decoder has not consumed all input"))));
  }

  private static byte[] appendCRLF(byte[] bytes) {
    byte[] result = Arrays.copyOf(bytes, bytes.length + 2);
    result[bytes.length] = '\r';
    result[bytes.length + 1] = '\n';
    return result;
  }
}

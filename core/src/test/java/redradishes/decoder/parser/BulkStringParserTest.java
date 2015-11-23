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

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static redradishes.decoder.parser.TestUtil.parse;
import static redradishes.decoder.parser.TestUtil.split;

@RunWith(Theories.class)
public class BulkStringParserTest {
  @Rule
  public final MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  private CharsetDecoder charsetDecoder;

  @Theory
  public void parses(@ForAll byte[] bytes, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    Parser<byte[]> parser = new BulkStringParser<>(bytes.length,
        (length, charsetDecoder) -> new BulkStringBuilderFactory.Builder<byte[]>() {
          private final byte[] bytes = new byte[length];
          private int offset = 0;
          private boolean finalized = false;

          @Override
          public void append(ByteBuffer buffer) {
            if (finalized) throw new IllegalStateException();
            int len = buffer.remaining();
            buffer.get(bytes, offset, len);
            offset += len;
          }

          @Override
          public void appendLast(ByteBuffer buffer) {
            append(buffer);
            finalized = true;
          }

          @Override
          public byte[] build() {
            if (!finalized) throw new IllegalStateException();
            return bytes;
          }
        });
    Iterator<ByteBuffer> chunks = split(appendCRLF(bytes), bufferSize);
    assertThat(parse(chunks, parser, Function.identity(), charsetDecoder), equalTo(bytes));
    verifyZeroInteractions(charsetDecoder);
  }

  private static byte[] appendCRLF(byte[] bytes) {
    byte[] result = Arrays.copyOf(bytes, bytes.length + 2);
    result[bytes.length] = '\r';
    result[bytes.length + 1] = '\n';
    return result;
  }
}

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
import redradishes.decoder.TestBulkStringBuilderFactory;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static redradishes.decoder.parser.TestUtil.assertNoFailure;
import static redradishes.decoder.parser.TestUtil.parseReply;
import static redradishes.decoder.parser.TestUtil.split;

@RunWith(Theories.class)
public class BulkStringParserTest {
  @Rule
  public final MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  private CharsetDecoder charsetDecoder;

  @Theory
  public void parses(@ForAll byte[] bytes, @TestedOn(ints = {1, 2, 3, 5, 100}) int bufferSize) {
    ReplyParser<byte[]> parser = new BulkStringParser<>(bytes.length, new TestBulkStringBuilderFactory());
    Iterator<ByteBuffer> chunks = split(appendCRLF(bytes), bufferSize);
    assertThat(parseReply(chunks, parser, Function.identity(), assertNoFailure(), charsetDecoder), equalTo(bytes));
    verifyZeroInteractions(charsetDecoder);
  }

  private static byte[] appendCRLF(byte[] bytes) {
    byte[] result = Arrays.copyOf(bytes, bytes.length + 2);
    result[bytes.length] = '\r';
    result[bytes.length + 1] = '\n';
    return result;
  }
}

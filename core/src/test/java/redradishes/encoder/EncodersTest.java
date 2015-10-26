package redradishes.encoder;

import com.google.common.primitives.Bytes;
import com.pholser.junit.quickcheck.ForAll;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static redradishes.encoder.Encoders.collArg;
import static redradishes.encoder.Encoders.intArg;
import static redradishes.encoder.TestUtil.serialize;

@RunWith(Theories.class)
public class EncodersTest {
  @Theory
  public void testLongArrayArg(@ForAll long[] val) throws Exception {
    ConstExpr writer = Encoders.longArrayArg().encode(val);
    assertEquals(writer.size(), val.length);
    assertThat(serialize(writer), equalTo(Bytes.concat(
        Arrays.stream(val).mapToObj(Long::toString).map(s -> s.getBytes(ISO_8859_1)).map(TestUtil::respBulkString)
            .toArray(byte[][]::new))));
  }

  @Theory
  public void testIntArrayArg(@ForAll int[] val) throws Exception {
    ConstExpr writer = Encoders.intArrayArg().encode(val);
    assertEquals(writer.size(), val.length);
    assertThat(serialize(writer), equalTo(Bytes.concat(
        Arrays.stream(val).mapToObj(Integer::toString).map(s -> s.getBytes(ISO_8859_1)).map(TestUtil::respBulkString)
            .toArray(byte[][]::new))));
  }

  @Theory
  public void testCollArg(@ForAll List<Integer> val) {
    ConstExpr c = collArg(intArg()).encode(val);
    assertEquals(val.size(), c.size());
    assertThat(serialize(c), equalTo(Bytes.concat(
        val.stream().map(Object::toString).map(s -> s.getBytes(ISO_8859_1)).map(TestUtil::respBulkString)
            .toArray(byte[][]::new))));
  }
}

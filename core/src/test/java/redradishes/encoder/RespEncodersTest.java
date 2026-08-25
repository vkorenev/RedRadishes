package redradishes.encoder;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.Only;
import com.pholser.junit.quickcheck.generator.java.lang.Encoded;
import com.pholser.junit.quickcheck.generator.java.lang.Encoded.InCharset;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static redradishes.encoder.TestUtil.respBulkString;
import static redradishes.encoder.TestUtil.serialize;

@RunWith(JUnitQuickcheck.class)
public class RespEncodersTest {
  public static final Charset[] CHARSETS = {UTF_8, UTF_16BE, UTF_16LE};

  @Property
  public void testArray(int i, boolean compact) {
    ConstExpr expr = RespEncoders.array().encode(i);
    ConstExpr c = compact ? expr.compact() : expr;
    assertEquals(0, c.size());
    assertThat(serialize(c), equalTo(String.format("*%d\r\n", i).getBytes(US_ASCII)));
  }

  @Property
  public void testOneByteCharsetStrBulkString(@From(Encoded.class) @InCharset("ISO-8859-1") String s,
      boolean compact) {
    assertStrBulkString(s, ISO_8859_1, compact);
  }

  @Property
  public void testStrBulkString(String s, @Only({"0", "1", "2"}) int charsetIndex, boolean compact) {
    Charset charset = CHARSETS[charsetIndex];
    assertStrBulkString(s, charset, compact);
  }

  private void assertStrBulkString(String s, Charset charset, boolean compact) {
    ConstExpr expr = RespEncoders.strBulkString(charset).encode(s);
    ConstExpr c = compact ? expr.compact() : expr;
    assertEquals(1, c.size());
    byte[] bytes = s.getBytes(charset);
    assertThat(serialize(c), equalTo(respBulkString(bytes)));
  }

  @Property
  public void testBytesBulkString(byte[] bytes, boolean compact) {
    ConstExpr expr = RespEncoders.bytesBulkString().encode(bytes);
    ConstExpr c = compact ? expr.compact() : expr;
    assertEquals(1, c.size());
    assertThat(serialize(c), equalTo(respBulkString(bytes)));
  }

  @Property
  public void testIntBulkString(
      @Only({"0", "1", "9", "10", "99", "100", "-1", "-9", "-10", "-99", "-100", "2147483647",
          "-2147483648"}) int i,
      boolean compact) {
    ConstExpr expr = RespEncoders.intBulkString().encode(i);
    ConstExpr c = compact ? expr.compact() : expr;
    assertEquals(1, c.size());
    String s = Integer.toString(i);
    assertThat(serialize(c), equalTo(String.format("$%d\r\n%s\r\n", s.length(), s).getBytes(US_ASCII)));
  }

  @Property
  public void testLongBulkString(
      @Only({"0", "1", "9", "10", "99", "100", "-1", "-9", "-10", "-99", "-100",
          "9223372036854775807", "-9223372036854775808"}) long i,
      boolean compact) {
    ConstExpr expr = RespEncoders.longBulkString().encode(i);
    ConstExpr c = compact ? expr.compact() : expr;
    assertEquals(1, c.size());
    String s = Long.toString(i);
    assertThat(serialize(c), equalTo(String.format("$%d\r\n%s\r\n", s.length(), s).getBytes(US_ASCII)));
  }

  @Property
  public void testToBytes(long i) throws Exception {
    assertThat(RespEncoders.toBytes(i), equalTo(Long.toString(i).getBytes(US_ASCII)));
  }
}

package redradishes.encoder;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import static java.nio.charset.StandardCharsets.US_ASCII;

class TestUtil {
  static byte[] serialize(ConstExpr c) {
    ByteArraySink byteArraySink = new ByteArraySink();
    c.writeTo(byteArraySink);
    return byteArraySink.geBytes();
  }

  static byte[] respBulkString(byte[] val) {
    ByteArrayDataOutput byteArrayOutputStream = ByteStreams.newDataOutput();
    byteArrayOutputStream.write('$');
    byteArrayOutputStream.write(Integer.toString(val.length).getBytes(US_ASCII));
    byteArrayOutputStream.write('\r');
    byteArrayOutputStream.write('\n');
    byteArrayOutputStream.write(val);
    byteArrayOutputStream.write('\r');
    byteArrayOutputStream.write('\n');
    return byteArrayOutputStream.toByteArray();
  }
}

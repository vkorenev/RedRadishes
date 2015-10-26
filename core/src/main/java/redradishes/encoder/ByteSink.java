package redradishes.encoder;

import java.nio.charset.CharsetEncoder;

public interface ByteSink {
  void write(byte b);

  void write(CharSequence s, CharsetEncoder charsetEncoder);

  void write(byte[] src);

  void write(byte[] src, int offset, int length);
}

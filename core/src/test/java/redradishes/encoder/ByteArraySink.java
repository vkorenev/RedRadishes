package redradishes.encoder;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import redradishes.UncheckedCharacterCodingException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;

class ByteArraySink implements ByteSink {
  private final ByteArrayDataOutput out = ByteStreams.newDataOutput();

  @Override
  public void write(byte b) {
    out.write(b);
  }

  @Override
  public void write(CharSequence s, CharsetEncoder charsetEncoder) {
    try {
      ByteBuffer byteBuffer = charsetEncoder.encode(CharBuffer.wrap(s));
      out.write(byteBuffer.array(), 0, byteBuffer.remaining());
    } catch (CharacterCodingException e) {
      throw new UncheckedCharacterCodingException(e);
    }
  }

  @Override
  public void write(byte[] src) {
    out.write(src);
  }

  @Override
  public void write(byte[] src, int offset, int length) {
    out.write(src, offset, length);
  }

  public byte[] geBytes() {
    return out.toByteArray();
  }
}

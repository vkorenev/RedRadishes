package redradishes;

import redradishes.encoder.ByteSink;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

class ByteBufferSink implements ByteSink {
  private final ByteBufferBundle byteBufferBundle;
  private ByteBuffer buffer;

  ByteBufferSink(ByteBufferBundle byteBufferBundle) {
    this.byteBufferBundle = byteBufferBundle;
    this.buffer = this.byteBufferBundle.get();
  }

  @Override
  public void write(byte b) {
    if (!buffer.hasRemaining()) {
      buffer = byteBufferBundle.getNew();
    }
    buffer.put(b);
  }

  @Override
  public void write(CharSequence s, CharsetEncoder charsetEncoder) {
    CharBuffer in = CharBuffer.wrap(s);
    try {
      charsetEncoder.reset();
      while (true) {
        CoderResult coderResult = in.hasRemaining() ? charsetEncoder.encode(in, buffer, true) : CoderResult.UNDERFLOW;
        if (coderResult.isUnderflow()) {
          coderResult = charsetEncoder.flush(buffer);
        }
        if (coderResult.isUnderflow()) {
          break;
        } else if (coderResult.isOverflow()) {
          buffer = byteBufferBundle.getNew();
        } else {
          coderResult.throwException();
        }
      }
    } catch (CharacterCodingException e) {
      throw new UncheckedCharacterCodingException(e);
    }
  }

  @Override
  public void write(byte[] src) {
    write(src, 0, src.length);
  }

  @Override
  public void write(byte[] src, int offset, int length) {
    while (true) {
      int freeSpace = buffer.remaining();
      if (freeSpace >= length) {
        buffer.put(src, offset, length);
        break;
      } else {
        buffer.put(src, offset, freeSpace);
        buffer = byteBufferBundle.getNew();
        offset += freeSpace;
        length -= freeSpace;
      }
    }
  }
}

package redradishes.encoder;

public interface ByteSink {
  void write(byte b);

  void write(CharSequence s);

  void write(byte[] src);

  void write(byte[] src, int offset, int length);
}

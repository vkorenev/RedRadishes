package redradishes.decoder.parser;

class UnexpectedArrayReplyParser<T> extends LenParser<T> {
  public UnexpectedArrayReplyParser() {
    super(len -> {
      throw new IllegalStateException("Unexpected array reply");
    });
  }
}

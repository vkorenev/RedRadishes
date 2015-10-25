package redradishes.decoder.parser;

public class ArrayReplyParser<T> extends SuccessOrFailureParser<T> {
  public ArrayReplyParser(Parser<T> parser) {
    super('*', parser);
  }
}

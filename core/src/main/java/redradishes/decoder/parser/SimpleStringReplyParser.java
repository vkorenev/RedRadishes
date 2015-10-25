package redradishes.decoder.parser;

public class SimpleStringReplyParser<T> extends SuccessOrFailureParser<T> {
  public SimpleStringReplyParser(Parser<T> parser) {
    super('+', parser);
  }
}

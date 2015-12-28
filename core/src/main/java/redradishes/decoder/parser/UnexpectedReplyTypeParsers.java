package redradishes.decoder.parser;

import redradishes.decoder.ReplyParseException;

class UnexpectedReplyTypeParsers {
  private static final Appendable NOOP_APPENDABLE = new Appendable() {
    @Override
    public Appendable append(CharSequence csq) {
      return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) {
      return this;
    }

    @Override
    public Appendable append(char c) {
      return this;
    }
  };
  private static final ReplyParser<Appendable> NOOP_SIMPLE_STRING_PARSER =
      new CharAppendingParser<>(() -> NOOP_APPENDABLE);
  private static final Parser<?> NOOP_INTEGER_PARSER = LongParser.PARSER.mapToParser(num -> null);

  private final String unexpectedSimpleStringMeggage;
  private final String unexpectedIntegerMessage;

  UnexpectedReplyTypeParsers(String expectedType) {
    unexpectedSimpleStringMeggage = wrongType("simple string", expectedType);
    unexpectedIntegerMessage = wrongType("integer", expectedType);
  }

  private static String wrongType(String actualTupe, String expectedType) {
    return String.format("Command returned %s reply while %s reply was expected", actualTupe, expectedType);
  }

  <T> ReplyParser<T> simpleStringParser() {
    return NOOP_SIMPLE_STRING_PARSER.fail(value -> new ReplyParseException(unexpectedSimpleStringMeggage));
  }

  <T> ReplyParser<T> integerParser() {
    return NOOP_INTEGER_PARSER.fail(value -> new ReplyParseException(unexpectedIntegerMessage));
  }
}

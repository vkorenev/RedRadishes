package redradishes.decoder.parser;

import redradishes.decoder.ArrayBuilderFactory;
import redradishes.decoder.BulkStringBuilderFactory;
import redradishes.decoder.ReplyParseException;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;

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
  private static final BulkStringBuilderFactory<?, ?> NOOP_BULK_STRING_BUILDER_FACTORY = noopBulkStringBuilderFactory();
  private static final ReplyParser<?> NOOP_BULK_STRING_PARSER =
      new LenParser<>(len -> new BulkStringParser<>(len, NOOP_BULK_STRING_BUILDER_FACTORY));
  private static final ArrayBuilderFactory.Builder<Object, ?> NOOP_ARRAY_BUILDER_FACTORY = noopArrayBuilderFactory();
  private static final ReplyParser<?> NOOP_ARRAY_PARSER = noopArrayParser();
  private static final ReplyParser<Object> NOOP_ANY_REPLY_PARSER =
      new AnyReplyParser<>(NOOP_SIMPLE_STRING_PARSER, NOOP_SIMPLE_STRING_PARSER, NOOP_INTEGER_PARSER,
          NOOP_BULK_STRING_PARSER, NOOP_ARRAY_PARSER);

  private final String unexpectedSimpleStringMeggage;
  private final String unexpectedIntegerMessage;
  private final String unexpectedArrayMessage;

  UnexpectedReplyTypeParsers(String expectedType) {
    unexpectedSimpleStringMeggage = wrongType("simple string", expectedType);
    unexpectedIntegerMessage = wrongType("integer", expectedType);
    unexpectedArrayMessage = wrongType("array", expectedType);
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

  <T> ReplyParser<T> arrayParser() {
    return NOOP_ARRAY_PARSER.fail(value -> new ReplyParseException(unexpectedArrayMessage));
  }

  private static <B, R> BulkStringBuilderFactory<B, R> noopBulkStringBuilderFactory() {
    return new BulkStringBuilderFactory<B, R>() {
      @Override
      public B create(int length, CharsetDecoder charsetDecoder) {
        return null;
      }

      @Override
      public B append(B builder, ByteBuffer buffer, CharsetDecoder charsetDecoder) throws Exception {
        buffer.position(buffer.limit());
        return builder;
      }

      @Override
      public R appendLast(B builder, ByteBuffer buffer, CharsetDecoder charsetDecoder) throws Exception {
        buffer.position(buffer.limit());
        return null;
      }
    };
  }

  private static <E, T> ArrayBuilderFactory.Builder<E, T> noopArrayBuilderFactory() {
    return new ArrayBuilderFactory.Builder<E, T>() {
      @Override
      public void add(@Nullable E element) {

      }

      @Override
      public T build() {
        return null;
      }
    };
  }

  private static ReplyParser<?> noopArrayParser() {
    return new LenParser<>(len -> new ArrayParser<>(len, length -> NOOP_ARRAY_BUILDER_FACTORY, NOOP_ANY_REPLY_PARSER));
  }
}

package redradishes.decoder.parser;

import redradishes.RedisException;

import static redradishes.decoder.parser.CharAppendingParser.CHAR_SEQUENCE_PARSER;

class ErrorParser {
  static <T> ReplyParser<T> errorParser() {
    return CHAR_SEQUENCE_PARSER.fail(message -> new RedisException(message.toString()));
  }
}

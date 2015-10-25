package redradishes;

import java.nio.charset.CharacterCodingException;

public class UncheckedCharacterCodingException extends RuntimeException {
  public UncheckedCharacterCodingException(CharacterCodingException cause) {
    super(cause);
  }
}

package redradishes.encoder;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static redradishes.encoder.RespEncoders.bytesBulkString;
import static redradishes.encoder.RespEncoders.intBulkString;
import static redradishes.encoder.RespEncoders.longBulkString;
import static redradishes.encoder.RespEncoders.strBulkString;

public class Encoders {

  public static Encoder<CharSequence> strArg(Charset charset) {
    return strBulkString(charset);
  }

  public static Encoder<Long> longArg() {
    return longBulkString();
  }

  public static Encoder<Integer> intArg() {
    return intBulkString();
  }

  public static Encoder<byte[]> bytesArg() {
    return bytesBulkString();
  }

  public static Encoder<long[]> longArrayArg() {
    return es -> Arrays.stream(es).mapToObj(e -> longBulkString().encode(e)).reduce(ConstExpr.EMPTY, ConstExpr::append);
  }

  public static Encoder<int[]> intArrayArg() {
    return es -> Arrays.stream(es).mapToObj(e -> intBulkString().encode(e)).reduce(ConstExpr.EMPTY, ConstExpr::append);
  }

  public static <E> Encoder<E[]> arrayArg(Encoder<? super E> elemEncoder) {
    return es -> Arrays.stream(es).map(elemEncoder::encode).reduce(ConstExpr.EMPTY, ConstExpr::append);
  }

  public static <E> Encoder<Collection<? extends E>> collArg(Encoder<? super E> elemEncoder) {
    return es -> es.stream().map(elemEncoder::encode).reduce(ConstExpr.EMPTY, ConstExpr::append);
  }

  public static <K, V> Encoder<Map<? extends K, ? extends V>> mapArg(Encoder<? super K> keyEncoder,
      Encoder<? super V> valueEncoder) {
    return es -> es.entrySet().stream()
        .map(e -> keyEncoder.encode(e.getKey()).append(valueEncoder.encode(e.getValue())))
        .reduce(ConstExpr.EMPTY, ConstExpr::append);
  }
}

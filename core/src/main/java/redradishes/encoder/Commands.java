package redradishes.encoder;

import redradishes.decoder.parser.ReplyParser;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class Commands {
  public static <R> Command<R> define(ReplyParser<? extends R> parser, ConstExpr cmd) {
    return new Command<R>() {
      @Override
      public ReplyParser<? extends R> parser() {
        return parser;
      }

      @Override
      public ConstExpr c() {
        return cmd;
      }
    };
  }

  public static <R> Command<R> define(ReplyParser<? extends R> parser, CharSequence... cmd) {
    return new Command<R>() {
      @Override
      public ReplyParser<? extends R> parser() {
        return parser;
      }

      @Override
      public ConstExpr c() {
        ConstExpr res = ConstExpr.EMPTY;
        for (CharSequence charSequence : cmd) {
          res = res.append(constStr(charSequence));
        }
        return res;
      }
    };
  }

  public static <T1, R> Command1<T1, R> define(ReplyParser<? extends R> parser, CharSequence cmd, Encoder<T1> enc1) {
    Encoder<T1> enc = constStr(cmd).append(enc1);
    return arg1 -> define(parser, enc.encode(arg1));
  }

  public static <T1, T2, R> Command2<T1, T2, R> define(ReplyParser<? extends R> parser, CharSequence cmd,
      Encoder<T1> enc1, Encoder<T2> enc2) {
    Encoder2<T1, T2> enc = constStr(cmd).append(enc1).append(enc2);
    return (arg1, arg2) -> define(parser, enc.encode(arg1, arg2));
  }

  public static <T1, T2, T3, R> Command3<T1, T2, T3, R> define(ReplyParser<? extends R> parser, CharSequence cmd,
      Encoder<T1> enc1, Encoder<T2> enc2, Encoder<T3> enc3) {
    Encoder3<T1, T2, T3> enc = constStr(cmd).append(enc1).append(enc2).append(enc3);
    return (arg1, arg2, arg3) -> define(parser, enc.encode(arg1, arg2, arg3));
  }

  private static ConstExpr constStr(CharSequence cmd) {
    return Encoders.strArg(US_ASCII).encode(cmd);
  }
}

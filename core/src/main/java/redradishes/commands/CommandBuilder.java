package redradishes.commands;

import redradishes.decoder.parser.ReplyParser;
import redradishes.encoder.ConstExpr;
import redradishes.encoder.Encoder;
import redradishes.encoder.Encoder2;
import redradishes.encoder.Encoder3;
import redradishes.encoder.Encoders;

import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class CommandBuilder {
  private static CommandBuilder0 cb0(ConstExpr constExpr) {
    return new CommandBuilder0() {
      @Override
      public <T> CommandBuilder0 withArg(T value, Encoder<? super T> encoder) {
        return cb0(constExpr.append(encoder.encode(value)));
      }

      @Override
      public <T> CommandBuilder1<T> withArg(Encoder<T> encoder) {
        return cb1(constExpr.append(encoder));
      }

      @Override
      public <R> Command<R> returning(ReplyParser<? extends R> parser) {
        return define(parser, constExpr.compact());
      }
    };
  }

  private static <T1> CommandBuilder1<T1> cb1(Encoder<T1> encoder) {
    return new CommandBuilder1<T1>() {
      @Override
      public <T> CommandBuilder1<T1> withArg(T value, Encoder<? super T> enc) {
        return cb1(encoder.append(enc.encode(value)));
      }

      @Override
      public <T> CommandBuilder2<T1, T> withArg(Encoder<T> enc) {
        return cb2(encoder.append(enc));
      }

      @Override
      public <U1 extends T1, R> Command1<U1, R> returning(ReplyParser<? extends R> parser) {
        Encoder<T1> enc = encoder.compact();
        return arg1 -> define(parser, enc.encode(arg1));
      }
    };
  }

  private static <T1, T2> CommandBuilder2<T1, T2> cb2(Encoder2<T1, T2> encoder) {
    return new CommandBuilder2<T1, T2>() {
      @Override
      public <T> CommandBuilder2<T1, T2> withArg(T value, Encoder<? super T> enc) {
        return cb2(encoder.append(enc.encode(value)));
      }

      @Override
      public <T> CommandBuilder3<T1, T2, T> withArg(Encoder<T> enc) {
        return cb3(encoder.append(enc));
      }

      @Override
      public <U1 extends T1, U2 extends T2, R> Command2<U1, U2, R> returning(ReplyParser<? extends R> parser) {
        Encoder2<T1, T2> enc = encoder.compact();
        return (arg1, arg2) -> define(parser, enc.encode(arg1, arg2));
      }
    };
  }

  private static <T1, T2, T3> CommandBuilder3<T1, T2, T3> cb3(Encoder3<T1, T2, T3> encoder) {
    return new CommandBuilder3<T1, T2, T3>() {
      @Override
      public <T> CommandBuilder3<T1, T2, T3> withArg(T value, Encoder<? super T> enc) {
        return cb3(encoder.append(enc.encode(value)));
      }

      @Override
      public <U1 extends T1, U2 extends T2, U3 extends T3, R> Command3<U1, U2, U3, R> returning(
          ReplyParser<? extends R> parser) {
        Encoder3<T1, T2, T3> enc = encoder.compact();
        return (arg1, arg2, arg3) -> define(parser, enc.encode(arg1, arg2, arg3));
      }
    };
  }

  private static <R> Command<R> define(ReplyParser<? extends R> parser, ConstExpr cmd) {
    return new Command<R>() {
      @Override
      public ReplyParser<? extends R> parser() {
        return parser;
      }

      @Override
      public ConstExpr c() {
        return cmd.compact();
      }
    };
  }

  public static CommandBuilder0 command(CharSequence name) {
    return cb0(Encoders.strArg(US_ASCII).encode(name));
  }

  public interface CommandBuilderBase<S extends CommandBuilderBase<S>> {
    <T> S withArg(T value, Encoder<? super T> encoder);

    default S withStrArg(CharSequence s, Charset charset) {
      return withArg(s, Encoders.strArg(charset));
    }

    default S withOption(CharSequence s) {
      return withArg(s, Encoders.strArg(US_ASCII));
    }

    default S withIntArg(Integer value) {
      return withArg(value, Encoders.intArg());
    }

    default S withLongArg(Long value, Charset charset) {
      return withArg(value, Encoders.longArg());
    }
  }

  public interface CommandBuilder0 extends CommandBuilderBase<CommandBuilder0> {
    <T> CommandBuilder1<? super T> withArg(Encoder<T> encoder);

    <R> Command<R> returning(ReplyParser<? extends R> parser);
  }

  public interface CommandBuilder1<T1> extends CommandBuilderBase<CommandBuilder1<T1>> {
    <T> CommandBuilder2<T1, T> withArg(Encoder<T> encoder);

    <U1 extends T1, R> Command1<U1, R> returning(ReplyParser<? extends R> parser);
  }

  public interface CommandBuilder2<T1, T2> extends CommandBuilderBase<CommandBuilder2<T1, T2>> {
    <T> CommandBuilder3<T1, T2, T> withArg(Encoder<T> encoder);

    <U1 extends T1, U2 extends T2, R> Command2<U1, U2, R> returning(ReplyParser<? extends R> parser);
  }

  public interface CommandBuilder3<T1, T2, T3> extends CommandBuilderBase<CommandBuilder3<T1, T2, T3>> {
    <U1 extends T1, U2 extends T2, U3 extends T3, R> Command3<U1, U2, U3, R> returning(ReplyParser<? extends R> parser);
  }
}

package redradishes.encoder;

public interface Command3<T1, T2, T3, R> {
  Command<R> apply(T1 arg1, T2 arg2, T3 arg3);
}

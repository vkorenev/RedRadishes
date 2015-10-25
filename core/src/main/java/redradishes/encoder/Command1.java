package redradishes.encoder;

public interface Command1<T1, R> {
  Command<R> apply(T1 arg1);
}

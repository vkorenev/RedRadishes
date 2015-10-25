package redradishes.decoder;

import javax.annotation.Nullable;

public interface ArrayBuilderFactory<E, T> {
  Builder<E, T> create(int length);

  interface Builder<E, T> {
    void add(@Nullable E element);

    T build();
  }
}

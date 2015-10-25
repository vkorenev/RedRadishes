package redradishes.decoder;

import javax.annotation.Nullable;

public interface MapBuilderFactory<K, V, T> {
  Builder<K, V, T> create(int length);

  interface Builder<K, V, T> {
    void put(@Nullable K key, @Nullable V value);

    T build();
  }
}

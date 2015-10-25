package redradishes.guava;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import redradishes.decoder.ArrayBuilderFactory;
import redradishes.decoder.MapBuilderFactory;

public class CollectionBuilders {
  public static <E> ArrayBuilderFactory<E, ImmutableList<E>> immutableList() {
    return length -> new ArrayBuilderFactory.Builder<E, ImmutableList<E>>() {
      private final ImmutableList.Builder<E> builder = ImmutableList.builder();

      @Override
      public void add(E element) {
        builder.add(element);
      }

      @Override
      public ImmutableList<E> build() {
        return builder.build();
      }
    };
  }

  public static <K, V> MapBuilderFactory<K, V, ImmutableMap<K, V>> immutableMap() {
    return length -> new MapBuilderFactory.Builder<K, V, ImmutableMap<K, V>>() {
      private final ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

      @Override
      public void put(K key, V value) {
        builder.put(key, value);
      }

      @Override
      public ImmutableMap<K, V> build() {
        return builder.build();
      }
    };
  }
}

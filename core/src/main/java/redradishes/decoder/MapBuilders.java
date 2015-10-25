package redradishes.decoder;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.IntFunction;

public class MapBuilders {
  public static <K, V, M extends Map<K, V>> MapBuilderFactory<K, V, M> map(IntFunction<M> mapFactory) {
    return length -> {
      M map = mapFactory.apply(length);
      return new MapBuilderFactory.Builder<K, V, M>() {
        @Override
        public void put(@Nullable K key, @Nullable V value) {
          map.put(key, value);
        }

        @Override
        public M build() {
          return map;
        }
      };
    };
  }
}

package redradishes.decoder;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.IntFunction;

public class ArrayBuilders {
  public static <E> ArrayBuilderFactory<E, E[]> array(IntFunction<E[]> arrayFactory) {
    return length -> {
      E[] array = arrayFactory.apply(length);
      return new ArrayBuilderFactory.Builder<E, E[]>() {
        private int i = 0;

        @Override
        public void add(@Nullable E element) {
          array[i++] = element;
        }

        @Override
        public E[] build() {
          return array;
        }
      };
    };
  }

  public static <E, C extends Collection<E>> ArrayBuilderFactory<E, C> collection(IntFunction<C> collectionFactory) {
    return length -> {
      C collection = collectionFactory.apply(length);
      return new ArrayBuilderFactory.Builder<E, C>() {
        @Override
        public void add(@Nullable E element) {
          collection.add(element);
        }

        @Override
        public C build() {
          return collection;
        }
      };
    };
  }
}

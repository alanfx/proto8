package org.infinispan.api.v8;

import java.util.NoSuchElementException;
import java.util.Optional;

// Using Void returns instead of void in order to avoid, as much as possible,
// the need to add `Consumer` overloaded methods in FunCache
public class EntryView {

   public interface ReadEntryView<K, V> extends MetaParam.Lookup {
      K key();

      /**
       *
       * @throws NoSuchElementException
       */
      V get();

      /**
       * Optional value. It'll return a non-empty value when the value is present,
       * and empty when the value is not present.
       */
      Optional<V> find();
   }

   public interface WriteEntryView<V> {
      /**
       * Set this value.
       *
       * It returns 'Void' instead of 'void' in order to avoid, as much as possible,
       * the need to add `Consumer` overloaded methods in FunctionalCache.
       */
      Void set(V value, MetaParam.Writable... metas);

      /**
       * Removes the value.
       *
       * Instead of creating set(Optional<V>...), add a simple remove() method that
       * removes the value. This feels cleaner and less cumbersome than having to
       * always pass in Optional to set()
       */
      Void remove();
   }

   public interface ReadWriteEntryView<K, V> extends ReadEntryView<K, V>, WriteEntryView<V> {}

}

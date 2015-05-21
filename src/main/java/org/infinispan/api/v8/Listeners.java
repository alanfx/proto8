package org.infinispan.api.v8;

import org.infinispan.api.v8.EntryView.ReadEntryView;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * DESIGN RATIONALE:
 * <ul>The current set of listener events that can be fired are related
 * to modifications, and deciding between create or modify, or providing
 * removed entry information, require reading the previous value. Hence,
 * the current set of listeners can only be provided for read-write maps.
 * In the future, if for example cache entry visited events are to be
 * supported, those would need to be associated with either a read or
 * read-write map, and hence the listeners interface would most likely
 * be split up.</ul>
 */
public interface Listeners {

   interface ReadWriteListeners<K, V> {
      AutoCloseable add(ReadWriteListener<K, V> l);
      AutoCloseable onCreate(Consumer<ReadEntryView<K, V>> f);
      AutoCloseable onModify(BiConsumer<ReadEntryView<K, V>, ReadEntryView<K, V>> f);
      AutoCloseable onRemove(Consumer<ReadEntryView<K, V>> f);

      interface ReadWriteListener<K, V> {
         default void onCreate(ReadEntryView<K, V> created) {}
         default void onModify(ReadEntryView<K, V> before, ReadEntryView<K, V> after) {}
         default void onRemove(ReadEntryView<K, V> removed) {}
      }
   }

   interface WriteListeners<K, V> {
      AutoCloseable add(WriteListener<K, V> l);
      AutoCloseable onWrite(Consumer<ReadEntryView<K, V>> f);

      interface WriteListener<K, V> {
         void onWrite(ReadEntryView<K, V> write);
      }
   }

}

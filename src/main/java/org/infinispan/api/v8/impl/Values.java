package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.EntryView.ReadEntryView;
import org.infinispan.api.v8.EntryView.ReadWriteEntryView;
import org.infinispan.api.v8.EntryView.WriteEntryView;
import org.infinispan.api.v8.MetaParam;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

class Values {

   static <V> ReadEntryView<V> readOnly(InternalEntry<V> entry) {
      return new ReadOnlyValue<>(entry);
   }

   static <K, V> WriteEntryView<V> writeOnly(K key, ConcurrentMap<K, InternalEntry<V>> data) {
      return new WriteOnlyValue<>(key, data);
   }

   static <K, V> ReadWriteEntryView<V> readWrite(K key, ConcurrentMap<K, InternalEntry<V>> data) {
      return new ReadWriteValue<>(key, data);
   }

   private static class ReadOnlyValue<V> implements ReadEntryView<V> {
      final InternalEntry<V> entry;

      private ReadOnlyValue(InternalEntry<V> entry) {
         this.entry = entry;
      }

      @Override
      public Optional<V> get() {
         return entry == null ? Optional.empty() : Optional.ofNullable(entry.value);
      }

      @Override
      public <T> Optional<T> findMetaParam(MetaParam.Id<T> id) {
         return entry.metaParams.find(id);
      }

      @Override
      public <T> T getMetaParam(MetaParam.Id<T> id) {
         return entry.metaParams.get(id);
      }
   }

   private static final class WriteOnlyValue<K, V> implements WriteEntryView<V> {
      final ConcurrentMap<K, InternalEntry<V>> data;
      final K key;

      private WriteOnlyValue(K key, ConcurrentMap<K, InternalEntry<V>> data) {
         this.data = data;
         this.key = key;
      }

      @Override
      public Void set(V value, MetaParam.Writable... metas) {
         MetaParams metaParams = MetaParams.empty();
         metaParams.addMany(metas);
         data.put(key, new InternalEntry<V>(value, metaParams));
         return null;
      }

      @Override
      public Void remove() {
         data.remove(key);
         return null;
      }
   }

   private static final class ReadWriteValue<K, V> implements ReadWriteEntryView<V> {
      final ConcurrentMap<K, InternalEntry<V>> data;
      final K key;

      private ReadWriteValue(K key, ConcurrentMap<K, InternalEntry<V>> data) {
         this.data = data;
         this.key = key;
      }

      @Override
      public Optional<V> get() {
         InternalEntry<V> curr = data.get(key);
         return curr == null ? Optional.<V>empty() : Optional.ofNullable(curr.value);
      }

      @Override
      public Void set(V value, MetaParam.Writable... metas) {
         InternalEntry<V> prev = data.get(key);
         if (prev != null) {
            prev.metaParams.addMany(metas);
            data.put(key, new InternalEntry<V>(value, prev.metaParams));
         } else {
            data.put(key, new InternalEntry<V>(value, MetaParams.of(metas)));
         }
         return null;
      }

      @Override
      public Void remove() {
         data.remove(key);
         return null;
      }

      @Override
      public <T> Optional<T> findMetaParam(MetaParam.Id<T> id) {
         InternalEntry<V> curr = data.get(key);
         return curr.metaParams.find(id);
      }

      @Override
      public <T> T getMetaParam(MetaParam.Id<T> id) {
         InternalEntry<V> curr = data.get(key);
         return curr.metaParams.get(id);
      }
   }

}

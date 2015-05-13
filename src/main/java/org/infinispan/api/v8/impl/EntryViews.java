package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.EntryView.ReadEntryView;
import org.infinispan.api.v8.EntryView.ReadWriteEntryView;
import org.infinispan.api.v8.EntryView.WriteEntryView;
import org.infinispan.api.v8.MetaParam;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

class EntryViews {

   static <K, V> ReadEntryView<K, V> readOnly(K key, InternalValue<V> entry) {
      return new ReadViewImpl<>(key, entry);
   }

   static <K, V> WriteEntryView<V> writeOnly(K key, ConcurrentMap<K, InternalValue<V>> data) {
      return new WriteViewImpl<>(key, data);
   }

   static <K, V> ReadWriteEntryView<K, V> readWrite(K key, ConcurrentMap<K, InternalValue<V>> data) {
      return new ReadWriteViewImpl<>(key, data);
   }

   private static class ReadViewImpl<K, V> implements ReadEntryView<K, V> {
      final K key;
      final InternalValue<V> entry;

      private ReadViewImpl(K key, InternalValue<V> entry) {
         this.entry = entry;
         this.key = key;
      }

      @Override
      public K key() {
         return key;
      }

      @Override
      public Optional<V> find() {
         return entry == null ? Optional.empty() : Optional.ofNullable(entry.value);
      }

      @Override
      public V get() {
         V curr = entry.value;
         if (curr == null)
            throw new NoSuchElementException("No value present");

         return curr;
      }

      @Override
      public <T> T getMetaParam(MetaParam.Id<T> id) {
         return entry.getMetaParam(id);
      }

      @Override
      public <T> Optional<T> findMetaParam(MetaParam.Id<T> id) {
         return entry.findMetaParam(id);
      }
   }

   private static final class WriteViewImpl<K, V> implements WriteEntryView<V> {
      final ConcurrentMap<K, InternalValue<V>> data;
      final K key;

      private WriteViewImpl(K key, ConcurrentMap<K, InternalValue<V>> data) {
         this.data = data;
         this.key = key;
      }

      @Override
      public Void set(V value, MetaParam.Writable... metas) {
         MetaParams metaParams = MetaParams.empty();
         metaParams.addMany(metas);
         data.put(key, new InternalValue<>(value, metaParams));
         return null;
      }

      @Override
      public Void remove() {
         data.remove(key);
         return null;
      }
   }

   private static final class ReadWriteViewImpl<K, V> implements ReadWriteEntryView<K, V> {
      final ConcurrentMap<K, InternalValue<V>> data;
      final K key;

      private ReadWriteViewImpl(K key, ConcurrentMap<K, InternalValue<V>> data) {
         this.data = data;
         this.key = key;
      }

      @Override
      public K key() {
         return key;
      }

      @Override
      public Optional<V> find() {
         InternalValue<V> curr = data.get(key);
         return curr == null ? Optional.empty() : Optional.ofNullable(curr.value);
      }

      @Override
      public Void set(V value, MetaParam.Writable... metas) {
         InternalValue<V> prev = data.get(key);
         if (prev != null) {
            prev.metaParams.addMany(metas);
            data.put(key, new InternalValue<>(value, prev.metaParams));
         } else {
            data.put(key, new InternalValue<>(value, MetaParams.of(metas)));
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
         InternalValue<V> curr = data.get(key);
         return curr.findMetaParam(id);
      }

      @Override
      public <T> T getMetaParam(MetaParam.Id<T> id) {
         InternalValue<V> curr = data.get(key);
         return curr.getMetaParam(id);
      }

      @Override
      public V get() {
         InternalValue<V> curr = data.get(key);
         if (curr == null)
            throw new NoSuchElementException("No value present");

         return curr.value;
      }
   }

}

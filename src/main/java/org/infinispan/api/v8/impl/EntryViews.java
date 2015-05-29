package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.EntryView.ReadEntryView;
import org.infinispan.api.v8.EntryView.ReadWriteEntryView;
import org.infinispan.api.v8.EntryView.WriteEntryView;
import org.infinispan.api.v8.MetaParam;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

final class EntryViews {

   private EntryViews() {
      // Cannot be instantiated, it's just a holder class
   }

   static <K, V> ReadEntryView<K, V> readOnly(K key, InternalEntry<V> entry) {
      return new ReadViewImpl<>(key, entry);
   }

   static <K, V> WriteEntryView<V> writeOnly(K key, AbstractFunctionalMap<K, V> functionalMap) {
      return new WriteViewImpl<>(key, functionalMap.functionalMap.data, functionalMap.functionalMap.notifier);
   }

   static <K, V> ReadWriteEntryView<K, V> readWrite(K key, AbstractFunctionalMap<K, V> functionalMap) {
      return new ReadWriteViewImpl<>(key, functionalMap.functionalMap.data, functionalMap.functionalMap.notifier);
   }

   private static <K, V> ReadEntryView<K, V> noValue(K key) {
      return new NoValueView<>(key);
   }

   private static final class ReadViewImpl<K, V> implements ReadEntryView<K, V> {
      final K key;
      final InternalEntry<V> entry;

      private ReadViewImpl(K key, InternalEntry<V> entry) {
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
      public V get() throws NoSuchElementException {
         if (entry == null || entry.value == null)
            throw new NoSuchElementException("No value present");

         return entry.value;
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
      final ConcurrentMap<K, InternalEntry<V>> data;
      final K key;
      final ListenerNotifier<K, V> notifier;

      private WriteViewImpl(K key, ConcurrentMap<K, InternalEntry<V>> data, ListenerNotifier<K, V> notifier) {
         this.data = data;
         this.key = key;
         this.notifier = notifier;
      }

      @Override
      public Void set(V value, MetaParam.Writable... metas) {
         MetaParams metaParams = MetaParams.empty();
         metaParams.addMany(metas);
         InternalEntry<V> internalEntry = new InternalEntry<>(value, metaParams);
         data.put(key, internalEntry);
         // Data written, no assumptions about previous value can be made,
         // hence we cannot distinguish between create or update.
         notifier.notifyOnWrite(EntryViews.readOnly(key, internalEntry));
         return null;
      }

      @Override
      public Void remove() {
         data.remove(key);
         // For remove write-only listener events, create a value-less read entry view
         notifier.notifyOnWrite(EntryViews.noValue(key));
         return null;
      }
   }

   private static final class ReadWriteViewImpl<K, V> implements ReadWriteEntryView<K, V> {
      final ConcurrentMap<K, InternalEntry<V>> data;
      final K key;
      final ListenerNotifier<K, V> notifier;

      private ReadWriteViewImpl(K key, ConcurrentMap<K, InternalEntry<V>> data, ListenerNotifier<K, V> notifier) {
         this.data = data;
         this.key = key;
         this.notifier = notifier;
      }

      @Override
      public K key() {
         return key;
      }

      @Override
      public Optional<V> find() {
         InternalEntry<V> curr = data.get(key);
         return curr == null ? Optional.empty() : Optional.ofNullable(curr.value);
      }

      @Override
      public Void set(V value, MetaParam.Writable... metas) {
         InternalEntry<V> prev = data.get(key);
         if (prev != null) {
            prev.metaParams.addMany(metas);
            InternalEntry<V> iv = new InternalEntry<>(value, prev.metaParams);
            data.put(key, iv);
            notifier.notifyOnModify(EntryViews.readOnly(key, prev), EntryViews.readOnly(key, iv));
         } else {
            InternalEntry<V> iv = new InternalEntry<>(value, MetaParams.of(metas));
            data.put(key, iv);
            notifier.notifyOnCreate(EntryViews.readOnly(key, iv));
         }
         return null;
      }

      @Override
      public Void remove() {
         InternalEntry<V> prev = data.remove(key);
         notifier.notifyOnRemove(EntryViews.readOnly(key, prev));
         return null;
      }

      @Override
      public <T> Optional<T> findMetaParam(MetaParam.Id<T> id) {
         InternalEntry<V> curr = data.get(key);
         return curr.findMetaParam(id);
      }

      @Override
      public <T> T getMetaParam(MetaParam.Id<T> id) {
         InternalEntry<V> curr = data.get(key);
         return curr.getMetaParam(id);
      }

      @Override
      public V get() throws NoSuchElementException {
         InternalEntry<V> curr = data.get(key);
         if (curr == null)
            throw new NoSuchElementException("No value present");

         return curr.value;
      }
   }

   public static final class NoValueView<K, V> implements ReadEntryView<K, V> {
      final K key;

      public NoValueView(K key) {
         this.key = key;
      }

      @Override
      public K key() {
         return key;
      }

      @Override
      public V get() throws NoSuchElementException {
         throw new NoSuchElementException("No value");
      }

      @Override
      public Optional<V> find() {
         return Optional.empty();
      }

      @Override
      public <T> T getMetaParam(MetaParam.Id<T> id) throws NoSuchElementException {
         throw new NoSuchElementException("No metadata available");
      }

      @Override
      public <T> Optional<T> findMetaParam(MetaParam.Id<T> id) {
         return Optional.empty();
      }
   }

}

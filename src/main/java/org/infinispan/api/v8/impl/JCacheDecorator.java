package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.EntryView.ReadEntryView;
import org.infinispan.api.v8.EntryView.ReadWriteEntryView;
import org.infinispan.api.v8.EntryView.WriteEntryView;
import org.infinispan.api.v8.FunctionalMap;
import org.infinispan.api.v8.Observable;
import org.infinispan.api.v8.Param;
import org.infinispan.api.v8.Status;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import javax.cache.processor.MutableEntry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

public class JCacheDecorator<K, V> implements Cache<K, V> {

   final FunctionalMap.ReadOnlyMap<K, V> readOnly;
   final FunctionalMap.WriteOnlyMap<K, V> writeOnly;
   final FunctionalMap.ReadWriteMap<K, V> readWrite;

   public JCacheDecorator(FunctionalMapImpl<K, V> map) {
      FunctionalMapImpl<K, V> blockingMap = map.withParams(Param.WaitMode.BLOCKING);
      this.readOnly = ReadOnlyMapImpl.create(blockingMap);
      this.writeOnly = WriteOnlyMapImpl.create(blockingMap);
      this.readWrite = ReadWriteMapImpl.create(blockingMap);
   }

   @Override
   public V get(K key) {
      return await(readOnly.eval(key, ro -> ro.find().orElse(null)));
   }

   @Override
   public Map<K, V> getAll(Set<? extends K> keys) {
      Observable<ReadEntryView<K, V>> obs = readOnly.evalMany(keys, ro -> ro);
      Map<K, V> map = new HashMap<>();
      obs.subscribe(ro -> map.put(ro.key(), ro.get())); // Wait mode is BLOCKING, so will block until completed
      return map;
   }

   @Override
   public boolean containsKey(K key) {
      return await(readOnly.eval(key, e -> e.find().isPresent()));
   }

   @Override
   public void put(K key, V value) {
      await(writeOnly.eval(key, value, (v, wo) -> wo.set(v)));
   }

   @Override
   public V getAndPut(K key, V value) {
      return await(readWrite.eval(key, value, (v, rw) -> {
         V prev = rw.find().orElse(null);
         rw.set(v);
         return prev;
      }));
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> map) {
      Observable<Void> obs = writeOnly.evalMany(map, (ev, v) -> v.set(ev));
      obs.subscribe(Observers.noop()); // Wait mode is BLOCKING, so will block until completed
   }

   @Override
   public boolean putIfAbsent(K key, V value) {
      return await(readWrite.eval(key, value, (v, rw) -> {
         Optional<V> opt = rw.find();
         boolean success = !opt.isPresent();
         if (success) rw.set(v);
         return success;
      }));
   }

   @Override
   public boolean remove(K key) {
      return await(readWrite.eval(key, v -> {
         boolean success = v.find().isPresent();
         v.remove();
         return success;
      }));
   }

   @Override
   public boolean remove(K key, V oldValue) {
      return await(readWrite.eval(key, oldValue, (v, rw) -> rw.find().map(prev -> {
         if (prev.equals(v)) {
            rw.remove();
            return true;
         }

         return false;
      }).orElse(false)));
   }

   @Override
   public V getAndRemove(K key) {
      return await(readWrite.eval(key, v -> {
         V prev = v.find().orElse(null);
         v.remove();
         return prev;
      }));
   }

   @Override
   public boolean replace(K key, V oldValue, V newValue) {
      return await(readWrite.eval(key, newValue, (v, rw) -> rw.find().map(prev -> {
         if (prev.equals(oldValue)) {
            rw.set(v);
            return true;
         }
         return false;
      }).orElse(false)));
   }

   @Override
   public boolean replace(K key, V value) {
      return await(readWrite.eval(key, value, (v, rw) -> rw.find().map(prev -> {
         rw.set(v);
         return true;
      }).orElse(false)));
   }

   @Override
   public V getAndReplace(K key, V value) {
      return await(readWrite.eval(key, value, (v, rw) -> rw.find().map(prev -> {
         rw.set(v);
         return prev;
      }).orElse(null)));
   }

   @Override
   public void removeAll(Set<? extends K> keys) {
      Observable<Void> obs = writeOnly.evalMany(keys, WriteEntryView::remove);
      obs.subscribe(Observers.noop()); // Wait mode is BLOCKING, so will block until completed
   }

   @Override
   public void removeAll() {
      Observable<WriteEntryView<V>> values = writeOnly.values();
      values.subscribe(WriteEntryView::remove); // Wait mode is BLOCKING, so will block until completed
   }

   @Override
   public void clear() {
      await(writeOnly.truncate());
   }

   @Override
   public Iterator<Entry<K, V>> iterator() {
      Observable<ReadEntryView<K, V>> entries = readOnly.entries();
      final BlockingQueue<Entry<K, V>> entryEvents = new LinkedBlockingQueue<>();
      // Wait mode is BLOCKING, so subscribe will block until completed
      entries.subscribe(rw -> entryEvents.add(new Entry<K, V>() {
         @Override
         public K getKey() {
            return rw.key();
         }

         @Override
         public V getValue() {
            return rw.get();
         }

         @Override
         public <T> T unwrap(Class<T> clazz) {
            return null;
         }
      }));

      return new Iterator<Entry<K, V>>() {
         @Override
         public boolean hasNext() {
            return entryEvents.peek() != null;
         }

         @Override
         public Entry<K, V> next() {
            try {
               return entryEvents.take();
            } catch (InterruptedException e) {
               throw new AssertionError(e);
            }
         }

         // In Java 8, default remove() implementation is unsupported, but
         // adding support for it would be relatively trivial if following
         // similar solution to the one in ConcurrentMapDecorator
      };
   }

   @Override
   public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments) throws EntryProcessorException {
      return await(readWrite.eval(key, rw ->
         entryProcessor.process(new ReadWriteMutableEntry<>(rw), arguments)));
   }

   private static final class ReadWriteMutableEntry<K, V> implements MutableEntry<K, V> {
      final ReadWriteEntryView<K, V> rw;

      private ReadWriteMutableEntry(ReadWriteEntryView<K, V> rw) {
         this.rw = rw;
      }

      @Override
      public boolean exists() {
         return rw.find().isPresent();
      }

      @Override
      public void remove() {
         rw.remove();
      }

      @Override
      public void setValue(V value) {
         rw.set(value);
      }

      @Override
      public K getKey() {
         return rw.key();
      }

      @Override
      public V getValue() {
         return rw.find().orElse(null);
      }

      @Override
      public <T> T unwrap(Class<T> clazz) {
         return null;  // TODO: Customise this generated block
      }
   }

   @Override
   public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> keys, EntryProcessor<K, V, T> entryProcessor, Object... arguments) {
      Observable<EntryProcessorResultWithKey<K, T>> obs = readWrite.evalMany(keys, rw -> {
            T t = entryProcessor.process(new ReadWriteMutableEntry<>(rw), arguments);
            return new EntryProcessorResultWithKey<>(rw.key(), t);
         }
      );

      Map<K, EntryProcessorResult<T>> map = new HashMap<>();
      obs.subscribe(res -> map.put(res.key, res)); // Wait mode is BLOCKING, so will block until completed
      return map;
   }

   private static final class EntryProcessorResultWithKey<K, T> implements EntryProcessorResult<T> {
      final K key;
      final T t;

      public EntryProcessorResultWithKey(K key, T t) {
         this.key = key;
         this.t = t;
      }

      @Override
      public T get() throws EntryProcessorException {
         return t;
      }
   }

   @Override
   public void close() {
      try {
         readOnly.close();
      } catch (Exception e) {
         throw new AssertionError(e);
      }
   }

   @Override
   public boolean isClosed() {
      return readOnly.getStatus() == Status.STOPPED;
   }

   @Override
   public String getName() {
      return readOnly.getName();
   }

   ////////////////////////////////////////////////////////////////////////////

   @Override
   public void loadAll(Set<? extends K> keys, boolean replaceExistingValues, CompletionListener completionListener) {
      // TODO: Customise this generated block
   }

   @Override
   public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public <T> T unwrap(Class<T> clazz) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public CacheManager getCacheManager() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
      // TODO: Customise this generated block
   }

   @Override
   public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
      // TODO: Customise this generated block
   }

   private static <T> T await(CompletableFuture<T> cf) {
      try {
         return cf.get();
      } catch (InterruptedException | ExecutionException e) {
         throw new Error(e);
      }
   }

}

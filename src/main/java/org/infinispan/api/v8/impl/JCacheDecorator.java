package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.EntryView;
import org.infinispan.api.v8.EntryView.ReadEntryView;
import org.infinispan.api.v8.EntryView.WriteEntryView;
import org.infinispan.api.v8.FunctionalMap;
import org.infinispan.api.v8.Observable;
import org.infinispan.api.v8.Param;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
      // TODO: Customise this generated block
   }

   @Override
   public Iterator<Entry<K, V>> iterator() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments) throws EntryProcessorException {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> keys, EntryProcessor<K, V, T> entryProcessor, Object... arguments) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void loadAll(Set<? extends K> keys, boolean replaceExistingValues, CompletionListener completionListener) {
      // TODO: Customise this generated block
   }

   @Override
   public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public String getName() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public CacheManager getCacheManager() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void close() {
      // TODO: Customise this generated block
   }

   @Override
   public boolean isClosed() {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public <T> T unwrap(Class<T> clazz) {
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

package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.FunctionalMap;
import org.infinispan.api.v8.Param;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class JCacheDecorator<K, V> implements Cache<K, V> {

   final FunctionalMap<K, V> readOnly;

   public JCacheDecorator(FunctionalMap<K, V> map) {
      this.readOnly = map.withParams(Param.WaitMode.BLOCKING);
//      this.writeOnly = map.withParams(Param.AccessMode.WRITE_ONLY, Param.WaitMode.BLOCKING);
//      this.readWrite = map.withParams(Param.AccessMode.READ_WRITE, Param.WaitMode.BLOCKING);
   }

   @Override
   public V get(K key) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public Map<K, V> getAll(Set<? extends K> keys) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public boolean containsKey(K key) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public void loadAll(Set<? extends K> keys, boolean replaceExistingValues, CompletionListener completionListener) {
      // TODO: Customise this generated block
   }

   @Override
   public void put(K key, V value) {
      // TODO: Customise this generated block
   }

   @Override
   public V getAndPut(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> map) {
      // TODO: Customise this generated block
   }

   @Override
   public boolean putIfAbsent(K key, V value) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public boolean remove(K key) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public boolean remove(K key, V oldValue) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public V getAndRemove(K key) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public boolean replace(K key, V oldValue, V newValue) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public boolean replace(K key, V value) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public V getAndReplace(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void removeAll(Set<? extends K> keys) {
      // TODO: Customise this generated block
   }

   @Override
   public void removeAll() {
      // TODO: Customise this generated block
   }

   @Override
   public void clear() {
      // TODO: Customise this generated block
   }

   @Override
   public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz) {
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

   @Override
   public Iterator<Entry<K, V>> iterator() {
      return null;  // TODO: Customise this generated block
   }
}

package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.CloseableIterator;
import org.infinispan.api.v8.FunctionalMap.ReadOnlyMap;
import org.infinispan.api.v8.FunctionalMap.ReadWriteMap;
import org.infinispan.api.v8.FunctionalMap.WriteOnlyMap;
import org.infinispan.api.v8.Param.WaitMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * A {@link ConcurrentMap} implementation that uses the operations exposed by
 * {@link ReadOnlyMap}, {@link WriteOnlyMap} and {@link ReadWriteMap}, and
 * validates their usefulness.
 */
public class ConcurrentMapDecorator<K, V> implements ConcurrentMap<K, V>  {

   final ReadOnlyMap<K, V> readOnly;
   final WriteOnlyMap<K, V> writeOnly;
   final ReadWriteMap<K, V> readWrite;

   // Rudimentary constructor, we'll provide more idiomatic construction
   // via main Infinispan class which is still to be defined
   public ConcurrentMapDecorator(FunctionalMapImpl<K, V> map) {
      FunctionalMapImpl<K, V> blockingMap = map.withParams(WaitMode.BLOCKING);
      this.readOnly = ReadOnlyMapImpl.create(blockingMap);
      this.writeOnly = WriteOnlyMapImpl.create(blockingMap);
      this.readWrite = ReadWriteMapImpl.create(blockingMap);
   }

   @Override
   public int size() {
      return (int) readOnly.keys().count();
   }

   @Override
   public boolean isEmpty() {
      return !readOnly.keys().findAny().isPresent();
   }

   @Override
   public boolean containsKey(Object key) {
      return await(readOnly.eval(toK(key), e -> e.find().isPresent()));
   }

   @Override
   public boolean containsValue(Object value) {
      return readOnly.entries().anyMatch(ro -> ro.get().equals(value));
   }

   @Override
   public V get(Object key) {
      return await(readOnly.eval(toK(key), e -> e.find().orElse(null)));
   }

   @SuppressWarnings("unchecked")
   private K toK(Object key) {
      return (K) key;
   }

   @SuppressWarnings("unchecked")
   private V toV(Object value) {
      return (V) value;
   }

   @Override
   public V put(K key, V value) {
      return await(readWrite.eval(toK(key), value, (v, rw) -> {
         V prev = rw.find().orElse(null);
         rw.set(v);
         return prev;
      }));
   }

   @Override
   public V remove(Object key) {
      return await(readWrite.eval(toK(key), v -> {
         V prev = v.find().orElse(null);
         v.remove();
         return prev;
      }));
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> m) {
      CloseableIterator<Void> it = writeOnly.evalMany(m, (ev, v) -> v.set(ev));
      it.forEachRemaining(aVoid -> {});
   }

   @Override
   public void clear() {
      await(writeOnly.truncate());
   }

   @Override
   public Set<K> keySet() {
      return readOnly.keys().collect(HashSet::new, HashSet::add, HashSet::addAll);
   }

   @Override
   public Collection<V> values() {
      return readOnly.entries().collect(ArrayList::new, (l, v) -> l.add(v.get()), ArrayList::addAll);
   }

   @Override
   public Set<Entry<K, V>> entrySet() {
      return readOnly.entries().collect(HashSet::new, (s, ro) -> s.add(new Entry<K, V>() {
         @Override
         public K getKey() {
            return ro.key();
         }

         @Override
         public V getValue() {
            return ro.get();
         }

         @Override
         public V setValue(V value) {
            V prev = ro.get();
            writeOnly.eval(ro.key(), value, (v, wo) -> wo.set(v));
            return prev;
         }

         @Override
         public boolean equals(Object o) {
            if (o == this)
               return true;
            if (o instanceof Map.Entry) {
               Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
               if (Objects.equals(ro.key(), e.getKey()) &&
                  Objects.equals(ro.get(), e.getValue()))
                  return true;
            }
            return false;
         }

         @Override
         public int hashCode() {
            return ro.hashCode();
         }
      }), HashSet::addAll);
   }

   @Override
   public V putIfAbsent(K key, V value) {
      return await(readWrite.eval(toK(key), value, (v, rw) -> {
         Optional<V> opt = rw.find();
         V prev = opt.orElse(null);
         if (!opt.isPresent())
            rw.set(v);

         return prev;
      }));
   }

   @Override
   public boolean remove(Object key, Object value) {
      return await(readWrite.eval(toK(key), toV(value), (v, rw) -> rw.find().map(prev -> {
         if (prev.equals(value)) {
            rw.remove();
            return true;
         }

         return false;
      }).orElse(false)));
   }

   @Override
   public boolean replace(K key, V oldValue, V newValue) {
      return await(readWrite.eval(toK(key), newValue, (v, rw) -> rw.find().map(prev -> {
         if (prev.equals(oldValue)) {
            rw.set(v);
            return true;
         }
         return false;
      }).orElse(false)));
   }

   @Override
   public V replace(K key, V value) {
      return await(readWrite.eval(toK(key), value, (v, rw) -> rw.find().map(prev -> {
         rw.set(v);
         return prev;
      }).orElse(null)));
   }

   public static <T> T await(CompletableFuture<T> cf) {
      try {
         return cf.get();
      } catch (InterruptedException | ExecutionException e) {
         throw new Error(e);
      }
   }

}

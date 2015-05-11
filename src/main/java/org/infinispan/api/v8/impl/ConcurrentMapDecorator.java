package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.FunctionalMap.ReadOnlyMap;
import org.infinispan.api.v8.FunctionalMap.ReadWriteMap;
import org.infinispan.api.v8.FunctionalMap.WriteOnlyMap;
import org.infinispan.api.v8.Observable;
import org.infinispan.api.v8.Param.StreamMode;
import org.infinispan.api.v8.Param.WaitMode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import static org.infinispan.api.v8.Param.StreamModes.KEYS;
import static org.infinispan.api.v8.Param.StreamModes.VALUES;

public class ConcurrentMapDecorator<K, V> implements ConcurrentMap<K, V>  {

   final ReadOnlyMap<K, V> readOnly;
   final WriteOnlyMap<K, V> writeOnly;
   final ReadWriteMap<K, V> readWrite;

   public ConcurrentMapDecorator(FunctionalMapImpl<K, V> map) {
      FunctionalMapImpl<K, V> blockingMap = map.withParams(WaitMode.BLOCKING);
      this.readOnly = ReadOnlyMapImpl.create(blockingMap);
      this.writeOnly = WriteOnlyMapImpl.create(blockingMap);
      this.readWrite = ReadWriteMapImpl.create(blockingMap);
   }

   @Override
   public int size() {
      return await(readOnly.reduce(0, (p, t) -> t + 1));
   }

   @Override
   public boolean isEmpty() {
      return await(readOnly.findAny(p ->
         p.key().isPresent() ? Optional.of(false) : Optional.empty())
      ).orElse(true);
   }

   @Override
   public boolean containsKey(Object key) {
      return await(readOnly.eval(toK(key), e -> e.get().isPresent()));
   }

   @Override
   public boolean containsValue(Object value) {
      // Finishes early, as soon as the value is found
      // TODO: This lambda captures 'value', so each time it's called it allocates a lambda...
      return await(readOnly.withParams(StreamMode.of(VALUES)).findAny(
            (p) -> p.value().get().equals(value) ? Optional.of(true) : Optional.empty())
      ).isPresent();
   }

   @Override
   public V get(Object key) {
      return await(readOnly.eval(toK(key), e -> e.get().orElse(null)));
   }

   @SuppressWarnings("unchecked")
   private K toK(Object key) {
      return (K) key;
   }

   @Override
   public V put(K key, V value) {
      return await(readWrite.eval(toK(key), value, (v, rw) -> {
         V prev = rw.get().orElse(null);
         rw.set(v);
         return prev;
      }));
   }

   @Override
   public V remove(Object key) {
      return await(readWrite.eval(toK(key), v -> {
         V prev = v.get().orElse(null);
         v.remove();
         return prev;
      }));
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> m) {
      Observable<Void> obs = writeOnly.evalMany(m, (ev, v) -> v.set(ev));

      // evalMany called with BLOCKING hence subscribe will block until completed
      obs.subscribe(Observers.noop());
   }

   @Override
   public void clear() {
      await(writeOnly.truncate());
   }

   @Override
   public Set<K> keySet() {
      return await(readOnly.reduce(new HashSet<>(), (p, set) -> {
         set.add(p.key().get());
         return set;
      }));
   }

   @Override
   public Collection<V> values() {
      return await(readOnly.withParams(StreamMode.of(VALUES)).reduce(new HashSet<>(), (p, set) -> {
         set.add(p.value().get());
         return set;
      }));
   }

   @Override
   public Set<Entry<K, V>> entrySet() {
      return await(readOnly.withParams(StreamMode.of(KEYS, VALUES)).reduce(new HashSet<>(), (p, set) -> {
         set.add(new Map.Entry<K, V>() {
            @Override
            public K getKey() {
               return p.key().get();
            }

            @Override
            public V getValue() {
               return p.value().get();
            }

            @Override
            public V setValue(V value) {
               V prev = p.value().get();
               writeOnly.eval(p.key().get(), v -> v.set(value));
               return prev;
            }

            @Override
            public boolean equals(Object o) {
               if (o == this)
                  return true;
               if (o instanceof Map.Entry) {
                  Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                  if (Objects.equals(p.key().get(), e.getKey()) &&
                     Objects.equals(p.value().get(), e.getValue()))
                     return true;
               }
               return false;
            }

            @Override
            public int hashCode() {
               return p.hashCode();
            }
         });
         return set;
      }));
   }

   @Override
   public V putIfAbsent(K key, V value) {
      // TODO: This lambda captures 'value', so each time it's called it allocates a lambda...
      return await(readWrite.eval(toK(key), v -> {
         V prev = v.get().orElse(null);
         if (!v.get().isPresent())
            v.set(value);

         return prev;
      }));
   }

   @Override
   public boolean remove(Object key, Object value) {
      // TODO: This lambda captures 'value', so each time it's called it allocates a lambda...
      return await(readWrite.eval(toK(key), v -> v.get().map(prev -> {
         if (prev.equals(value)) {
            v.remove();
            return true;
         }

         return false;
      }).orElse(false)));
   }

   @Override
   public boolean replace(K key, V oldValue, V newValue) {
      return await(readWrite.eval(toK(key), v -> v.get().map(prev -> {
         if (prev.equals(oldValue)) {
            v.set(newValue);
            return true;
         }
         return false;
      }).orElse(false)));
   }

   @Override
   public V replace(K key, V value) {
      return await(readWrite.eval(toK(key), v -> v.get().map(prev -> {
         v.set(value);
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

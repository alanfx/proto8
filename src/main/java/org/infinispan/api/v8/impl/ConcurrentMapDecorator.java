package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.FunctionalMap;
import org.infinispan.api.v8.Observable;
import org.infinispan.api.v8.Param;
import org.infinispan.api.v8.Param.AccessMode;
import org.infinispan.api.v8.Param.StreamMode;
import org.infinispan.api.v8.Param.StreamModes;
import org.infinispan.api.v8.Param.WaitMode;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.infinispan.api.v8.Param.StreamModes.*;

public class ConcurrentMapDecorator<K, V> implements ConcurrentMap<K, V>  {

   final FunctionalMap<K, V> readOnly;
   final FunctionalMap<K, V> writeOnly;
   final FunctionalMap<K, V> readWrite;

   public ConcurrentMapDecorator(FunctionalMap<K, V> map) {
      this.readOnly = map.withParams(WaitMode.BLOCKING);
      this.writeOnly = map.withParams(AccessMode.WRITE_ONLY, WaitMode.BLOCKING);
      this.readWrite = map.withParams(AccessMode.READ_WRITE, WaitMode.BLOCKING);
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
      // TODO: This lambda captures 'value', so each time it's called it allocates a lambda...
      return await(readWrite.eval(toK(key), v -> {
         V prev = v.get().orElse(null);
         v.set(value);
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
      return null;  // TODO: Customise this generated block
   }

   @Override
   public Collection<V> values() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public Set<Entry<K, V>> entrySet() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public V putIfAbsent(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public boolean remove(Object key, Object value) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public boolean replace(K key, V oldValue, V newValue) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public V replace(K key, V value) {
      return null;  // TODO: Customise this generated block
   }

//   @Override
//   public Set<K> keySet() {
//      return await(map.fold(Mode.StreamMode.KEYS_ONLY, new HashSet<>(), (p, set) -> {
//         set.add(p.key().get());
//         return set;
//      }));
//   }
//
//   @Override
//   public Collection<V> values() {
//      return await(map.fold(Mode.StreamMode.VALUES_ONLY, new HashSet<>(), (p, set) -> {
//         set.add(p.value().get());
//         return set;
//      }));
//   }
//
//   @Override
//   public Set<Entry<K, V>> entrySet() {
//      return await(map.fold(Mode.StreamMode.KEYS_AND_VALUES, new HashSet<>(), (p, set) -> {
//         set.add(new Entry<K, V>() {
//            @Override
//            public K getKey() {
//               return p.key().get();
//            }
//
//            @Override
//            public V getValue() {
//               return p.value().get();
//            }
//
//            @Override
//            public V setValue(V value) {
//               V prev = p.value().get();
//               map.eval(p.key().get(), WRITE_ONLY, v -> v.set(value));
//               return prev;
//            }
//
//            @Override
//            public boolean equals(Object o) {
//               if (o == this)
//                  return true;
//               if (o instanceof Entry) {
//                  Entry<?, ?> e = (Entry<?, ?>) o;
//                  if (Objects.equals(p.key().get(), e.getKey()) &&
//                     Objects.equals(p.value().get(), e.getValue()))
//                     return true;
//               }
//               return false;
//            }
//
//            @Override
//            public int hashCode() {
//               return p.hashCode();
//            }
//         });
//         return set;
//      }));
//   }

   public static <T> T await(CompletableFuture<T> cf) {
      try {
         return cf.get();
      } catch (InterruptedException | ExecutionException e) {
         throw new Error(e);
      }
   }

}

package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.FunctionalMap;
import org.infinispan.api.v8.Mode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MapDecorator<K, V>
//   implements Map<K, V>
{

//   final FunctionalMap<K, V> map;
//
//   public MapDecorator(FunctionalMap<K, V> map) {
//      this.map = map;
//   }
//
//   @Override
//   public int size() {
//      // FIXME: Could be more efficient with a potential StreamMode.NONE
//      return await(map.fold(Mode.StreamMode.KEYS_ONLY, 0, (p, t) -> t + 1));
//   }
//
//   @Override
//   public boolean isEmpty() {
//      // Finishes early, as soon as an entry is found
//      return !await(map.search(Mode.StreamMode.VALUES_ONLY,
//            (p) -> p.value().isPresent() ? true : null)
//      ).isPresent();
//   }
//
//   @Override
//   public boolean containsKey(Object key) {
//      return await(map.eval(toK(key), READ_ONLY, e -> e.get().isPresent()));
//   }
//
//   @Override
//   public boolean containsValue(Object value) {
//      // Finishes early, as soon as the value is found
//      return await(map.search(Mode.StreamMode.VALUES_ONLY,
//            (p) -> p.value().get().equals(value) ? true : null)
//      ).isPresent();
//   }
//
//   @Override
//   public V get(Object key) {
//      return await(map.eval(toK(key), READ_ONLY, e -> e.get().orElse(null)));
//   }
//
//   @SuppressWarnings("unchecked")
//   private K toK(Object key) {
//      return (K) key;
//   }
//
//   @Override
//   public V put(K key, V value) {
//      return await(map.eval(toK(key), READ_WRITE, v -> {
//         V prev = v.get().orElse(null);
//         v.set(value);
//         return prev;
//      }));
//   }
//
//   @Override
//   public V remove(Object key) {
//      return await(map.eval(toK(key), READ_WRITE, v -> {
//         V prev = v.get().orElse(null);
//         v.remove();
//         return prev;
//      }));
//   }
//
//   @Override
//   public void putAll(Map<? extends K, ? extends V> m) {
//      Map<K, CompletableFuture<Object>> futures = map.evalAll(m, WRITE_ONLY, (x, v) -> {
//         v.set(x);
//         return null;
//      });
//
//      // Wait for all futures to complete
//      await(CompletableFuture.allOf(
//         futures.values().toArray(new CompletableFuture[futures.size()])));
//   }
//
//   @Override
//   public void clear() {
//      await(map.truncate());
//   }
//
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
//
//   public static <T> T await(CompletableFuture<T> cf) {
//      try {
//         // FIXME: Should be timed...
//         return cf.get();
//      } catch (InterruptedException | ExecutionException e) {
//         throw new Error(e);
//      }
//   }

}

package org.infinispan.api.v8;

import org.infinispan.api.v8.EntryView.ReadEntryView;
import org.infinispan.api.v8.EntryView.ReadWriteEntryView;
import org.infinispan.api.v8.EntryView.WriteEntryView;
import org.infinispan.api.v8.util.Tuple;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface FunctionalMap<K, V> extends AutoCloseable {

   /**
    *
    */
   FunctionalMap<K, V> withParams(Param<?>... ps);

   interface ReadOnlyMap<K, V> extends FunctionalMap<K, V> {
      ReadOnlyMap<K, V> withParams(Param<?>... ps);

      /**
       * Evaluate a read-only function on the value associated with the key.
       *
       * This method can be used to implement read-only single-key based operations in
       * {@link ConcurrentMap} and {@link javax.cache.Cache} such as:
       *
       * <ul>
       * <li>{@link ConcurrentMap#get(Object)}</li>
       * <li>{@link ConcurrentMap#containsKey(Object)}</li>
       * <li>{@link javax.cache.Cache#get(Object)}</li>
       * <li>{@link javax.cache.Cache#containsKey(Object)}</li>
       * </ul>
       */
      <R> CompletableFuture<R> eval(K key, Function<ReadEntryView<K, V>, R> f);

      /**
       * Evaluate a function on the values associated with the subset of keys passed in.
       *
       * This method can be used to implement operations such as
       * {@link javax.cache.Cache#getAll(Set)}.
       */
      <R> Observable<R> evalMany(Collection<? extends K> s, Function<ReadEntryView<K, V>, R> f);

      /**
       * {@link ConcurrentMap#size()},
       * {@link ConcurrentMap#keySet()},
       * {@link ConcurrentMap#isEmpty()},
       */
      Observable<K> keys();

      /**
       * {@link ConcurrentMap#containsValue(Object)},
       * {@link ConcurrentMap#values()},
       */
      Observable<Value<V>> values();

      /**
       * {@link ConcurrentMap#entrySet()},
       */
      Observable<Tuple<K, Value<V>>> entries();

//      /**
//       * This method can be used to implement {@link ConcurrentMap#isEmpty()}.
//       */
//      <R> CompletableFuture<Optional<R>> findKeys(Function<K, Optional<R>> f);
//
//      /**
//       * This method can be used to implement {@link ConcurrentMap#containsValue(Object)}.
//       */
//      <R> CompletableFuture<Optional<R>> findValues(Function<V, Optional<R>> f);
//
//      /**
//       * This method can be used to implement {@link ConcurrentMap#size()},
//       * {@link ConcurrentMap#keySet()}.
//       */
//      <R> CompletableFuture<R> reduceKeys(R z, BiFunction<K, R, R> f);
//
//      /**
//       * This method can be used to implement...
//       */
//      <R> CompletableFuture<R> reduceValues(R z, BiFunction<V, R, R> f);
   }

   interface WriteOnlyMap<K, V> extends FunctionalMap<K, V> {
      WriteOnlyMap<K, V> withParams(Param<?>... ps);

      /**
       * Evaluate a write-only function on the value associated with the key.
       *
       * This method can be used to implement single-key write-only operations
       * such as:
       *
       * <ul>
       * <li>{@link javax.cache.Cache#put(Object, Object)}</li>
       * <li></li>
       * <li></li>
       * </ul>
       */
      <R> CompletableFuture<R> eval(K key, V value, BiFunction<V, WriteEntryView<V>, R> f);

      <R> CompletableFuture<R> eval(K key, Function<WriteEntryView<V>, R> f);

      /**
       * Evaluate a function... TODO...
       *
       * This method can be used to implement operations such as:
       *
       * <ul>
       * <li>{@link ConcurrentMap#putAll(Map)}</li>
       * <li>{@link javax.cache.Cache#putAll(Map)}</li>
       * </ul>
       */
      <R> Observable<R> evalMany(Map<? extends K, ? extends V> m, BiFunction<V, WriteEntryView<V>, R> f);

      /**
       * This method can be used to implement {@link ConcurrentMap#clear()}.
       */
      CompletableFuture<Void> truncate();
   }

   interface ReadWriteMap<K, V> extends FunctionalMap<K, V> {
      ReadWriteMap<K, V> withParams(Param<?>... ps);

      /**
       * This method can be used to implement single-key read-write operations
       * in {@link ConcurrentMap} and {@link javax.cache.Cache} that do not
       * depend on value information given by the user such as:
       *
       * <ul>
       * <li>{@link ConcurrentMap#remove(Object)}</li>
       * <li>{@link javax.cache.Cache#remove(Object)}</li>
       * <li>{@link javax.cache.Cache#getAndRemove(Object)}</li>
       * </ul>
       */
      <R> CompletableFuture<R> eval(K key, Function<ReadWriteEntryView<K, V>, R> f);

      /**
       * Evaluate a read-write function on the value associated with the key,
       * with the capability to both update the value and metadata associated
       * with that key, and return previous value or metadata.
       *
       * This method can be used to implement the vast majority of single-key
       * read-write operations in {@link ConcurrentMap} and {@link javax.cache.Cache}
       * such as:
       *
       * <ul>
       * <li>{@link ConcurrentMap#put(Object, Object)}</li>
       * <li>{@link ConcurrentMap#putIfAbsent(Object, Object)}</li>
       * <li>{@link ConcurrentMap#replace(Object, Object)}</li>
       * <li>{@link ConcurrentMap#replace(Object, Object, Object)}</li>
       * <li>{@link ConcurrentMap#remove(Object, Object)}</li>
       * <li>{@link javax.cache.Cache#getAndPut(Object, Object)}</li>
       * <li>{@link javax.cache.Cache#putIfAbsent(Object, Object)}</li>
       * <li>{@link javax.cache.Cache#remove(Object, Object)}</li>
       * <li>{@link javax.cache.Cache#replace(Object, Object, Object)}</li>
       * <li>{@link javax.cache.Cache#replace(Object, Object)}</li>
       * <li>{@link javax.cache.Cache#getAndReplace(Object, Object)}</li>
       * </ul>
       */
      <R> CompletableFuture<R> eval(K key, V value, BiFunction<V, ReadWriteEntryView<K, V>, R> f);
   }

}

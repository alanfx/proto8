package org.infinispan.api.v8;

import org.infinispan.api.v8.EntryView.ReadEntryView;
import org.infinispan.api.v8.EntryView.ReadWriteEntryView;
import org.infinispan.api.v8.EntryView.WriteEntryView;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface FunctionalMap<K, V> extends AutoCloseable {

   // TODO: Consider adding filter and/or filterKeys, filterValues...

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
       * {@link ConcurrentMap} and {@link javax.cache.Cache} such as
       * {@link ConcurrentMap#get(Object)} and {@link javax.cache.Cache#get(Object)}.
       */
      <R> CompletableFuture<R> eval(K key, Function<ReadEntryView<V>, R> f);

      /**
       * Evaluate a function on the values associated with the subset of keys passed in.
       *
       * This method can be used to implement operations such as
       * {@link javax.cache.Cache#getAll(Set)}.
       */
      <R> Observable<R> evalMany(Collection<? extends K> s, Function<ReadEntryView<V>, R> f);

      /**
       * Attempts to find an element, or a part of it, in the underlying collection,
       * applying the given function to each element in the collection.
       *
       * When the element is found, the function should return a non-empty
       * {@link Optional} containing the found element. If the element has not
       * been found, the function should return an empty {@link Optional} which
       * results in the search continuing.
       *
       * By default, only keys are searched, meaning that {@link Pair#key()} is
       * guaranteed to return a non-empty {@link Optional}. Values can also be
       * searched by passing in a parameter containing {@link Param.StreamModes#VALUES}.
       *
       * Returns an asynchronous {@link Optional} describing the found element, or
       * an empty asynchronous {@code Optional} no element was found.
       *
       * This method can be used to implement {@link ConcurrentMap} operations such as
       * {@link ConcurrentMap#isEmpty()} and {@link ConcurrentMap#containsValue(Object)}.
       */
      <R> CompletableFuture<Optional<R>> findAny(Function<Pair<K, V>, Optional<R>> f);

      /**
       * TODO...
       */
      <R> CompletableFuture<R> reduce(R z, BiFunction<Pair<K, V>, R, R> f);
   }

   interface WriteOnlyMap<K, V> extends FunctionalMap<K, V> {
      WriteOnlyMap<K, V> withParams(Param<?>... ps);

      /**
       * Evaluate a write-only function on the value associated with the key.
       *
       * This method can be used to implement the vast majority of single-key
       * based operations in {@link ConcurrentMap} such as
       * {@link ConcurrentMap#put(Object, Object)}, {@link ConcurrentMap#get(Object)},
       * {@link ConcurrentMap#remove(Object)}, {@link ConcurrentMap#putIfAbsent(Object, Object)},
       * {@link ConcurrentMap#replace(Object, Object)}...etc.
       */
      <R> CompletableFuture<R> eval(K key, V value, BiFunction<V, WriteEntryView<V>, R> f);

      <R> CompletableFuture<R> eval(K key, Function<WriteEntryView<V>, R> f);

      /**
       * Evaluate a function... TODO...
       *
       * This method can be used to implement operations such as
       * {@link ConcurrentMap#putAll(Map)}.
       */
      <R> Observable<R> evalMany(Map<? extends K, ? extends V> m, BiFunction<V, WriteEntryView<V>, R> f);

      /**
       * Truncate
       */
      CompletableFuture<Void> truncate();
   }

   interface ReadWriteMap<K, V> extends FunctionalMap<K, V> {
      ReadWriteMap<K, V> withParams(Param<?>... ps);

      /**
       * {@link ConcurrentMap#remove(Object)}
       */
      <R> CompletableFuture<R> eval(K key, Function<ReadWriteEntryView<V>, R> f);

      /**
       * Evaluate a read-write function on the value associated with the key,
       * with the capability to both update the value and metadata associated
       * with that key, and return previous value or metadata.
       *
       * This method can be used to implement the vast majority of single-key
       * read-write operations in {@link ConcurrentMap} such as
       * {@link ConcurrentMap#put(Object, Object)},
       * {@link ConcurrentMap#putIfAbsent(Object, Object)},
       * {@link ConcurrentMap#replace(Object, Object)}...etc.
       */
      <R> CompletableFuture<R> eval(K key, V value, BiFunction<V, ReadWriteEntryView<V>, R> f);
   }

}

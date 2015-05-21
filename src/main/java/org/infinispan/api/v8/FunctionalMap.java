package org.infinispan.api.v8;

import org.infinispan.api.v8.EntryView.ReadEntryView;
import org.infinispan.api.v8.EntryView.ReadWriteEntryView;
import org.infinispan.api.v8.EntryView.WriteEntryView;
import org.infinispan.api.v8.Listeners.ReadWriteListeners;
import org.infinispan.api.v8.Listeners.WriteListeners;

import javax.cache.processor.EntryProcessor;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface FunctionalMap<K, V> extends AutoCloseable {

   /**
    *
    */
   FunctionalMap<K, V> withParams(Param<?>... ps);

   String getName();

   Status getStatus();

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
       *
       * DESIGN RATIONALE: It makes sense to expose global operation like this
       * instead of forcing users to iterate over the keys to lookup and call
       * get individually since Infinispan can do things more efficiently.
       */
      <R> Observable<R> evalMany(Set<? extends K> s, Function<ReadEntryView<K, V>, R> f);

      /**
       * {@link ConcurrentMap#size()},
       * {@link ConcurrentMap#keySet()},
       * {@link ConcurrentMap#isEmpty()},
       */
      Observable<K> keys();

      /**
       * {@link ConcurrentMap#containsValue(Object)},
       * {@link ConcurrentMap#values()},
       * {@link ConcurrentMap#entrySet()},
       * {@link javax.cache.Cache#iterator()},
       */
      Observable<ReadEntryView<K, V>> entries();

      // DESIGN RATIONALE: Why no values() method?
      // Having keys() makes sense since that way we can have an observe all
      // keys without having to bring values. Having entries() makes sense
      // since it allows you to observe on both keys and values, but this is
      // no extra cost to exposing just values since keys are the main index
      // and hence will always be available. Hence, adding values() offers
      // nothing extra to the API.
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
       *
       * DESIGN RATIONALES:
       * <ul>
       * <li>Since this is a write-only operation, no entry attributes can be
       * queried, hence the only reasonable thing can be returned is Void.</li>
       * </ul>
       */
      CompletableFuture<Void> eval(K key, V value, BiConsumer<V, WriteEntryView<V>> f);

      /**
       * DESIGN RATIONALES:
       * <ul>
       * <li>Since this is a write-only operation, no entry attributes can be
       * queried, hence the only reasonable thing can be returned is Void.</li>
       * </ul>
       */
      CompletableFuture<Void> eval(K key, Consumer<WriteEntryView<V>> f);

      /**
       * Evaluate a function... TODO...
       *
       * This method can be used to implement operations such as:
       *
       * <ul>
       * <li>{@link ConcurrentMap#putAll(Map)}</li>
       * <li>{@link javax.cache.Cache#putAll(Map)}</li>
       * </ul>
       *
       * DESIGN RATIONALE:
       * <ul>
       * <li>It makes sense to expose global operation like this
       * instead of forcing users to iterate over the keys to lookup and call
       * get individually since Infinispan can do things more efficiently.</li>
       * <li>Since this is a write-only operation, no entry attributes can be
       * queried, hence the only reasonable thing can be returned is Void.</li>
       * <li>It still makes sense to return Observable<Void> instead of
       * CompletableFuture<Void> in case the user wants to do something as each
       * entry gets consumed, e.g. keep track of progress.</li>
       * </ul>
       */
      Observable<Void> evalMany(Map<? extends K, ? extends V> entries, BiConsumer<V, WriteEntryView<V>> f);

      /**
       * This method can be used to implement operations such as
       * {@link javax.cache.Cache#removeAll(Set)}.
       *
       * DESIGN RATIONALE:
       * <ul>
       * <li>It makes sense to expose global operation like this
       * instead of forcing users to iterate over the keys to lookup and call
       * get individually since Infinispan can do things more efficiently.</li>
       * <li>Since this is a write-only operation, no entry attributes can be
       * queried, hence the only reasonable thing can be returned is Void.</li>
       * <li>It still makes sense to return Observable<Void> instead of
       * CompletableFuture<Void> in case the user wants to do something as each
       * entry gets consumed, e.g. keep track of progress.</li>
       * </ul>
       */
      Observable<Void> evalMany(Set<? extends K> m, Consumer<WriteEntryView<V>> f);

      /**
       * This method can be used to implement operations such as
       * {@link javax.cache.Cache#removeAll()}.
       */
      Observable<WriteEntryView<V>> values();

      /**
       * This method can be used to implement:
       *
       * <ul>
       * <li>{@link ConcurrentMap#clear()}</li>
       * <li>{@link javax.cache.Cache#clear()}</li>
       * </ul>
       */
      CompletableFuture<Void> truncate();

      WriteListeners<K, V> listeners();
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
       * <li>{@link javax.cache.Cache#invoke(Object, EntryProcessor, Object...)}</li>
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

      /**
       * TODO: Put a set of keys returning previous values/metaparams
       */
      <R> Observable<R> evalMany(Map<? extends K, ? extends V> m, BiFunction<V, ReadWriteEntryView<K, V>, R> f);

      /**
       * TODO: Remove a set of keys returning previous values/metaparams
       *
       * {@link javax.cache.Cache#invokeAll(Set, EntryProcessor, Object...)}
       */
      <R> Observable<R> evalMany(Set<? extends K> keys, Function<ReadWriteEntryView<K, V>, R> f);

      /**
       * TODO: Remove all cached entries individually returning previous values/metaparams
       */
      Observable<ReadWriteEntryView<K, V>> entries();

      /**
       *
       */
      ReadWriteListeners<K, V> listeners();
   }

}

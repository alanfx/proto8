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

/**
 * Top level functional map interface offering common functionality for the
 * read-only, read-write, and write-only operations that can be run against a
 * functional map.
 *
 * DESIGN RATIONALES:
 * <ul>
 *    <li>Originally, I tried to come up with a single functional map interface
 *    that would encompass read-only, read-write and write-only operations, but
 *    it felt quite bloated. By separating each major type of operations to its
 *    own interface, it becomes easier to figure out which are all the read-only
 *    operations, which are write-only...etc, which also helps the user quickly
 *    see what they can do without being obstructed by other operation types.
 *    </li>
 *    <li>In the original design, we defined custom functional interfaces that
 *    were serializable. By doing this, we could ship them around and apply
 *    them remotely. However, for a function to be serializable, it can't
 *    capture non-serializable objects which these days can only be detected
 *    at runtime, hence it's not very typesafe. On top of that, using foreign
 *    function definitions would have made the code harder to read. For all
 *    these reasons, we've gone for the approach of using standard lambda
 *    functions. When these have to run in a clustered environment, instead of
 *    shipping the lambda function around, necessary elements are brought to
 *    the node where the function is passed, and the functions gets executed
 *    locally. This way of working has also the benefit of matching how
 *    Infinispan works internally, bringing necessary elements from other
 *    nodes, executing operations locally and shipping results around. In the
 *    future, we might decide to make these functions marshallable, but
 *    outside the standard</li>
 * </ul>
 */
public interface FunctionalMap<K, V> extends AutoCloseable {

   /**
    * Tweak functional map executions providing {@link Param} instances.
    */
   FunctionalMap<K, V> withParams(Param<?>... ps);

   /**
    * Functional map's name.
    */
   String getName();

   /**
    * Functional map's status.
    */
   Status getStatus();

   /**
    * Exposes read-only operations that can be executed against the functional map.
    * The information that can be read per entry in the functional map is
    * exposed by {@link ReadEntryView}.
    *
    * DESIGN RATIONALES:
    * <ul>
    *    <li>Why does it make sense to expose read-only operations?
    *    Because read-only operations don't acquired locks, and hence all sorts
    *    of optimizations can be carried out by the internal logic.
    *    </li>
    *    <li>Why no values() method? Having keys() makes sense since that way
    *    we can have an observe all keys without having to bring values.
    *    Having entries() makes sense since it allows you to observe on both
    *    keys and values, but this is no extra cost to exposing just values
    *    since keys are the main index and hence will always be available.
    *    Hence, adding values() offers nothing extra to the API.
    *    </li>
    * </ul>
    */
   interface ReadOnlyMap<K, V> extends FunctionalMap<K, V> {
      /**
       * Tweak read-only functional map executions providing {@link Param} instances.
       */
      ReadOnlyMap<K, V> withParams(Param<?>... ps);

      /**
       * Evaluate a read-only function on the value associated with the key
       * and return a {@link CompletableFuture} with the return type of the function.
       * If the user is not sure if the key is present, {@link ReadEntryView#find()}
       * can be used to find out for sure. Typically, function implementations
       * would return value or {@link MetaParam} information from the cache
       * entry in the functional map.
       *
       * By returning {@link CompletableFuture} instead of the function's
       * return type directly, the method hints at the possibility that to
       * executing the function might require remote data present in either
       * a persistent store or a remote clustered node.
       *
       * This method can be used to implement read-only single-key based
       * operations in {@link ConcurrentMap} and {@link javax.cache.Cache}
       * such as:
       *
       * <ul>
       *    <li>{@link ConcurrentMap#get(Object)}</li>
       *    <li>{@link ConcurrentMap#containsKey(Object)}</li>
       *    <li>{@link javax.cache.Cache#get(Object)}</li>
       *    <li>{@link javax.cache.Cache#containsKey(Object)}</li>
       * </ul>
       *
       * @param key the key associated with the {@link ReadEntryView} to be
       *            passed to the function.
       * @param f function that takes a {@link ReadEntryView} associated with
       *          the key, and returns a value.
       * @param <R> function return type
       * @return a {@link CompletableFuture} which will be completed with the
       *         returned value from the function
       */
      <R> CompletableFuture<R> eval(K key, Function<ReadEntryView<K, V>, R> f);

      /**
       * Evaluate a function on a key and potential value associated in
       * the functional map, for each of the keys in the set passed in, and
       * returns an {@link Observable} to which the user can subscribe get
       * asynchronous callbacks as each function's result is computed.
       *
       * The function passed in will be executed for as many keys
       * present in keys collection set. Similar to {@link #eval(Object, Function)},
       * if the user is not sure whether a particular key is present,
       * {@link ReadEntryView#find()} can be used to find out for sure.
       *
       * This method can be used to implement operations such as
       * {@link javax.cache.Cache#getAll(Set)}.
       *
       * DESIGN RATIONALE:
       * <ul>
       *    <li>It makes sense to expose global operation like this instead of
       *    forcing users to iterate over the keys to lookup and call get
       *    individually since Infinispan can do things more efficiently.
       *    </li>
       * </ul>
       *
       * @param keys the keys associated with each of the {@link ReadEntryView}
       *             passed in the function callbacks
       * @param f function that takes a {@link ReadEntryView} associated with
       *          the key, and returns a value. It'll be invoked once for each key
       *          passed in
       * @param <R> function return type
       * @return an {@link Observable} who will emit an element for
       *         each function return value
       */
      <R> Observable<R> evalMany(Set<? extends K> keys, Function<ReadEntryView<K, V>, R> f);

      /**
       * Provides an Observable to which subscribers can be registered to work
       * on all the cached keys.
       *
       * This method can be used to implement operations such as:
       * <ul>
       *    <li>{@link ConcurrentMap#size()}</li>
       *    <li>{@link ConcurrentMap#keySet()}</li>
       *    <li>{@link ConcurrentMap#isEmpty()}</li>
       * </ul>
       *
       * @return an {@link Observable} who will emit an element for each cached key
       */
      Observable<K> keys();

      /**
       * Provides an Observable to which subscribers can be registered to work
       * on all the cached entries.
       *
       * This method can be used to implement operations such as:
       * <ul>
       *    <li>{@link ConcurrentMap#containsValue(Object)}</li>
       *    <li>{@link ConcurrentMap#values()}</li>
       *    <li>{@link ConcurrentMap#entrySet()}</li>
       *    <li>{@link javax.cache.Cache#iterator()}</li>
       * </ul>
       *
       * @return an {@link Observable} who will emit an element for each cached entry
       */
      Observable<ReadEntryView<K, V>> entries();
   }

   /**
    * Exposes write-only operations that can be executed against the functional map.
    * The write operations that can be applied per entry are exposed by
    * {@link WriteEntryView}.
    *
    * DESIGN RATIONALES:
    * <ul>
    *    <li>Why does it make sense to expose write-only operations?
    *    Because write-only operations do not need to read. In other words,
    *    since write-only operations do not read or query the previous value,
    *    these read/query operations, which sometimes can be expensive since
    *    they involve talking to a remote node in the cluster or the
    *    persistence layer, can be avoided optimising write-only operations.
    *    </li>
    * </ul>
    */
   interface WriteOnlyMap<K, V> extends FunctionalMap<K, V> {
      /**
       * Tweak write-only functional map executions providing {@link Param} instances.
       */
      WriteOnlyMap<K, V> withParams(Param<?>... ps);

      /**
       * Evaluate a write-only {@link BiConsumer} operation, with a value
       * passed in and a {@link WriteEntryView} of the value associated with
       * the key, and return a {@link CompletableFuture} which will be
       * completed when the operation completes.
       *
       * By returning {@link CompletableFuture} instead of the function's
       * return type directly, the method hints at the possibility that to
       * executing the function might require remote data present in either
       * a persistent store or a remote clustered node.
       *
       * This method can be used to implement single-key write-only operations
       * which do not need to query previous value, such as:
       *
       * <ul>
       * <li>{@link javax.cache.Cache#put(Object, Object)}</li>
       * </ul>
       *
       * DESIGN RATIONALES:
       * <ul>
       *    <li>Since this is a write-only operation, no entry attributes can be
       *    queried, hence the only reasonable thing can be returned is Void.
       *    </li>
       *    <li>Why provide this operation? Isn't {@link #eval(Object, Consumer)} enough?
       *    The functionality provided by this function could indeed be implemented
       *    with {@link #eval(Object, Consumer)}, but there's a crucial difference.
       *    If you want to store a value and reference the value to be stored
       *    from the passed in operation, {@link #eval(Object, Consumer)} needs
       *    to capture that value. Capturing means that each time the operation
       *    is called, a new lambda needs to be instantiated. By offering a
       *    {@link BiConsumer} that takes user provided value as first parameter,
       *    the operation does not capture any external objects when implementing
       *    simple operations such as {@link javax.cache.Cache#put(Object, Object)},
       *    and hence, the {@link BiConsumer} could be cached and reused each
       *    time it's invoked.
       *    </li>
       * </ul>
       *
       * @param key the key associated with the {@link WriteEntryView} to be
       *            passed to the operation
       * @param value value to write, passed in as first parameter to the
       *              {@link BiConsumer} operation.
       * @param f operation that takes a user defined value, and a
       *          {@link WriteEntryView} associated with the key, and writes
       *          to the {@link WriteEntryView} passed in without returning anything
       * @return a {@link CompletableFuture} which will be completed the
       *         operation completes
       */
      CompletableFuture<Void> eval(K key, V value, BiConsumer<V, WriteEntryView<V>> f);

      /**
       * Evaluate a write-only {@link Consumer} operation with a
       * {@link WriteEntryView} of the value associated with the key,
       * and return a {@link CompletableFuture} which will be
       * completed with the object returned by the operation.
       *
       * By returning {@link CompletableFuture} instead of the function's
       * return type directly, the method hints at the possibility that to
       * executing the function might require remote data present in either
       * a persistent store or a remote clustered node.
       *
       * DESIGN RATIONALES:
       * <ul>
       *    <li>Since this is a write-only operation, no entry attributes can be
       *    queried, hence the only reasonable thing can be returned is Void.
       *    </li>
       *    <li>Why provide this operation? Isn't {@link #eval(Object, Object, BiConsumer)} enough?
       *    Possibly, this operation makes a bit simpler to write constant
       *    values along with optional metadata parameters.
       *    </li>
       * </ul>
       *
       * @param key the key associated with the {@link WriteEntryView} to be
       *            passed to the operation
       * @param f operation that takes a {@link WriteEntryView} associated with
       *          the key and writes to the it without returning anything
       * @return a {@link CompletableFuture} which will be completed the
       *         operation completes
       */
      CompletableFuture<Void> eval(K key, Consumer<WriteEntryView<V>> f);

      /**
       * Evaluate a function... TODO...
       *
       * This method can be used to implement operations such as:
       *
       * <ul>
       *    <li>{@link ConcurrentMap#putAll(Map)}</li>
       *    <li>{@link javax.cache.Cache#putAll(Map)}</li>
       * </ul>
       *
       * DESIGN RATIONALE:
       * <ul>
       *    <li>It makes sense to expose global operation like this
       *    instead of forcing users to iterate over the keys to lookup and call
       *    get individually since Infinispan can do things more efficiently.
       *    </li>
       *    <li>Since this is a write-only operation, no entry attributes can be
       *    queried, hence the only reasonable thing can be returned is Void.
       *    </li>
       *    <li>It still makes sense to return Observable<Void> instead of
       *    CompletableFuture<Void> in case the user wants to do something as each
       *    entry gets consumed, e.g. keep track of progress.
       *    </li>
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

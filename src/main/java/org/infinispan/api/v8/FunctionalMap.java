package org.infinispan.api.v8;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface FunctionalMap<K, V> extends AutoCloseable {

   // TODO: Consider adding filter and/or filterKeys, filterValues...
   // TODO: Consider adding eval(K, V, BiFunction) to avoid lambda instantiation for put ops

   /**
    *
    */
   FunctionalMap<K, V> withParams(Param<?>... ps);

   /**
    * Evaluate a function on the value associated with the key.
    *
    * This method can be used to implement the vast majority of single-key
    * based operations in {@link ConcurrentMap} such as
    * {@link ConcurrentMap#put(Object, Object)}, {@link ConcurrentMap#get(Object)},
    * {@link ConcurrentMap#remove(Object)}, {@link ConcurrentMap#putIfAbsent(Object, Object)},
    * {@link ConcurrentMap#replace(Object, Object)}...etc.
    */
   <T> CompletableFuture<T> eval(K key, Function<Value<V>, ? extends T> f);

   /**
    * Evaluate a function on all the values.
    *
    * This method can be used to implement JCache's removeAll().
    */
   <T> Observable<T> evalAll(Function<Value<V>, ? extends T> f);

   /**
    * Evaluate a function on the values associated with the subset of keys passed in.
    *
    * This method can be used to ...
    */
   <T> Observable<T> evalMany(Collection<? extends K> s, Function<Value<V>, ? extends T> f);

   /**
    * Evaluate a function... TODO...
    *
    * This method can be used to implement operations such as
    * {@link ConcurrentMap#putAll(Map)}.
    */
   <T> Observable<T> evalMany(Map<? extends K, ? extends V> s, BiFunction<? super V, Value<V>, ? extends T> f);

   /**
    * Truncate
    */
   CompletableFuture<Void> truncate();

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
   <T> CompletableFuture<Optional<T>> findAny(Function<Pair<K, V>, Optional<T>> f);

   /**
    * TODO...
    */
   <T> CompletableFuture<T> reduce(T z, BiFunction<Pair<K, V>, T, T> f);

}

package org.infinispan.api.v8;

import org.infinispan.api.v8.Functions.PairFunction;
import org.infinispan.api.v8.Functions.ValueFunction;
import org.infinispan.api.v8.Mode.AccessMode;
import org.infinispan.api.v8.Mode.StreamMode;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface FunctionalMap<K, V> {

   FunctionalMap<K, V> withParams(Param... params);

   <T> CompletableFuture<T> eval(K key, AccessMode mode, ValueFunction<? super V, ? extends T> f);

   // Eval all, to do: side-effecting fold e.g. remove all entries, one by one, and fire listeners...
   <T, P> Stream<Pair<K, T>> evalAll(AccessMode mode, ValueFunction<? super V, ? extends T> f);

   // Even better: pipe input Stream into output Stream
   <T, P> Stream<Pair<K, T>> evalMany(Stream<Pair<K, P>> iter, AccessMode mode,
      Functions.ValueBiFunction<? super P, ? extends T> f);

   // A further improvement: better, return a stream...
   // <T> Stream<Pair<K, T>> evalMany(Map<? extends K, ? extends V> iter, AccessMode mode,
   //    ValueBiFunction<? super V, ? extends T> f);

   // Original option: a bit limiting...
   //   <T> Map<K, CompletableFuture<T>> evalMany(Map<? extends K, ? extends V> iter, AccessMode mode,
   //      ValueBiFunction<? super V, ? extends T> f);

   CompletableFuture<Void> truncate();

   // TODO: Move to EnumSet to be able to pass combination of stream modes, e.g. KEYS + SEGMENT, VALUES + MACHINE, KEYS + VALUES + RACK
   <T> CompletableFuture<Optional<T>> search(StreamMode mode, PairFunction<? super K, ? super V, ? extends T> f);

   // TODO: Move to EnumSet to be able to pass combination of stream modes, e.g. KEYS + SEGMENT, VALUES + MACHINE, KEYS + VALUES + RACK
   <T> CompletableFuture<T> fold(StreamMode mode, T z, Functions.PairBiFunction<? super K, ? super V, ? super T, ? extends T> f);

}

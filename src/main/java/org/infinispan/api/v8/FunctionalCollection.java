package org.infinispan.api.v8;

import org.infinispan.api.v8.Functions.ValueFunction;
import org.infinispan.api.v8.Mode.AccessMode;
import org.infinispan.api.v8.Mode.StreamMode;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public interface FunctionalCollection<E> extends AutoCloseable {

   FunctionalCollection<E> withParams(Param... params);

   <T> CompletableFuture<T> eval(AccessMode mode, ValueFunction<? super E, ? extends T> f);

//   // Eval all, to do: side-effecting fold e.g. remove all entries, one by one, and fire listeners...
//   <T, P> Stream<Pair<K, T>> evalAll(AccessMode mode, ValueBiFunction<? super P, ? extends T> f);
//
//   // Even better: pipe input Stream into output Stream
//   <T, P> Stream<Pair<K, T>> evalMany(Stream<Pair<K, P>> iter, AccessMode mode,
//      ValueBiFunction<? super P, ? extends T> f);
//
//   CompletableFuture<Void> truncate();

   Stream<E> stream();

   // TODO: Move to EnumSet to be able to pass combination of stream modes, e.g. KEYS + SEGMENT, VALUES + MACHINE, KEYS + VALUES + RACK
   <T> CompletableFuture<Optional<T>> search(StreamMode mode, Function<E, ? extends T> f);

   // TODO: Move to EnumSet to be able to pass combination of stream modes, e.g. KEYS + SEGMENT, VALUES + MACHINE, KEYS + VALUES + RACK
   <T> CompletableFuture<T> fold(StreamMode mode, T z, BiFunction<? super E, ? super T, ? extends T> f);

}

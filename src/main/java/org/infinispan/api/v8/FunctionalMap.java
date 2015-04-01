package org.infinispan.api.v8;

import org.infinispan.api.v8.Functions.PairFunction;
import org.infinispan.api.v8.Functions.ValueFunction;
import org.infinispan.api.v8.Mode.AccessMode;
import org.infinispan.api.v8.Mode.StreamMode;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface FunctionalMap<K, V> extends AutoCloseable {

   FunctionalMap<K, V> withParams(Param... params);

   FunctionalMap<K, V> withAccessMode(AccessMode mode); // weakest default?

   // TODO: @Mario, Weakest default? READ?
   // TODO: @Mario, compute lambda locally and replicate the value only? For traditional cases it would be enough...
   // TODO: What if the lambda not serialize? Could we run it locally alone? And fetch/send whatever you need? Default is local...
   // TODO: Executing lambdas remotely optional in the future, for higher level data structures maybe? e.g. List

   // TODO: CompletableFuture can't be interrupted? The workaround would be to complete it with a fake value or similar...
   <T> CompletableFuture<T> eval(K key, AccessMode mode, ValueFunction<? super V, ? extends T> f);

   // Eval all, to do: side-effecting fold e.g. remove all entries, one by one, and fire listeners...
   // TODO: @Mario: stream should not block when trying to provide the next element... something like Observable would be better...
   // TODO: @Mario, stream can be lazy... but is not designed to be asynchronous. Either:
   // our own Observable, or Stream<Completable<T>>...
   // TODO: Combining Observables vs combining streams
   // <T, P> Stream<Pair<K, T>> evalAll(AccessMode mode, ValueFunction<? super V, ? extends T> f);

   // Even better: pipe input Stream into output Stream
   // TODO: @Mario, Observable to Stream? Stream to Observable?
   // TODO: Our own Observable to a Stream is trivial, but doing the opposite
   // TODO: Push vs Pull, Future.get vs Completable future, that's the same as Stream and RxJava's Observable
   // TODO: @Mario, too generic, split into two...
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
   // TODO: Better with a Predicate and return true/false, findFirst???
   <T> CompletableFuture<Optional<T>> search(StreamMode mode, PairFunction<? super K, ? super V, ? extends T> f);

   // TODO: Add searchAll or filter... method for being able to return a Observable/Stream....

   // TODO: Move to EnumSet to be able to pass combination of stream modes, e.g. KEYS + SEGMENT, VALUES + MACHINE, KEYS + VALUES + RACK
   // TODO: Reduce better naming
   <T> CompletableFuture<T> reduce(StreamMode mode, T z, Functions.PairBiFunction<? super K, ? super V, ? super T, ? extends T> f);

}

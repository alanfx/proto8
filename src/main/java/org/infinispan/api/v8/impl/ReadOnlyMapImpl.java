package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.EntryView.ReadEntryView;
import org.infinispan.api.v8.FunctionalMap.ReadOnlyMap;
import org.infinispan.api.v8.Observable;
import org.infinispan.api.v8.Pair;
import org.infinispan.api.v8.Param;
import org.infinispan.api.v8.Param.WaitMode;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.infinispan.api.v8.Param.WaitMode.withWaitMode;

public class ReadOnlyMapImpl<K, V> implements ReadOnlyMap<K, V> {

   private final Params params;
   private final FunctionalMapImpl<K, V> functionalMap;

   private ReadOnlyMapImpl(Params params, FunctionalMapImpl<K, V> functionalMap) {
      this.params = params;
      this.functionalMap = functionalMap;
   }

   public static <K, V> ReadOnlyMap<K, V> create(FunctionalMapImpl<K, V> functionalMap) {
      return new ReadOnlyMapImpl<>(Params.from(functionalMap.params.params), functionalMap);
   }

   private static <K, V> ReadOnlyMap<K, V> create(Params params, FunctionalMapImpl<K, V> functionalMap) {
      return new ReadOnlyMapImpl<>(params, functionalMap);
   }

   @Override
   public <R> CompletableFuture<R> eval(K key, Function<ReadEntryView<V>, R> f) {
      System.out.printf("[R] Invoked eval(k=%s, %s)%n", key, params);
      Param<WaitMode> waitMode = params.get(WaitMode.ID);
      return withWaitMode(waitMode.get(), () -> f.apply(Values.readOnly(functionalMap.data.get(key))));
   }

   @Override
   public <R> Observable<R> evalMany(Collection<? extends K> s, Function<ReadEntryView<V>, R> f) {
      return null;  // TODO: To implement...
   }

   @Override
   public <T> CompletableFuture<Optional<T>> findAny(Function<Pair<K, V>, Optional<T>> f) {
      System.out.printf("[R] Invoked findAny(%s)%n", params);
      Param<EnumSet<Param.StreamModes>> streamModes = params.get(Param.StreamMode.ID);

      if (streamModes.get().contains(Param.StreamModes.KEYS))
         return CompletableFuture.supplyAsync(() -> keysOnlyFind(f));
      else if (streamModes.get().contains(Param.StreamModes.VALUES))
         return CompletableFuture.supplyAsync(() -> valuesOnlyFind(f));
      else
         throw new IllegalStateException();
   }

   private <T> Optional<T> keysOnlyFind(Function<Pair<K, V>, Optional<T>> f) {
      for (K next : functionalMap.data.keySet()) {
         Optional<T> applied = f.apply(Pairs.ofKey(next));
         if (applied.isPresent())
            return applied;
      }

      return Optional.empty();
   }

   private <T> Optional<T> valuesOnlyFind(Function<Pair<K, V>, Optional<T>> f) {
      for (InternalEntry<V> next : functionalMap.data.values()) {
         Optional<T> applied = f.apply(Pairs.ofValue(next.value));
         if (applied.isPresent())
            return applied;
      }

      return Optional.empty();
   }

   @Override
   public <T> CompletableFuture<T> reduce(T z, BiFunction<Pair<K, V>, T, T> f) {
      System.out.printf("[R] Invoked reduce(%s)%n", params);
      Param<EnumSet<Param.StreamModes>> streamModes = params.get(Param.StreamMode.ID);
      Param<WaitMode> waitMode = params.get(WaitMode.ID);

      if (streamModes.get().contains(Param.StreamModes.KEYS) && streamModes.get().contains(Param.StreamModes.VALUES))
         return withWaitMode(waitMode.get(), () -> keysValuesReduce(z, f));
      else if (streamModes.get().contains(Param.StreamModes.KEYS))
         return withWaitMode(waitMode.get(), () -> keysOnlyReduce(z, f));
      else if (streamModes.get().contains(Param.StreamModes.VALUES))
         return withWaitMode(waitMode.get(), () -> valuesOnlyReduce(z, f));
      else
         throw new IllegalStateException();
   }

   public <T> T keysValuesReduce(T z, BiFunction<Pair<K, V>, T, T> f) {
      T acc = z;
      for (Map.Entry<K, InternalEntry<V>> next : functionalMap.data.entrySet())
         acc = f.apply(Pairs.of(next.getKey(), next.getValue().value), acc);

      return acc;
   }

   public <T> T keysOnlyReduce(T z, BiFunction<Pair<K, V>, T, T> f) {
      T acc = z;
      for (K next : functionalMap.data.keySet())
         acc = f.apply(Pairs.ofKey(next), acc);

      return acc;
   }

   public <T> T valuesOnlyReduce(T z, BiFunction<Pair<K, V>, T, T> f) {
      T acc = z;
      for (InternalEntry<V> next : functionalMap.data.values())
         acc = f.apply(Pairs.ofValue(next.value), acc);

      return acc;
   }

   @Override
   public ReadOnlyMap<K, V> withParams(Param<?>... ps) {
      if (ps == null || ps.length == 0)
         return this;

      if (params.containsAll(ps))
         return this; // We already have all specified params

      return create(params.addAll(ps), functionalMap);
   }

   @Override
   public void close() throws Exception {
      functionalMap.close();
   }
}

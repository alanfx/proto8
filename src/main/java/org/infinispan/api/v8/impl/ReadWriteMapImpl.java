package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.EntryView.ReadWriteEntryView;
import org.infinispan.api.v8.FunctionalMap.ReadWriteMap;
import org.infinispan.api.v8.Listeners.ReadWriteListeners;
import org.infinispan.api.v8.Param;
import org.infinispan.api.v8.Traversable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.infinispan.api.v8.Param.WaitMode.ID;
import static org.infinispan.api.v8.Param.WaitMode.withWaitMode;

public final class ReadWriteMapImpl<K, V> extends AbstractFunctionalMap<K, V> implements ReadWriteMap<K, V> {

   private final Params params;

   private ReadWriteMapImpl(Params params, FunctionalMapImpl<K, V> functionalMap) {
      super(functionalMap);
      this.params = params;
   }

   public static <K, V> ReadWriteMap<K, V> create(FunctionalMapImpl<K, V> functionalMap) {
      return new ReadWriteMapImpl<>(Params.from(functionalMap.params.params), functionalMap);
   }

   private static <K, V> ReadWriteMap<K, V> create(Params params, FunctionalMapImpl<K, V> functionalMap) {
      return new ReadWriteMapImpl<>(params, functionalMap);
   }

   @Override
   public <R> CompletableFuture<R> eval(K key, Function<ReadWriteEntryView<K, V>, R> f) {
      System.out.printf("[RW] Invoked eval(k=%s, %s)%n", key, params);
      Param<Param.WaitMode> waitMode = params.get(Param.WaitMode.ID);
      return withWaitMode(waitMode.get(), () -> f.apply(EntryViews.readWrite(key, this)));
   }

   @Override
   public <R> CompletableFuture<R> eval(K key, V value, BiFunction<V, ReadWriteEntryView<K, V>, R> f) {
      System.out.printf("[W] Invoked eval(k=%s, v=%s, %s)%n", key, value, params);
      Param<Param.WaitMode> waitMode = params.get(Param.WaitMode.ID);
      return withWaitMode(waitMode.get(), () -> f.apply(value, EntryViews.readWrite(key, this)));
   }

   @Override
   public <R> Traversable<R> evalMany(Map<? extends K, ? extends V> m, BiFunction<V, ReadWriteEntryView<K, V>, R> f) {
      throw new IllegalStateException("Not yet implemented");
   }

   @Override
   public <R> Traversable<R> evalMany(Set<? extends K> keys, Function<ReadWriteEntryView<K, V>, R> f) {
      System.out.printf("[RW] Invoked evalMany(keys=%s, %s)%n", keys, params);
      Param<Param.WaitMode> waitMode = params.get(ID);
      switch (waitMode.get()) {
         case BLOCKING:
            Stream<R> stream = keys.stream()
               .map(k -> f.apply(EntryViews.readWrite(k, ReadWriteMapImpl.this)));
            return Traversables.of(stream);
         default:
            throw new IllegalStateException();
      }
   }

   @Override
   public Traversable<ReadWriteEntryView<K, V>> entries() {
      throw new IllegalStateException("Not yet implemented");
   }

   @Override
   public ReadWriteListeners<K, V> listeners() {
      return functionalMap.notifier;
   }

   @Override
   public ReadWriteMap<K, V> withParams(Param<?>... ps) {
      if (ps == null || ps.length == 0)
         return this;

      if (params.containsAll(ps))
         return this; // We already have all specified params

      return create(params.addAll(ps), functionalMap);
   }

}

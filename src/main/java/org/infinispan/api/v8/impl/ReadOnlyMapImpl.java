package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.EntryView.ReadEntryView;
import org.infinispan.api.v8.FunctionalMap.ReadOnlyMap;
import org.infinispan.api.v8.Param;
import org.infinispan.api.v8.Param.WaitMode;
import org.infinispan.api.v8.Traversable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.infinispan.api.v8.Param.WaitMode.withWaitMode;

public final class ReadOnlyMapImpl<K, V> extends AbstractFunctionalMap<K, V> implements ReadOnlyMap<K, V> {

   private final Params params;

   private ReadOnlyMapImpl(Params params, FunctionalMapImpl<K, V> functionalMap) {
      super(functionalMap);
      this.params = params;
   }

   public static <K, V> ReadOnlyMap<K, V> create(FunctionalMapImpl<K, V> functionalMap) {
      return new ReadOnlyMapImpl<>(Params.from(functionalMap.params.params), functionalMap);
   }

   private static <K, V> ReadOnlyMap<K, V> create(Params params, FunctionalMapImpl<K, V> functionalMap) {
      return new ReadOnlyMapImpl<>(params, functionalMap);
   }

   @Override
   public <R> CompletableFuture<R> eval(K key, Function<ReadEntryView<K, V>, R> f) {
      System.out.printf("[R] Invoked eval(k=%s, %s)%n", key, params);
      Param<WaitMode> waitMode = params.get(WaitMode.ID);
      return withWaitMode(waitMode.get(), () -> f.apply(EntryViews.readOnly(key, functionalMap.data.get(key))));
   }

   @Override
   public <R> Traversable<R> evalMany(Set<? extends K> s, Function<ReadEntryView<K, V>, R> f) {
      System.out.printf("[R] Invoked evalMany(m=%s, %s)%n", s, params);
      Param<WaitMode> waitMode = params.get(WaitMode.ID);
      switch (waitMode.get()) {
         case BLOCKING:
            Stream<R> stream = functionalMap.data.entrySet().stream()
               .filter(e -> s.contains(e.getKey()))
               .map(e -> f.apply(EntryViews.readOnly(e.getKey(), e.getValue())));
            return Traversables.eager(stream);
         default:
            throw new IllegalStateException("Not yet implemented");
      }
   }

   @Override
   public Traversable<K> keys() {
      System.out.printf("[R] Invoked keys(%s)%n", params);
      Param<WaitMode> waitMode = params.get(WaitMode.ID);
      switch (waitMode.get()) {
         case BLOCKING:
            return Traversables.eager(functionalMap.data.keySet().stream());
         default:
            throw new IllegalStateException("Not yet implemented");
      }
   }

   @Override
   public Traversable<ReadEntryView<K, V>> entries() {
      System.out.printf("[R] Invoked entries(%s)%n", params);
      Param<WaitMode> waitMode = params.get(WaitMode.ID);
      switch (waitMode.get()) {
         case BLOCKING:
            Stream<ReadEntryView<K, V>> stream = functionalMap.data.entrySet().stream()
               .map(e -> EntryViews.readOnly(e.getKey(), e.getValue()));
            return Traversables.of(stream);
         default:
            throw new IllegalStateException("Not yet implemented");
      }
   }

   @Override
   public ReadOnlyMap<K, V> withParams(Param<?>... ps) {
      if (ps == null || ps.length == 0)
         return this;

      if (params.containsAll(ps))
         return this; // We already have all specified params

      return create(params.addAll(ps), functionalMap);
   }

}

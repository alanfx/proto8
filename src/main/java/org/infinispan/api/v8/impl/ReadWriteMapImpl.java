package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.EntryView;
import org.infinispan.api.v8.EntryView.ReadWriteEntryView;
import org.infinispan.api.v8.FunctionalMap;
import org.infinispan.api.v8.FunctionalMap.ReadWriteMap;
import org.infinispan.api.v8.Param;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.infinispan.api.v8.Param.WaitMode.withWaitMode;

public class ReadWriteMapImpl<K, V> implements ReadWriteMap<K, V> {

   private final Params params;
   private final FunctionalMapImpl<K, V> functionalMap;

   private ReadWriteMapImpl(Params params, FunctionalMapImpl<K, V> functionalMap) {
      this.params = params;
      this.functionalMap = functionalMap;
   }

   public static <K, V> ReadWriteMap<K, V> create(FunctionalMapImpl<K, V> functionalMap) {
      return new ReadWriteMapImpl<>(Params.from(functionalMap.params.params), functionalMap);
   }

   private static <K, V> ReadWriteMap<K, V> create(Params params, FunctionalMapImpl<K, V> functionalMap) {
      return new ReadWriteMapImpl<>(params, functionalMap);
   }

   @Override
   public <R> CompletableFuture<R> eval(K key, Function<ReadWriteEntryView<V>, R> f) {
      System.out.printf("[RW] Invoked eval(k=%s, %s)%n", key, params);
      Param<Param.WaitMode> waitMode = params.get(Param.WaitMode.ID);
      return withWaitMode(waitMode.get(), () -> f.apply(Values.readWrite(key, functionalMap.data)));
   }

   @Override
   public <R> CompletableFuture<R> eval(K key, V value, BiFunction<V, ReadWriteEntryView<V>, R> f) {
      System.out.printf("[W] Invoked eval(k=%s, v=%s, %s)%n", key, value, params);
      Param<Param.WaitMode> waitMode = params.get(Param.WaitMode.ID);
      return withWaitMode(waitMode.get(), () -> f.apply(value, Values.readWrite(key, functionalMap.data)));
   }

   @Override
   public ReadWriteMap<K, V> withParams(Param<?>... ps) {
      if (ps == null || ps.length == 0)
         return this;

      if (params.containsAll(ps))
         return this; // We already have all specified params

      return create(params.addAll(ps), functionalMap);
   }

   @Override
   public void close() throws Exception {
      // TODO: Customise this generated block
   }
}

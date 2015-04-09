package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.FunctionalMap;
import org.infinispan.api.v8.Functions;
import org.infinispan.api.v8.Mode;
import org.infinispan.api.v8.Pair;
import org.infinispan.api.v8.Param;
import org.infinispan.api.v8.Param.AccessMode;
import org.infinispan.api.v8.Value;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Stream;

public class FunctionalMapImpl<K, V> implements FunctionalMap<K, V> {

   private final Params params;
   private final ConcurrentMap<K, ? super V> data;

   private FunctionalMapImpl(Params params, ConcurrentMap<K, ? super V> data) {
      this.params = params;
      this.data = data;
   }

   public static <K, V> FunctionalMap<K, V> create() {
      return new FunctionalMapImpl<K, V>(Params.create(), new ConcurrentHashMap<>());
   }

   private static <K, V> FunctionalMap<K, V> create(Params params, ConcurrentMap<K, ? super V> data) {
      return new FunctionalMapImpl<K, V>(params, data);
   }

   @Override
   public FunctionalMap<K, V> withParams(Param<?>... ps) {
      if (ps == null || ps.length == 0)
         return this;

      if (params.containsAll(ps))
         return this; // We already have all specified params

      return create(params.addAll(ps), data);
   }

   @Override
   public <T> CompletableFuture<T> eval(K key, Function<Value<? super V>, ? extends T> f) {
      System.out.printf("Invoked eval(k=%s, %s)%n", key, params);
      Param<AccessMode> accessMode = params.get(AccessMode.ID);
      switch (accessMode.get()) {
         case READ_ONLY:
            return CompletableFuture.supplyAsync(() -> f.apply(Values.readOnly(data.get(key))));
         case WRITE_ONLY:
            return CompletableFuture.supplyAsync(() -> f.apply(Values.writeOnly(key, data)));
         case READ_WRITE:
            return CompletableFuture.supplyAsync(() -> f.apply(Values.writeOnly(key, data)));
         default:
            throw new IllegalStateException();
      }
   }

   @Override
   public <T, P> Stream<Pair<K, T>> evalMany(Stream<Pair<K, P>> iter, Functions.ValueBiFunction<? super P, ? extends T> f) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public CompletableFuture<Void> truncate() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public <T> CompletableFuture<Optional<T>> search(Mode.StreamMode mode, Functions.PairFunction<? super K, ? super V, ? extends T> f) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public <T> CompletableFuture<T> reduce(Mode.StreamMode mode, T z, Functions.PairBiFunction<? super K, ? super V, ? super T, ? extends T> f) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void close() throws Exception {
      System.out.println("close");
   }

}

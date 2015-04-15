package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.FunctionalMap;
import org.infinispan.api.v8.Observable;
import org.infinispan.api.v8.Pair;
import org.infinispan.api.v8.Param;
import org.infinispan.api.v8.Param.AccessMode;
import org.infinispan.api.v8.Param.StreamMode;
import org.infinispan.api.v8.Param.StreamModes;
import org.infinispan.api.v8.Param.WaitMode;
import org.infinispan.api.v8.Value;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.infinispan.api.v8.Param.WaitMode.ID;
import static org.infinispan.api.v8.Param.WaitMode.defaultValue;

public class FunctionalMapImpl<K, V> implements FunctionalMap<K, V> {

   private final Params params;
   private final ConcurrentMap<K, V> data;

   private FunctionalMapImpl(Params params, ConcurrentMap<K, V> data) {
      this.params = params;
      this.data = data;
   }

   public static <K, V> FunctionalMap<K, V> create() {
      return new FunctionalMapImpl<K, V>(Params.create(), new ConcurrentHashMap<>());
   }

   private static <K, V> FunctionalMap<K, V> create(Params params, ConcurrentMap<K, V> data) {
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
   public <T> CompletableFuture<T> eval(K key, Function<Value<V>, ? extends T> f) {
      System.out.printf("Invoked eval(k=%s, %s)%n", key, params);
      Param<AccessMode> accessMode = params.get(AccessMode.ID);
      Param<WaitMode> waitMode = params.get(WaitMode.ID);
      switch (accessMode.get()) {
         case READ_ONLY:
            return withWaitMode(waitMode.get(), () -> f.apply(Values.readOnly(data.get(key))));
         case WRITE_ONLY:
            return withWaitMode(waitMode.get(), () -> f.apply(Values.writeOnly(key, data)));
         case READ_WRITE:
            return withWaitMode(waitMode.get(), () -> f.apply(Values.readWrite(key, data)));
         default:
            throw new IllegalStateException();
      }
   }

   private <T> CompletableFuture<T> withWaitMode(WaitMode waitMode, Supplier<T> s) {
      switch (waitMode) {
         case BLOCKING:
            return CompletableFuture.completedFuture(s.get());
         case NON_BLOCKING:
            return CompletableFuture.supplyAsync(s);
         default:
            throw new IllegalStateException();
      }
   }

   @Override
   public <T> Observable<T> evalAll(Function<Value<V>, ? extends T> f) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public <T> Observable<T> evalMany(Collection<? extends K> s, Function<Value<V>, ? extends T> f) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public <T> Observable<T> evalMany(Map<? extends K, ? extends V> s, BiFunction<? super V, Value<V>, ? extends T> f) {
      System.out.printf("Invoked evalMany(%s)%n", params);
      Param<WaitMode> waitMode = params.get(ID);
      switch (waitMode.get()) {
         case BLOCKING:
            return new Observable<T>() {
               @Override
               public Subscription subscribe(Observer<? super T> observer) {
                  s.entrySet().forEach(e ->
                     f.apply(e.getValue(), Values.writeOnly(e.getKey(), data)));
                  return null;
               }
            };
         default:
            throw new IllegalStateException();
      }
   }

   @Override
   public CompletableFuture<Void> truncate() {
      return CompletableFuture.runAsync(data::clear);
   }

   @Override
   public <T> CompletableFuture<Optional<T>> findAny(Function<Pair<K, V>, Optional<T>> f) {
      System.out.printf("Invoked findAny(%s)%n", params);
      Param<EnumSet<StreamModes>> streamModes = params.get(StreamMode.ID);

      if (streamModes.get().contains(StreamModes.KEYS))
         return CompletableFuture.supplyAsync(() -> keysOnlyFind(f));
      else if (streamModes.get().contains(StreamModes.VALUES))
         return CompletableFuture.supplyAsync(() -> valuesOnlyFind(f));
      else
         throw new IllegalStateException();
   }

   private <T> Optional<T> keysOnlyFind(Function<Pair<K, V>, Optional<T>> f) {
      for (K next : data.keySet()) {
         Optional<T> applied = f.apply(Pairs.ofKey(next));
         if (applied.isPresent())
            return applied;
      }

      return Optional.empty();
   }

   private <T> Optional<T> valuesOnlyFind(Function<Pair<K, V>, Optional<T>> f) {
      for (V next : data.values()) {
         Optional<T> applied = f.apply(Pairs.ofValue(next));
         if (applied.isPresent())
            return applied;
      }

      return Optional.empty();
   }

   @Override
   public <T> CompletableFuture<T> reduce(T z, BiFunction<Pair<K, V>, T, T> f) {
      System.out.printf("Invoked reduce(%s)%n", params);
      Param<EnumSet<StreamModes>> streamModes = params.get(StreamMode.ID);

      if (streamModes.get().contains(StreamModes.KEYS))
         return CompletableFuture.supplyAsync(() -> keysOnlyReduce(z, f));
      else
         throw new IllegalStateException();
   }

   public <T> T keysOnlyReduce(T z, BiFunction<Pair<K, V>, T, T> f) {
      T acc = z;
      for (K next : data.keySet())
         acc = f.apply(Pairs.ofKey(next), acc);

      return acc;
   }

   @Override
   public void close() throws Exception {
      System.out.println("close");
   }

}

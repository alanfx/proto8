package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.FunctionalMap;
import org.infinispan.api.v8.Param;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class FunctionalMapImpl<K, V> implements FunctionalMap<K, V> {

   final Params params;
   final ConcurrentMap<K, InternalEntry<V>> data;

   private FunctionalMapImpl(Params params, ConcurrentMap<K, InternalEntry<V>> data) {
      this.params = params;
      this.data = data;
   }

   public static <K, V> FunctionalMapImpl<K, V> create() {
      return new FunctionalMapImpl<K, V>(Params.create(), new ConcurrentHashMap<>());
   }

   private static <K, V> FunctionalMapImpl<K, V> create(Params params, ConcurrentMap<K, InternalEntry<V>> data) {
      return new FunctionalMapImpl<K, V>(params, data);
   }

   @Override
   public FunctionalMapImpl<K, V> withParams(Param<?>... ps) {
      if (ps == null || ps.length == 0)
         return this;

      if (params.containsAll(ps))
         return this; // We already have all specified params

      return create(params.addAll(ps), data);
   }

//   @Override
//   public <T> CompletableFuture<T> eval(K key, Function<EntryView<V>, ? extends T> f) {
//      System.out.printf("Invoked eval(k=%s, %s)%n", key, params);
//      Param<AccessMode> accessMode = params.get(AccessMode.ID);
//      Param<WaitMode> waitMode = params.get(WaitMode.ID);
//      switch (accessMode.get()) {
//         case READ_ONLY:
//            return withWaitMode(waitMode.get(), () -> f.apply(Values.readOnly(data.get(key))));
//         case WRITE_ONLY:
//            return withWaitMode(waitMode.get(), () -> f.apply(Values.writeOnly(key, data)));
//         case READ_WRITE:
//            return withWaitMode(waitMode.get(), () -> f.apply(Values.readWrite(key, data)));
//         default:
//            throw new IllegalStateException();
//      }
//   }

//   @Override
//   public <T> Observable<T> evalAll(Function<EntryView<V>, ? extends T> f) {
//      return null;  // TODO: Customise this generated block
//   }
//
//   @Override
//   public <T> Observable<T> evalMany(Collection<? extends K> s, Function<EntryView<V>, ? extends T> f) {
//      return null;  // TODO: Customise this generated block
//   }

//   @Override
//   public <T> Observable<T> evalMany(Map<? extends K, ? extends V> s, BiFunction<? super V, EntryView<V>, ? extends T> f) {
//      System.out.printf("Invoked evalMany(%s)%n", params);
//      Param<WaitMode> waitMode = params.get(ID);
//      switch (waitMode.get()) {
//         case BLOCKING:
//            return new Observable<T>() {
//               @Override
//               public Subscription subscribe(Observer<? super T> observer) {
//                  s.entrySet().forEach(e ->
//                     f.apply(e.getValue(), Values.writeOnly(e.getKey(), data)));
//                  return null;
//               }
//            };
//         default:
//            throw new IllegalStateException();
//      }
//   }

//   @Override
//   public CompletableFuture<Void> truncate() {
//      System.out.printf("Invoked truncate(%s)%n", params);
//      return CompletableFuture.runAsync(data::clear);
//   }

//   @Override
//   public <T> CompletableFuture<Optional<T>> findAny(Function<Pair<K, V>, Optional<T>> f) {
//      System.out.printf("Invoked findAny(%s)%n", params);
//      Param<EnumSet<StreamModes>> streamModes = params.get(StreamMode.ID);
//
//      if (streamModes.get().contains(StreamModes.KEYS))
//         return CompletableFuture.supplyAsync(() -> keysOnlyFind(f));
//      else if (streamModes.get().contains(StreamModes.VALUES))
//         return CompletableFuture.supplyAsync(() -> valuesOnlyFind(f));
//      else
//         throw new IllegalStateException();
//   }

//   private <T> Optional<T> keysOnlyFind(Function<Pair<K, V>, Optional<T>> f) {
//      for (K next : data.keySet()) {
//         Optional<T> applied = f.apply(Pairs.ofKey(next));
//         if (applied.isPresent())
//            return applied;
//      }
//
//      return Optional.empty();
//   }
//
//   private <T> Optional<T> valuesOnlyFind(Function<Pair<K, V>, Optional<T>> f) {
//      for (InternalEntry<V> next : data.values()) {
//         Optional<T> applied = f.apply(Pairs.ofValue(next.value));
//         if (applied.isPresent())
//            return applied;
//      }
//
//      return Optional.empty();
//   }

//   @Override
//   public <T> CompletableFuture<T> reduce(T z, BiFunction<Pair<K, V>, T, T> f) {
//      System.out.printf("Invoked reduce(%s)%n", params);
//      Param<EnumSet<StreamModes>> streamModes = params.get(StreamMode.ID);
//      Param<WaitMode> waitMode = params.get(WaitMode.ID);
//
//      if (streamModes.get().contains(StreamModes.KEYS) && streamModes.get().contains(StreamModes.VALUES))
//         return withWaitMode(waitMode.get(), () -> keysValuesReduce(z, f));
//      else if (streamModes.get().contains(StreamModes.KEYS))
//         return withWaitMode(waitMode.get(), () -> keysOnlyReduce(z, f));
//      else if (streamModes.get().contains(StreamModes.VALUES))
//         return withWaitMode(waitMode.get(), () -> valuesOnlyReduce(z, f));
//      else
//         throw new IllegalStateException();
//   }

//   public <T> T keysValuesReduce(T z, BiFunction<Pair<K, V>, T, T> f) {
//      T acc = z;
//      for (Map.Entry<K, InternalEntry<V>> next : data.entrySet())
//         acc = f.apply(Pairs.of(next.getKey(), next.getValue().value), acc);
//
//      return acc;
//   }
//
//   public <T> T keysOnlyReduce(T z, BiFunction<Pair<K, V>, T, T> f) {
//      T acc = z;
//      for (K next : data.keySet())
//         acc = f.apply(Pairs.ofKey(next), acc);
//
//      return acc;
//   }
//
//   public <T> T valuesOnlyReduce(T z, BiFunction<Pair<K, V>, T, T> f) {
//      T acc = z;
//      for (InternalEntry<V> next : data.values())
//         acc = f.apply(Pairs.ofValue(next.value), acc);
//
//      return acc;
//   }

   @Override
   public void close() throws Exception {
      System.out.println("close");
   }

}

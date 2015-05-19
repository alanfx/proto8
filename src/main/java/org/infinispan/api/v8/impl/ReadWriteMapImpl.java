package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.EntryView.ReadWriteEntryView;
import org.infinispan.api.v8.FunctionalMap.ReadWriteMap;
import org.infinispan.api.v8.Observable;
import org.infinispan.api.v8.Param;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

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
      return withWaitMode(waitMode.get(), () -> f.apply(EntryViews.readWrite(key, functionalMap.data)));
   }

   @Override
   public <R> CompletableFuture<R> eval(K key, V value, BiFunction<V, ReadWriteEntryView<K, V>, R> f) {
      System.out.printf("[W] Invoked eval(k=%s, v=%s, %s)%n", key, value, params);
      Param<Param.WaitMode> waitMode = params.get(Param.WaitMode.ID);
      return withWaitMode(waitMode.get(), () -> f.apply(value, EntryViews.readWrite(key, functionalMap.data)));
   }

   @Override
   public <R> Observable<R> evalMany(Map<? extends K, ? extends V> m, BiFunction<V, ReadWriteEntryView<K, V>, R> f) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public <R> Observable<R> evalMany(Set<? extends K> keys, Function<ReadWriteEntryView<K, V>, R> f) {
      System.out.printf("[RW] Invoked evalMany(keys=%s, %s)%n", keys, params);
      Param<Param.WaitMode> waitMode = params.get(ID);
      switch (waitMode.get()) {
         case BLOCKING:
            return new Observable<R>() {
               @Override
               public Subscription subscribe(Observer<? super R> observer) {
                  keys.forEach(k -> observer.onNext(f.apply(EntryViews.readWrite(k, functionalMap.data))));
                  observer.onCompleted();
                  return null;
               }

               @Override
               public Subscription subscribe(Subscriber<? super R> subscriber) {
                  Iterator<? extends K> it = keys.iterator();
                  while (it.hasNext() && !subscriber.isUnsubscribed())
                     subscriber.onNext(f.apply(EntryViews.readWrite(it.next(), functionalMap.data)));

                  if (!subscriber.isUnsubscribed()) subscriber.onCompleted();
                  return null;
               }
            };
         default:
            throw new IllegalStateException();
      }
   }

   @Override
   public Observable<ReadWriteEntryView<K, V>> entries() {
      return null;  // TODO: Customise this generated block
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

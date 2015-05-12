package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.EntryView.WriteEntryView;
import org.infinispan.api.v8.FunctionalMap.WriteOnlyMap;
import org.infinispan.api.v8.Observable;
import org.infinispan.api.v8.Param;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.infinispan.api.v8.Param.WaitMode.ID;
import static org.infinispan.api.v8.Param.WaitMode.withWaitMode;

public class WriteOnlyMapImpl<K, V> implements WriteOnlyMap<K, V> {

   private final Params params;
   private final FunctionalMapImpl<K, V> functionalMap;

   private WriteOnlyMapImpl(Params params, FunctionalMapImpl<K, V> functionalMap) {
      this.params = params;
      this.functionalMap = functionalMap;
   }

   public static <K, V> WriteOnlyMap<K, V> create(FunctionalMapImpl<K, V> functionalMap) {
      return new WriteOnlyMapImpl<>(Params.from(functionalMap.params.params), functionalMap);
   }

   private static <K, V> WriteOnlyMap<K, V> create(Params params, FunctionalMapImpl<K, V> functionalMap) {
      return new WriteOnlyMapImpl<>(params, functionalMap);
   }

   @Override
   public <R> CompletableFuture<R> eval(K key, Function<WriteEntryView<V>, R> f) {
      System.out.printf("[W] Invoked eval(k=%s, %s)%n", key, params);
      Param<Param.WaitMode> waitMode = params.get(Param.WaitMode.ID);
      return withWaitMode(waitMode.get(), () -> f.apply(EntryViews.writeOnly(key, functionalMap.data)));
   }

   @Override
   public <R> CompletableFuture<R> eval(K key, V value, BiFunction<V, WriteEntryView<V>, R> f) {
      System.out.printf("[W] Invoked eval(k=%s, v=%s, %s)%n", key, value, params);
      Param<Param.WaitMode> waitMode = params.get(Param.WaitMode.ID);
      return withWaitMode(waitMode.get(), () -> f.apply(value, EntryViews.writeOnly(key, functionalMap.data)));
   }

   @Override
   public <R> Observable<R> evalMany(Map<? extends K, ? extends V> m, BiFunction<V, WriteEntryView<V>, R> f) {
      System.out.printf("[W] Invoked evalMany(m=%s, %s)%n", m, params);
      Param<Param.WaitMode> waitMode = params.get(ID);
      switch (waitMode.get()) {
         case BLOCKING:
            return new Observable<R>() {
               @Override
               public Subscription subscribe(Observer<? super R> observer) {
                  m.entrySet().forEach(e -> {
                     R result = f.apply(e.getValue(), EntryViews.writeOnly(e.getKey(), functionalMap.data));
                     observer.onNext(result);
                  });
                  observer.onCompleted();
                  return null;
               }

               @Override
               public Subscription subscribe(Subscriber<? super R> subscriber) {
                  Iterator<? extends Map.Entry<? extends K, ? extends V>> it = m.entrySet().iterator();
                  while (it.hasNext() && !subscriber.isUnsubscribed()) {
                     Map.Entry<? extends K, ? extends V> e = it.next();
                     R result = f.apply(e.getValue(), EntryViews.writeOnly(e.getKey(), functionalMap.data));
                     subscriber.onNext(result);
                  }

                  if (!subscriber.isUnsubscribed()) subscriber.onCompleted();
                  return null;
               }
            };
         default:
            throw new IllegalStateException();
      }
   }

   @Override
   public CompletableFuture<Void> truncate() {
      System.out.printf("[W] Invoked truncate(%s)%n", params);
      return CompletableFuture.runAsync(functionalMap.data::clear);
   }

   @Override
   public WriteOnlyMap<K, V> withParams(Param<?>... ps) {
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

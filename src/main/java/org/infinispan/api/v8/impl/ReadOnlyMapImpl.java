package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.EntryView.ReadEntryView;
import org.infinispan.api.v8.FunctionalMap.ReadOnlyMap;
import org.infinispan.api.v8.Observable;
import org.infinispan.api.v8.Param;
import org.infinispan.api.v8.Param.WaitMode;
import org.infinispan.api.v8.Value;
import org.infinispan.api.v8.util.Tuple;
import org.infinispan.api.v8.util.Tuples;

import java.util.Collection;
import java.util.Iterator;
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
   public <R> CompletableFuture<R> eval(K key, Function<ReadEntryView<K, V>, R> f) {
      System.out.printf("[R] Invoked eval(k=%s, %s)%n", key, params);
      Param<WaitMode> waitMode = params.get(WaitMode.ID);
      return withWaitMode(waitMode.get(), () -> f.apply(EntryViews.readOnly(key, functionalMap.data.get(key))));
   }

   @Override
   public <R> Observable<R> evalMany(Collection<? extends K> s, Function<ReadEntryView<K, V>, R> f) {
      System.out.printf("[R] Invoked evalMany(m=%s, %s)%n", s, params);
      Param<Param.WaitMode> waitMode = params.get(WaitMode.ID);
      switch (waitMode.get()) {
         case BLOCKING:
            return new Observable<R>() {
               @Override
               public Subscription subscribe(Observer<? super R> observer) {
                  s.forEach(k -> {
                     InternalValue<V> entry = functionalMap.data.get(k);
                     if (entry != null) {
                        R result = f.apply(EntryViews.readOnly(k, entry));
                        observer.onNext(result);
                     }
                  });
                  observer.onCompleted();
                  return null;
               }

               @Override
               public Subscription subscribe(Subscriber<? super R> subscriber) {
                  Iterator<? extends K> it = s.iterator();
                  while (it.hasNext() && !subscriber.isUnsubscribed()) {
                     K k = it.next();
                     InternalValue<V> entry = functionalMap.data.get(k);
                     if (entry != null) {
                        R result = f.apply(EntryViews.readOnly(k, entry));
                        subscriber.onNext(result);
                     }
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
   public Observable<K> keys() {
      System.out.printf("[R] Invoked keys(%s)%n", params);
      Param<Param.WaitMode> waitMode = params.get(WaitMode.ID);
      switch (waitMode.get()) {
         case BLOCKING:
            return new Observable<K>() {
               @Override
               public Subscription subscribe(Observer<? super K> observer) {
                  functionalMap.data.forEach((k, v) -> observer.onNext(k));
                  observer.onCompleted();
                  return null;
               }

               @Override
               public Subscription subscribe(Subscriber<? super K> subscriber) {
                  Iterator<K> it = functionalMap.data.keySet().iterator();
                  while (it.hasNext() && !subscriber.isUnsubscribed())
                     subscriber.onNext(it.next());

                  if (!subscriber.isUnsubscribed()) subscriber.onCompleted();
                  return null;
               }
            };
         default:
            throw new IllegalStateException();
      }
   }

   @Override
   public Observable<Value<V>> values() {
      System.out.printf("[R] Invoked values(%s)%n", params);
      Param<Param.WaitMode> waitMode = params.get(WaitMode.ID);
      switch (waitMode.get()) {
         case BLOCKING:
            return new Observable<Value<V>>() {
               @Override
               public Subscription subscribe(Observer<? super Value<V>> observer) {
                  functionalMap.data.forEach((k, v) -> observer.onNext(v));
                  observer.onCompleted();
                  return null;
               }

               @Override
               public Subscription subscribe(Subscriber<? super Value<V>> subscriber) {
                  Iterator<InternalValue<V>> it = functionalMap.data.values().iterator();
                  while (it.hasNext() && !subscriber.isUnsubscribed())
                     subscriber.onNext(it.next());

                  if (!subscriber.isUnsubscribed()) subscriber.onCompleted();
                  return null;
               }
            };
         default:
            throw new IllegalStateException();
      }
   }

   @Override
   public Observable<Tuple<K, Value<V>>> entries() {
      System.out.printf("[R] Invoked entries(%s)%n", params);
      Param<Param.WaitMode> waitMode = params.get(WaitMode.ID);
      switch (waitMode.get()) {
         case BLOCKING:
            return new Observable<Tuple<K, Value<V>>>() {
               @Override
               public Subscription subscribe(Observer<? super Tuple<K, Value<V>>> observer) {
                  functionalMap.data.forEach((k, v) -> observer.onNext(Tuples.of(k, (Value<V>) v)));
                  observer.onCompleted();
                  return null;
               }

               @Override
               public Subscription subscribe(Subscriber<? super Tuple<K, Value<V>>> subscriber) {
                  Iterator<Map.Entry<K, InternalValue<V>>> it = functionalMap.data.entrySet().iterator();
                  while (it.hasNext() && !subscriber.isUnsubscribed()) {
                     Map.Entry<K, InternalValue<V>> entry = it.next();
                     subscriber.onNext(Tuples.of(entry.getKey(), (Value<V>) entry.getValue()));
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

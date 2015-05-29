package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.EntryView.WriteEntryView;
import org.infinispan.api.v8.FunctionalMap.WriteOnlyMap;
import org.infinispan.api.v8.Listeners.WriteListeners;
import org.infinispan.api.v8.Observable;
import org.infinispan.api.v8.Param;
import org.infinispan.api.v8.Param.WaitMode;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.infinispan.api.v8.Param.WaitMode.ID;
import static org.infinispan.api.v8.Param.WaitMode.withWaitMode;

public class WriteOnlyMapImpl<K, V> extends AbstractFunctionalMap<K, V> implements WriteOnlyMap<K, V> {

   private final Params params;

   private WriteOnlyMapImpl(Params params, FunctionalMapImpl<K, V> functionalMap) {
      super(functionalMap);
      this.params = params;
   }

   public static <K, V> WriteOnlyMap<K, V> create(FunctionalMapImpl<K, V> functionalMap) {
      return new WriteOnlyMapImpl<>(Params.from(functionalMap.params.params), functionalMap);
   }

   private static <K, V> WriteOnlyMap<K, V> create(Params params, FunctionalMapImpl<K, V> functionalMap) {
      return new WriteOnlyMapImpl<>(params, functionalMap);
   }

   @Override
   public CompletableFuture<Void> eval(K key, Consumer<WriteEntryView<V>> f) {
      System.out.printf("[W] Invoked eval(k=%s, %s)%n", key, params);
      Param<WaitMode> waitMode = params.get(WaitMode.ID);
      return withWaitMode(waitMode.get(), () -> {
         f.accept(EntryViews.writeOnly(key, this));
         return null;
      });
   }

   @Override
   public CompletableFuture<Void> eval(K key, V value, BiConsumer<V, WriteEntryView<V>> f) {
      System.out.printf("[W] Invoked eval(k=%s, v=%s, %s)%n", key, value, params);
      Param<WaitMode> waitMode = params.get(WaitMode.ID);
      return withWaitMode(waitMode.get(), () -> {
         f.accept(value, EntryViews.writeOnly(key, this));
         return null;
      });
   }

   @Override
   public Observable<Void> evalMany(Map<? extends K, ? extends V> entries, BiConsumer<V, WriteEntryView<V>> f) {
      System.out.printf("[W] Invoked evalMany(entries=%s, %s)%n", entries, params);
      Param<WaitMode> waitMode = params.get(ID);
      switch (waitMode.get()) {
         case BLOCKING:
            return new Observable<Void>() {
               @Override
               public Subscription subscribe(Observer<? super Void> observer) {
                  entries.entrySet().forEach(e -> {
                     f.accept(e.getValue(), EntryViews.writeOnly(e.getKey(), WriteOnlyMapImpl.this));
                     observer.onNext(null);
                  });
                  observer.onCompleted();
                  return null;
               }

               @Override
               public Subscription subscribe(Subscriber<? super Void> subscriber) {
                  Iterator<? extends Map.Entry<? extends K, ? extends V>> it = entries.entrySet().iterator();
                  while (it.hasNext() && !subscriber.isUnsubscribed()) {
                     Map.Entry<? extends K, ? extends V> e = it.next();
                     f.accept(e.getValue(), EntryViews.writeOnly(e.getKey(), WriteOnlyMapImpl.this));
                     subscriber.onNext(null);
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
   public Observable<Void> evalMany(Set<? extends K> keys, Consumer<WriteEntryView<V>> f) {
      System.out.printf("[W] Invoked evalMany(keys=%s, %s)%n", keys, params);
      Param<WaitMode> waitMode = params.get(ID);
      switch (waitMode.get()) {
         case BLOCKING:
            return new Observable<Void>() {
               @Override
               public Subscription subscribe(Observer<? super Void> observer) {
                  keys.forEach(k -> {
                     f.accept(EntryViews.writeOnly(k, WriteOnlyMapImpl.this));
                     observer.onNext(null);
                  });
                  observer.onCompleted();
                  return null;
               }

               @Override
               public Subscription subscribe(Subscriber<? super Void> subscriber) {
                  Iterator<? extends K> it = keys.iterator();
                  while (it.hasNext() && !subscriber.isUnsubscribed()) {
                     f.accept(EntryViews.writeOnly(it.next(), WriteOnlyMapImpl.this));
                     subscriber.onNext(null);
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
   public Observable<WriteEntryView<V>> values() {
      System.out.printf("[W] Invoked values(%s)%n", params);
      Param<WaitMode> waitMode = params.get(WaitMode.ID);
      switch (waitMode.get()) {
         case BLOCKING:
            return new Observable<WriteEntryView<V>>() {
               @Override
               public Subscription subscribe(Observer<? super WriteEntryView<V>> observer) {
                  functionalMap.data.forEach((k, v) ->
                     observer.onNext(EntryViews.writeOnly(k, WriteOnlyMapImpl.this)));
                  observer.onCompleted();
                  return null;
               }

               @Override
               public Subscription subscribe(Subscriber<? super WriteEntryView<V>> subscriber) {
                  Iterator<Map.Entry<K, InternalEntry<V>>> it = functionalMap.data.entrySet().iterator();
                  while (it.hasNext() && !subscriber.isUnsubscribed()) {
                     Map.Entry<K, InternalEntry<V>> entry = it.next();
                     subscriber.onNext(EntryViews.writeOnly(entry.getKey(), WriteOnlyMapImpl.this));
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
   public WriteListeners<K, V> listeners() {
      return functionalMap.notifier;
   }

}

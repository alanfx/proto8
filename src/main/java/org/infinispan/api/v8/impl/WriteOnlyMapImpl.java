package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.Closeables.CloseableIterator;
import org.infinispan.api.v8.EntryView.WriteEntryView;
import org.infinispan.api.v8.FunctionalMap.WriteOnlyMap;
import org.infinispan.api.v8.Listeners.WriteListeners;
import org.infinispan.api.v8.Param;
import org.infinispan.api.v8.Param.WaitMode;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.infinispan.api.v8.Param.WaitMode.*;

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
      return withWaitFuture(waitMode.get(), () -> {
         f.accept(EntryViews.writeOnly(key, this));
         return null;
      });
   }

   @Override
   public CompletableFuture<Void> eval(K key, V value, BiConsumer<V, WriteEntryView<V>> f) {
      System.out.printf("[W] Invoked eval(k=%s, v=%s, %s)%n", key, value, params);
      Param<WaitMode> waitMode = params.get(WaitMode.ID);
      return withWaitFuture(waitMode.get(), () -> {
         f.accept(value, EntryViews.writeOnly(key, this));
         return null;
      });
   }

   @Override
   public CloseableIterator<Void> evalMany(Map<? extends K, ? extends V> entries, BiConsumer<V, WriteEntryView<V>> f) {
      System.out.printf("[W] Invoked evalMany(entries=%s, %s)%n", entries, params);
      Param<WaitMode> waitMode = params.get(ID);
      return withWaitIterator(waitMode, () -> entries.entrySet().stream().map(e -> {
            f.accept(e.getValue(), EntryViews.writeOnly(e.getKey(), WriteOnlyMapImpl.this));
            return null;
         })
      );
   }

   @Override
   public CloseableIterator<Void> evalMany(Set<? extends K> keys, Consumer<WriteEntryView<V>> f) {
      System.out.printf("[W] Invoked evalMany(keys=%s, %s)%n", keys, params);
      Param<WaitMode> waitMode = params.get(ID);
      return withWaitIterator(waitMode, () -> keys.stream().map(k -> {
         f.accept(EntryViews.writeOnly(k, WriteOnlyMapImpl.this));
         return null;
      }));
   }

   @Override
   public CloseableIterator<WriteEntryView<V>> values() {
      System.out.printf("[W] Invoked values(%s)%n", params);
      Param<WaitMode> waitMode = params.get(WaitMode.ID);
      return withWaitIterator(waitMode, () -> functionalMap.data.entrySet().stream()
         .map(e -> EntryViews.writeOnly(e.getKey(), WriteOnlyMapImpl.this))
      );
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

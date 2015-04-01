package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.FunctionalMap;
import org.infinispan.api.v8.Functions;
import org.infinispan.api.v8.Mode;
import org.infinispan.api.v8.Param;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class FunctionalMapImpl<K, V> implements FunctionalMap<K, V> {

   Map<Class<?>, Param> paramsMap; // <- TBD!

   final org.infinispan.cache.impl.CacheImpl oldCacheImpl;

   @Override
   public FunctionalMap<K, V> withParams(Param... params) {
      // paramsMap.put(, ...);
      return null;  // TODO: Customise this generated block
   }

   @Override
   public <T> CompletableFuture<T> eval(K key, Mode.AccessMode mode, Functions.ValueFunction<? super V, ? extends T> f) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public <T> Map<K, CompletableFuture<T>> evalAll(Map<? extends K, ? extends V> iter, Mode.AccessMode mode, Functions.ValueBiFunction<? super V, ? extends T> f) {
      return null;
   }

   @Override
   public CompletableFuture<Void> clearAll() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public <T> CompletableFuture<Optional<T>> search(Mode.StreamMode mode, Functions.PairFunction<? super K, ? super V, ? extends T> f) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public <T> CompletableFuture<T> fold(Mode.StreamMode mode, T z, Functions.PairBiFunction<? super K, ? super V, ? super T, ? extends T> f) {
      return null;  // TODO: Customise this generated block
   }

}

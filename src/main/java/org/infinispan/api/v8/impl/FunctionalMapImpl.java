package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.FunctionalMap;
import org.infinispan.api.v8.Param;
import org.infinispan.api.v8.Status;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class FunctionalMapImpl<K, V> implements FunctionalMap<K, V> {

   final Params params;
   final ConcurrentMap<K, InternalValue<V>> data;
   volatile Status status;

   private FunctionalMapImpl(Params params, ConcurrentMap<K, InternalValue<V>> data) {
      this.params = params;
      this.data = data;
      this.status = Status.STARTED;
   }

   public static <K, V> FunctionalMapImpl<K, V> create() {
      return new FunctionalMapImpl<>(Params.create(), new ConcurrentHashMap<>());
   }

   private static <K, V> FunctionalMapImpl<K, V> create(Params params, ConcurrentMap<K, InternalValue<V>> data) {
      return new FunctionalMapImpl<>(params, data);
   }

   @Override
   public FunctionalMapImpl<K, V> withParams(Param<?>... ps) {
      if (ps == null || ps.length == 0)
         return this;

      if (params.containsAll(ps))
         return this; // We already have all specified params

      return create(params.addAll(ps), data);
   }

   @Override
   public String getName() {
      return "";  // TODO: Customise this generated block
   }

   @Override
   public Status getStatus() {
      return status;
   }

   @Override
   public void close() throws Exception {
      System.out.println("close");
      status = Status.STOPPED;
   }

}

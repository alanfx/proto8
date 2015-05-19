package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.FunctionalMap;
import org.infinispan.api.v8.Status;

public abstract class AbstractFunctionalMap<K, V> implements FunctionalMap<K, V> {

   protected final FunctionalMapImpl<K, V> functionalMap;

   protected AbstractFunctionalMap(FunctionalMapImpl<K, V> functionalMap) {
      this.functionalMap = functionalMap;
   }

   @Override
   public String getName() {
      return "";
   }

   @Override
   public Status getStatus() {
      return functionalMap.getStatus();
   }

   @Override
   public void close() throws Exception {
      functionalMap.close();
   }

}

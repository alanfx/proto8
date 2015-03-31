package org.infinispan.api.v8;

import org.infinispan.api.v8.impl.FunctionalMapImpl;
import org.infinispan.api.v8.impl.MapDecorator;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class Infinispan {

   // CacheManager manager;

   public <K, V> Map<K, V> map(String name) {
      return new MapDecorator<>(new FunctionalMapImpl<>());
   }

   public <K, V> ConcurrentMap<K, V> concurrentMap(String name) {
      return null; // Concurrent map decorator around FunctionalMap
   }

   public <K, V> ConcurrentMap<K, V> minimal(String name) {
      return null; // Bounded CHM + expiration + eviction only!
   }

   // ...

}

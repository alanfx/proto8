package org.infinispan.api.v8;

import org.infinispan.api.v8.impl.FunctionalMapImpl;
import org.infinispan.api.v8.impl.MapDecorator;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class Infinispan {

   // CacheManager manager;

//   public <K, V> Map<K, V> map(String name) {
//      return new MapDecorator<>(new FunctionalMapImpl<>());
//   }                                                                `
//
//   public <K, V> ConcurrentMap<K, V> concurrentMap(String name) {
//      return null; // Concurrent map decorator around FunctionalMap
//   }
//
//   public <K, V> ConcurrentMap<K, V> minimal(String name) {
//      return null; // Bounded CHM + expiration + eviction only!
//   }

   // ...

   public <T> T local(DataStructures dataStructures) {
      // Lookup the cache impl and pass it...
      return (T) dataStructures.create();
   }

   interface Creates<T> {
      T create();
      Class<T> getType();
   }

   public enum DataStructures implements Creates {
      MAP(Map.class) {
         @Override
         public Map create() {
            //return new MapDecorator<>(new FunctionalMapImpl<>());
            return null;
         }

         @Override
         public Class getType() {
            return Map.class;
         }
      },
      CONCURRENT_MAP(ConcurrentMap.class) {
         @Override
         public ConcurrentMap create() {
            return null;
         }

         @Override
         public Class getType() {
            return ConcurrentMap.class;
         }
      };

      final Object instance;

      <T> DataStructures(T instance) {
         this.instance = instance;
      }
   }

}

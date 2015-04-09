package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.Value;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

class Values {

   public static <V> Value<V> readOnly(V value) {
      return new ReadOnlyValue<>(value);
   }

   public static <K, V> Value<V> writeOnly(K key, ConcurrentMap<K, V> data) {
      return new WriteOnlyValue<>(key, data);
   }

   private static class ReadOnlyValue<V> implements Value<V> {
      final V value;

      private ReadOnlyValue(V value) {
         this.value = value;
      }

      @Override
      public Optional<V> get() {
         return Optional.ofNullable(value);
      }

      @Override
      public Void set(V value) {
         throw new IllegalStateException();
      }

      @Override
      public Void remove() {
         throw new IllegalStateException();
      }
   }

   private static class WriteOnlyValue<K, V> implements Value<V> {
      private final ConcurrentMap<K, V> data;
      private final K key;

      private WriteOnlyValue(K key, ConcurrentMap<K, V> data) {
         this.data = data;
         this.key = key;
      }

      @Override
      public Optional<V> get() {
         throw new IllegalStateException();
      }

      @Override
      public Void set(V value) {
         data.put(key, value);
         return null;
      }

      @Override
      public Void remove() {
         throw new IllegalStateException();
      }
   }

}

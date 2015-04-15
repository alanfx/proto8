package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.Pair;

import java.util.Optional;

class Pairs {

   // FIXME: Provide good hash code for pairs

   public static <K, V> Pair<K, V> of(K key, V value) {
      return new PairImpl<>(key, value);
   }

   public static <K, V> Pair<K, V> ofKey(K key) {
      return new KeyOnlyPair<>(key);
   }

   public static <K, V> Pair<K, V> ofValue(V value) {
      return new ValueOnlyPair<>(value);
   }

   private static abstract class AbstractSingleElement<K, V, T> implements Pair<K, V> {
      protected final Optional<T> elem;

      protected AbstractSingleElement(T elem) {
         this.elem = Optional.of(elem);
      }

      @Override
      public Optional<K> key() {
         throw new IllegalStateException();
      }

      @Override
      public Optional<V> value() {
         throw new IllegalStateException();
      }
   }

   private static final class KeyOnlyPair<K, V> extends AbstractSingleElement<K, V, K> {
      private KeyOnlyPair(K key) {
         super(key);
      }

      @Override
      public Optional<K> key() {
         return elem;
      }
   }

   private static final class ValueOnlyPair<K, V> extends AbstractSingleElement<K, V, V> {
      private ValueOnlyPair(V value) {
         super(value);
      }

      @Override
      public Optional<V> value() {
         return elem;
      }
   }

   private static final class PairImpl<K, V> implements Pair<K, V> {
      private final Optional<K> key;
      private final Optional<V> value;

      public PairImpl(K k, V v) {
         this.key = Optional.of(k);
         this.value = Optional.of(v);
      }

      @Override
      public Optional<K> key() {
         return key;
      }

      @Override
      public Optional<V> value() {
         return value;
      }
   }

}

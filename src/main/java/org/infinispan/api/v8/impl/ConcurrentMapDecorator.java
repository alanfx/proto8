package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.FunctionalMap.ReadOnlyMap;
import org.infinispan.api.v8.FunctionalMap.ReadWriteMap;
import org.infinispan.api.v8.FunctionalMap.WriteOnlyMap;
import org.infinispan.api.v8.Observable;
import org.infinispan.api.v8.Observable.Observer;
import org.infinispan.api.v8.Observable.Subscriber;
import org.infinispan.api.v8.Param.WaitMode;
import org.infinispan.api.v8.Value;
import org.infinispan.api.v8.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

public class ConcurrentMapDecorator<K, V> implements ConcurrentMap<K, V>  {

   final ReadOnlyMap<K, V> readOnly;
   final WriteOnlyMap<K, V> writeOnly;
   final ReadWriteMap<K, V> readWrite;

   public ConcurrentMapDecorator(FunctionalMapImpl<K, V> map) {
      FunctionalMapImpl<K, V> blockingMap = map.withParams(WaitMode.BLOCKING);
      this.readOnly = ReadOnlyMapImpl.create(blockingMap);
      this.writeOnly = WriteOnlyMapImpl.create(blockingMap);
      this.readWrite = ReadWriteMapImpl.create(blockingMap);
   }

   @Override
   public int size() {
      Observable<K> keys = readOnly.keys();
      CountingObserver<K> obs = new CountingObserver<K>();
      keys.subscribe(obs); // Wait mode is BLOCKING, so will block until completed
      return obs.count;
   }

   private static final class CountingObserver<T> implements Observer<T> {
      int count;
      @Override public void onNext(T t) {
         count++;
      }
   }

   @Override
   public boolean isEmpty() {
      Observable<K> keys = readOnly.keys();
      NotEmptySubscriber<K> subs = new NotEmptySubscriber<>();
      keys.subscribe(subs); // Wait mode is BLOCKING, so will block until completed
      return subs.isEmpty;
   }

   private static final class NotEmptySubscriber<T> extends Subscriber<T> {
      boolean isEmpty = true;
      @Override public void onNext(T t) {
         isEmpty = false;
         this.unsubscribe();
      }
   }

   @Override
   public boolean containsKey(Object key) {
      return await(readOnly.eval(toK(key), e -> e.find().isPresent()));
   }

   @Override
   public boolean containsValue(Object value) {
      Observable<Value<V>> values = readOnly.values();
      FindValueSubscriber<V> subs = new FindValueSubscriber<>(value);
      values.subscribe(subs); // Wait mode is BLOCKING, so will block until completed
      return subs.found;
   }

   private static final class FindValueSubscriber<V> extends Subscriber<Value<V>> {
      final Object valueToFind;
      boolean found = false;

      private FindValueSubscriber(Object valueToFind) {
         this.valueToFind = valueToFind;
      }

      @Override public void onNext(Value<V> t) {
         if (valueToFind.equals(t.get())) {
            found = true;
            this.unsubscribe();
         }
      }
   }

   @Override
   public V get(Object key) {
      return await(readOnly.eval(toK(key), e -> e.find().orElse(null)));
   }

   @SuppressWarnings("unchecked")
   private K toK(Object key) {
      return (K) key;
   }

   @SuppressWarnings("unchecked")
   private V toV(Object value) {
      return (V) value;
   }

   @Override
   public V put(K key, V value) {
      return await(readWrite.eval(toK(key), value, (v, rw) -> {
         V prev = rw.find().orElse(null);
         rw.set(v);
         return prev;
      }));
   }

   @Override
   public V remove(Object key) {
      return await(readWrite.eval(toK(key), v -> {
         V prev = v.find().orElse(null);
         v.remove();
         return prev;
      }));
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> m) {
      Observable<Void> obs = writeOnly.evalMany(m, (ev, v) -> v.set(ev));
      obs.subscribe(Observers.noop()); // Wait mode is BLOCKING, so will block until completed
   }

   @Override
   public void clear() {
      await(writeOnly.truncate());
   }

   @Override
   public Set<K> keySet() {
      Observable<K> keys = readOnly.keys();
      Set<K> set = new HashSet<>();
      keys.subscribe(set::add); // Wait mode is BLOCKING, so will block until completed
      return set;
   }

   @Override
   public Collection<V> values() {
      Observable<Value<V>> values = readOnly.values();
      Collection<V> c = new ArrayList<>();
      values.subscribe(v -> c.add(v.get())); // Wait mode is BLOCKING, so will block until completed
      return c;
   }

   @Override
   public Set<Entry<K, V>> entrySet() {
      Observable<Tuple<K, Value<V>>> entries = readOnly.entries();
      Set<Entry<K, V>> set = new HashSet<>();
      entries.subscribe(kv -> set.add(new Entry<K, V>() {
         @Override
         public K getKey() {
            return kv.a();
         }

         @Override
         public V getValue() {
            return kv.b().get();
         }

         @Override
         public V setValue(V value) {
            V prev = kv.b().get();
            writeOnly.eval(kv.a(), value, (v, wo) -> wo.set(v));
            return prev;
         }

         @Override
         public boolean equals(Object o) {
            if (o == this)
               return true;
            if (o instanceof Map.Entry) {
               Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
               if (Objects.equals(kv.a(), e.getKey()) &&
                  Objects.equals(kv.b().get(), e.getValue()))
                  return true;
            }
            return false;
         }

         @Override
         public int hashCode() {
            return kv.hashCode();
         }
      }));

      return set;
   }

   @Override
   public V putIfAbsent(K key, V value) {
      return await(readWrite.eval(toK(key), value, (v, rw) -> {
         Optional<V> opt = rw.find();
         V prev = opt.orElse(null);
         if (!opt.isPresent())
            rw.set(v);

         return prev;
      }));
   }

   @Override
   public boolean remove(Object key, Object value) {
      return await(readWrite.eval(toK(key), toV(value), (v, rw) -> rw.find().map(prev -> {
         if (prev.equals(value)) {
            rw.remove();
            return true;
         }

         return false;
      }).orElse(false)));
   }

   @Override
   public boolean replace(K key, V oldValue, V newValue) {
      return await(readWrite.eval(toK(key), newValue, (v, rw) -> rw.find().map(prev -> {
         if (prev.equals(oldValue)) {
            rw.set(v);
            return true;
         }
         return false;
      }).orElse(false)));
   }

   @Override
   public V replace(K key, V value) {
      return await(readWrite.eval(toK(key), value, (v, rw) -> rw.find().map(prev -> {
         rw.set(v);
         return prev;
      }).orElse(null)));
   }

   public static <T> T await(CompletableFuture<T> cf) {
      try {
         return cf.get();
      } catch (InterruptedException | ExecutionException e) {
         throw new Error(e);
      }
   }

}

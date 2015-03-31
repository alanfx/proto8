package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.FunctionalCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.infinispan.api.v8.Mode.AccessMode.WRITE_ONLY;

public class QueueDecorator<E> implements Queue<E> {

   final FunctionalCollection<E> c;

   public QueueDecorator(FunctionalCollection<E> collection) {
      this.c = collection;
   }

   @Override
   public int size() {
      return await(c.fold(null, 0, (e, t) -> t + 1));
   }

   @Override
   public boolean isEmpty() {
      // Finishes early, as soon as an entry is found
      return !await(c.search(null, (e) -> true)).isPresent();
   }

   @Override
   public boolean contains(Object o) {
      return !await(c.search(null, (e) -> e.equals(o) ? true : null)).isPresent();
   }

   @Override
   public Iterator<E> iterator() {
      return c.stream().iterator();
   }

   @Override
   public Object[] toArray() {
      // TODO: Could be done more efficiently passing in an array? You'd need a Tuple to carry the index as well as the array itself
      return await(c.fold(null, new ArrayList<>(), (e, array) -> {
         array.add(e);
         return array;
      })).toArray();
   }

   @Override
   public <T> T[] toArray(T[] a) {
      // TODO: Could be done more efficiently passing in an array? You'd need a Tuple to carry the index as well as the array itself
      ArrayList<Object> objArray = await(c.fold(null, new ArrayList<>(), (e, array) -> {
         array.add(e);
         return array;
      }));

      return (T[]) Arrays.copyOf(objArray.toArray(), objArray.size(), a.getClass());
   }

   @Override
   public boolean add(E e) {
      return await(c.eval(WRITE_ONLY, v -> {
         v.set(e);
         return true;
      }));
   }

   @Override
   public boolean remove(Object o) {
      return await(c.eval(WRITE_ONLY, v -> {
         v.set(e);
         return true;
      }));
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public void clear() {
      // TODO: Customise this generated block
   }

   @Override
   public boolean equals(Object o) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public int hashCode() {
      return 0;  // TODO: Customise this generated block
   }

   @Override
   public boolean offer(E e) {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public E remove() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public E poll() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public E element() {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public E peek() {
      return null;  // TODO: Customise this generated block
   }

   public static <T> T await(CompletableFuture<T> cf) {
      try {
         // FIXME: Should be timed...
         return cf.get();
      } catch (InterruptedException | ExecutionException e) {
         throw new Error(e);
      }
   }

}

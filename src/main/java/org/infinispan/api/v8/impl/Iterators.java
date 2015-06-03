package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.CloseableIterator;
import org.infinispan.api.v8.Traversable;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Iterators {

   /**
    * Provide a lazily evaluated closeable iterator for a stream.
    */
   public static <T> CloseableIterator<T> of(Stream<T> stream) {
      return new StreamCloseableIterator<>(stream);
   }

   /**
    * Provide an eagerly evaluated closeable iterator for a stream.
    * By eagerly evaluating the stream, it can fully consumed in advance,
    * and the closeable iterator returned is simply an iterator over the
    * already produced streamed results.
    */
   public static <T> CloseableIterator<T> eager(Stream<T> stream) {
      List<T> list = stream.collect(Collectors.toList());
      return new StreamCloseableIterator<>(list.stream());
   }

   private Iterators() {
      // Cannot be instantiated, it's just a holder class
   }

   private static final class StreamCloseableIterator<E> implements CloseableIterator<E> {
      volatile boolean isClosed = false;
      final Iterator<E> it;

      private StreamCloseableIterator(Stream<E> stream) {
         this.it = stream.iterator();
      }

      @Override
      public boolean hasNext() {
         return !isClosed && it.hasNext();
      }

      @Override
      public E next() {
         if (isClosed)
            throw new NoSuchElementException("Iterator closed");

         return it.next();
      }

      @Override
      public void close() {
         isClosed = true;
      }
   }

}

package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.CloseableIterator;
import org.infinispan.api.v8.EntryView;
import org.infinispan.api.v8.EntryView.ReadEntryView;
import org.infinispan.api.v8.Traversable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class Traversables {

   public static <T> Traversable<T> of(Stream<T> stream) {
      return new StreamTraversable<>(stream);
   }

   private Traversables() {
      // Cannot be instantiated, it's just a holder class
   }

   // Attention! This is a very rudimentary/simplistic implementation!
   private static final class StreamTraversable<T> implements Traversable<T> {
      final Stream<T> stream;

      private StreamTraversable(Stream<T> stream) {
         this.stream = stream;
      }

      @Override
      public Traversable<T> filter(Predicate<? super T> p) {
         return new StreamTraversable<>(stream.filter(p));
      }

      @Override
      public <R> Traversable<R> map(Function<? super T, ? extends R> f) {
         return new StreamTraversable<>(stream.map(f));
      }

      @Override
      public <R> Traversable<R> flatMap(Function<? super T, ? extends Traversable<? extends R>> f) {
         Function<? super T, ? extends Stream<? extends R>> mapper = new Function<T, Stream<? extends R>>() {
            @Override
            public Stream<? extends R> apply(T t) {
               StreamTraversable<? extends R> applied = (StreamTraversable<? extends R>) f.apply(t);
               return applied.stream;
            }
         };

         return new StreamTraversable<>(stream.flatMap(mapper));
      }

      @Override
      public Traversable<T> peek(Consumer<? super T> c) {
         return new StreamTraversable<>(stream.peek(c));
      }

      @Override
      public void forEach(Consumer<? super T> c) {
         stream.forEach(c);
      }

      @Override
      public T reduce(T z, BinaryOperator<T> folder) {
         return stream.reduce(z, folder);
      }

      @Override
      public Optional<T> reduce(BinaryOperator<T> folder) {
         return stream.reduce(folder);
      }

      @Override
      public <U> U reduce(U z, BiFunction<U, ? super T, U> mapper, BinaryOperator<U> folder) {
         return stream.reduce(z, mapper, folder);
      }

      @Override
      public <R> R collect(Supplier<R> s, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
         return stream.collect(s, accumulator, combiner);
      }

      @Override
      public Optional<T> min(Comparator<? super T> comparator) {
         return stream.min(comparator);
      }

      @Override
      public Optional<T> max(Comparator<? super T> comparator) {
         return stream.max(comparator);
      }

      @Override
      public long count() {
         return stream.count();
      }

      @Override
      public boolean anyMatch(Predicate<? super T> p) {
         return stream.anyMatch(p);
      }

      @Override
      public boolean allMatch(Predicate<? super T> p) {
         return stream.allMatch(p);
      }

      @Override
      public boolean noneMatch(Predicate<? super T> predicate) {
         return stream.noneMatch(predicate);
      }

      @Override
      public Optional<T> findAny() {
         return stream.findAny();
      }

      @Override
      public CloseableIterator<T> iterator() {
         return Iterators.of(stream);
      }
   }

}

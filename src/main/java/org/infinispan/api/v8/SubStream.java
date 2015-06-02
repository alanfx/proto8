package org.infinispan.api.v8;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * A subset of the {@link java.util.stream.Stream} API. The following
 * {@link java.util.stream.Stream} operations are not included:
 *
 * <ul>
 *    <li>{@link java.util.stream.Stream#distinct()}: This operation would be
 *    extremely costly in a distributed environment as all data up to that
 *    point of the stream must be read by a single node. Due to the memory and
 *    performance constraints this method wouldn't be supported.
 *    </li>
 *    <li>{@link java.util.stream.Stream#sorted()}: Expensive?
 *    </li>
 *    <li>{@link java.util.stream.Stream#limit(long)}: In a distributed
 *    environment this would require sequential requests for each node to
 *    verify that we haven't reached the limit yet. This could be optimized
 *    depending on the terminator. This operation Makes most sense when
 *    combined with sorted streams.
 *    </li>
 *    <li>{@link java.util.stream.Stream#sorted()}: Operation that makes
 *    most sense when combined with sorting.
 *    </li>
 *    <li>{@link java.util.stream.Stream#forEachOrdered(Consumer)}:
 *    Sorting related.
 *    </li>
 *    <li>{@link java.util.stream.Stream#toArray()}: Creating an array
 *    requires size to be computed first, and then populate it, making it a
 *    potentially expensive operation in a distributed operation. Also,
 *    there are other ways to achieve this, such as mapping to a variable
 *    length collection and then narrowing it down to an array?</li>
 * </ul>
 *
 * @param <T>
 */
public interface SubStream<T> {

   // TODO: In distributed environments, where the lambdas passed are executed could matter a lot:
   // For example, for filtering, the predicate would be better run directly
   // in the source of data, rather than bring all data and filter it locally.
   // If the default would to run lambdas at the data source, there's a couple
   // of things considering:
   //    1. How to marshall a predicate without forcing the predicate to be Serializable.
   //       SerializedLambda exposes how serialization works, we'd need to apply a
   //       similar thing caching as much as we can.
   //    2. Have a way to tweak lambda executions to happen locally instead of
   //       at data source. This would be useful for operations such as
   //       peek() and for each. This could easily be done with a new Param.
   //       This option is also handy for situations where the lambda captures
   //       objects that simply cannot be marshalled.

   SubStream<T> filter(Predicate<? super T> p);

   <R> SubStream<R> map(Function<? super T, ? extends R> f);

   <R> SubStream<R> flatMap(Function<? super T, ? extends SubStream<? extends R>> f);

   SubStream<T> peek(Consumer<? super T> c);

   void forEach(Consumer<? super T> c);

   T reduce(T z, BinaryOperator<T> folder);

   Optional<T> reduce(BinaryOperator<T> folder);

   <U> U reduce(U z, BiFunction<U, ? super T, U> mapper,  BinaryOperator<U> folder);

   <R> R collect(Supplier<R> s, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner);

   <R, A> R collect(Collector<? super T, A, R> collector);

   Optional<T> min(Comparator<? super T> comparator);

   Optional<T> max(Comparator<? super T> comparator);

   long count();

   boolean anyMatch(Predicate<? super T> p);

   boolean allMatch(Predicate<? super T> p);

   boolean noneMatch(Predicate<? super T> predicate);

   Optional<T> findFirst();

   Optional<T> findAny();

//   private EntryStream() {
//      // Cannot be instantiated, it's just a holder class
//   }
//
//   public interface ReadStream {
//      Stream<T> filter(Predicate<? super T> predicate);
//   }



}

package org.infinispan.api.v8;

import org.infinispan.api.v8.Closeables.CloseableIterator;
import org.infinispan.api.v8.impl.Iterators;
import org.infinispan.api.v8.impl.Traversables;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An easily extensible parameter that allows functional map operations to be
 * tweaked. Apart from {@link org.infinispan.api.v8.Param.WaitMode}, examples
 * would include local-only parameter, skip-cache-store parameter and others.
 *
 * What makes {@link Param} different from {@link MetaParam} is that {@link Param}
 * values are never stored in the functional map. They merely act as ways to
 * tweak how operations are executed.
 *
 * Since {@link Param} instances control how the internals work, only
 * {@link Param} implementations by Infinispan will be supported.
 *
 * DESIGN RATIONALES:
 * <ul>
 *    <il>This interface is equivalent to Infinispan's Flag, but it's more powerful
 *    because it allows to pass a flag along with a value. Infinispan's Flag
 *    are enum based which means no values can be passed along with value.</il>
 * </ul>
 *
 * @param <P> type of parameter
 */
public interface Param<P> {

   /**
    * A parameter's identifier. Each parameter must have a different id.
    *
    * DESIGN RATIONALES:
    * <ul>
    *    <il>Why does a Param need an id? The most efficient way to store
    *    multiple parameters is to keep them in an array. An integer-based id
    *    means it can act as index of the array.</il>
    * </ul>
    */
   int id();

   /**
    * Parameter's value.
    */
   P get();

   /**
    * Wait mode controls whether the functional operation is blocking or
    * not blocking.
    *
    * By default, functional map operations are non-blocking,
    * so for {@link CompletableFuture} returns, these be asynchronously
    * completed, and for {@link Traversable} or {@link CloseableIterator}
    * returns, they will be computed without waiting for all the results
    * to be available.
    *
    * If blocking, functional map operations will block
    * until the operations are completed. So, when an operation returns a
    * {@link CompletableFuture}, it'll already be completed. For operations
    * that {@link Traversable} or {@link CloseableIterator}, these will
    * already be pre-computed and the navigation will happen over the
    * already computed values.
    */
   enum WaitMode implements Param<WaitMode> {
      BLOCKING {
         @Override
         public WaitMode get() {
            return BLOCKING;
         }
      }, NON_BLOCKING {
         @Override
         public WaitMode get() {
            return NON_BLOCKING;
         }
      };

      public static final int ID = 0;

      @Override
      public int id() {
         return ID;
      }

      /**
       * Provides default wait mode.
       */
      public static WaitMode defaultValue() {
         return NON_BLOCKING;
      }

      public static <T> CompletableFuture<T> withWaitFuture(Param<WaitMode> waitParam, Supplier<T> s) {
         switch (waitParam.get()) {
            case BLOCKING:
               // If blocking, complete the future directly with the result.
               // No separate thread or executor is instantiated.
               return CompletableFuture.completedFuture(s.get());
            case NON_BLOCKING:
               // If non-blocking execute the supply function asynchronously,
               // and return a future that's completed when the supply
               // function returns.
               return CompletableFuture.supplyAsync(s);
            default:
               throw new IllegalStateException();
         }
      }

      public static <T> Traversable<T> withWaitTraversable(Param<WaitMode> waitParam, Supplier<Stream<T>> s) {
         switch (waitParam.get()) {
            case BLOCKING:
               return Traversables.eager(s.get());
            default:
               throw new IllegalStateException("Not yet implemented");
         }
      }

      public static <T> CloseableIterator<T> withWaitIterator(Param<WaitMode> waitParam, Supplier<Stream<T>> s) {
         switch (waitParam.get()) {
            case BLOCKING:
               return Iterators.eagerIterator(s.get());
            default:
               throw new IllegalStateException("Not yet implemented");
         }
      }

   }

}

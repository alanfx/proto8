package org.infinispan.api.v8;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * An easily extensible parameter that allows functional map operations to be
 * tweaked. Apart from {@link org.infinispan.api.v8.Param.WaitMode}, examples
 * would include local-only parameter, skip-cache-store parameter and others.
 *
 * What makes {@link Param} different to {@link MetaParam} is that {@link Param}
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
    * not blocking. By default, functional map operations are non-blocking,
    * which means that they return either a {@link CompletableFuture} for
    * single-value returns, or a {@link Observable} for multi-value returns,
    * and when the result(s) are available, they make callbacks onto the
    * returned values. If blocking, functional map operations will block
    * until the operations are completed, and the {@link CompletableFuture}
    * or {@link Observable} will contain operation result(s).
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

      public static <T> CompletableFuture<T> withWaitMode(WaitMode waitMode, Supplier<T> s) {
         switch (waitMode) {
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
   }

}

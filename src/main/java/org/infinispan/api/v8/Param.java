package org.infinispan.api.v8;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface Param<P> {

   int id();
   P get();

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

      public static WaitMode defaultValue() {
         return NON_BLOCKING;
      }

      public static <T> CompletableFuture<T> withWaitMode(WaitMode waitMode, Supplier<T> s) {
         switch (waitMode) {
            case BLOCKING:
               return CompletableFuture.completedFuture(s.get());
            case NON_BLOCKING:
               return CompletableFuture.supplyAsync(s);
            default:
               throw new IllegalStateException();
         }
      }
   }

}

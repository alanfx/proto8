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

   enum StreamModes {
      // TODO: Add more possibilities: SEGMENT, MACHINE, RACK, SITE...
      KEYS, VALUES
   }

   final class StreamMode implements Param<EnumSet<StreamModes>> {
      public static final int ID = 1;
      private static final StreamMode DEFAULT = StreamMode.of(StreamModes.KEYS);

      private final EnumSet<StreamModes> streamModes;

      private StreamMode(EnumSet<StreamModes> streamModes) {
         this.streamModes = streamModes;
      }

      @Override
      public int id() {
         return ID;
      }

      @Override
      public EnumSet<StreamModes> get() {
         return streamModes;
      }

      public static StreamMode defaultValue() {
         return DEFAULT;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         StreamMode that = (StreamMode) o;

         return !(streamModes != null ? !streamModes.equals(that.streamModes) : that.streamModes != null);

      }

      @Override
      public int hashCode() {
         return streamModes != null ? streamModes.hashCode() : 0;
      }

      @Override
      public String toString() {
         return "StreamModes=" + streamModes;
      }

      public static StreamMode of(StreamModes mode) {
         return new StreamMode(EnumSet.of(mode));
      }

      public static StreamMode of(StreamModes mode, StreamModes... modes) {
         return new StreamMode(EnumSet.of(mode, modes));
      }
   }

}

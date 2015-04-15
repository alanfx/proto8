package org.infinispan.api.v8;

import java.util.EnumSet;

public interface Param<P> {

   int id();
   P get();

   // TODO: Add blocking param

   enum AccessMode implements Param<AccessMode> {
      READ_ONLY {
         @Override
         public AccessMode get() {
            return READ_ONLY;
         }
      }, READ_WRITE {
         @Override
         public AccessMode get() {
            return READ_WRITE;
         }
      }, WRITE_ONLY {
         @Override
         public AccessMode get() {
            return WRITE_ONLY;
         }
      };

      public static final int ID = 0;

      public boolean isWrite() {
         return this == READ_WRITE || this == WRITE_ONLY;
      }

      @Override
      public int id() {
         return ID;
      }

      public static AccessMode defaultValue() {
         return READ_ONLY;
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

      public static final int ID = 2;

      @Override
      public int id() {
         return ID;
      }

      public static WaitMode defaultValue() {
         return NON_BLOCKING;
      }
   }

   final class Lifespan implements Param<Long> {
      public static final int ID = 3;
      private static final Lifespan DEFAULT = new Lifespan(-1);

      private final long lifespan;

      public Lifespan(long lifespan) {
         this.lifespan = lifespan;
      }

      @Override
      public int id() {
         return ID;
      }

      @Override
      public Long get() {
         return lifespan;
      }

      public static Lifespan defaultValue() {
         return DEFAULT;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         Lifespan lifespan1 = (Lifespan) o;

         return lifespan == lifespan1.lifespan;

      }

      @Override
      public int hashCode() {
         return (int) (lifespan ^ (lifespan >>> 32));
      }

      @Override
      public String toString() {
         return "Lifespan=" + lifespan;
      }
   }



}

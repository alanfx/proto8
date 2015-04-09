package org.infinispan.api.v8;

public interface Param<P> {

   int id();
   P get();

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

   final class Lifespan implements Param<Long> {
      public static final int ID = 1;
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

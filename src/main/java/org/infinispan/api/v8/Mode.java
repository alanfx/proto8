package org.infinispan.api.v8;

public interface Mode {

   public static enum   AccessMode implements Mode {
      READ_ONLY, READ_WRITE, WRITE_ONLY;

      public boolean isWrite() {
         return this == READ_WRITE || this == WRITE_ONLY;
      }
   }

   public static enum StreamMode implements Mode {
      // FIXME: Add a NONE stream mode for when no keys nor values are needed? e.g. size counting
      KEYS_ONLY, VALUES_ONLY, KEYS_AND_VALUES

      // Other interesting stream modes: SEGMENT, RACK, MACHINE
   }

//   public static enum WaitMode implements Mode {
//      BLOCKING, NON_BLOCKING
//   }
//
////   public static class Expiration implements Mode {
////      static Expiration lifespan(long l) {
////         return new Expiration(l);
////      }
////   }

}

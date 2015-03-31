package org.infinispan.api.v8;

public interface Param<P> {

   public static enum LocalOnly implements Param<Boolean> {
      LOCAL_ONLY;
   }

   public static class Lifespan implements Param<Long> {
      final long lifespan;

      public Lifespan(long lifespan) {
         this.lifespan = lifespan;
      }
   }

}

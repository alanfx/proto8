package org.infinispan.api.v8.util;

public class Tuples {

   public static <A, B> Tuple<A, B> of(A a, B b) {
      return new TupleImpl<>(a, b);
   }

   private static final class TupleImpl<A, B> implements Tuple<A, B> {
      private final A a;
      private final B b;

      private TupleImpl(A a, B b) {
         this.a = a;
         this.b = b;
      }

      @Override
      public A a() {
         return a;
      }

      @Override
      public B b() {
         return b;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         TupleImpl<?, ?> pair = (TupleImpl<?, ?>) o;

         if (!a.equals(pair.a)) return false;
         return b.equals(pair.b);

      }

      @Override
      public int hashCode() {
         int result = a.hashCode();
         result = 31 * result + b.hashCode();
         return result;
      }

      @Override
      public String toString() {
         return "(" + a + "," + b + ")";
      }
   }

}

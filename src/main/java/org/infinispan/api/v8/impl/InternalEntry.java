package org.infinispan.api.v8.impl;

final class InternalEntry<V> {
   final V value;
   final MetaParams metaParams;

   InternalEntry(V value, MetaParams metaParams) {
      this.value = value;
      this.metaParams = metaParams;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      InternalEntry<?> that = (InternalEntry<?>) o;

      if (!value.equals(that.value)) return false;
      return metaParams.equals(that.metaParams);

   }

   @Override
   public int hashCode() {
      int result = value.hashCode();
      result = 31 * result + metaParams.hashCode();
      return result;
   }

   @Override
   public String toString() {
      return "InternalEntry{" +
         "value=" + value +
         ", metaParams=" + metaParams +
         '}';
   }
}

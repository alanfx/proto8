package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.MetaParam;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Internal entry holder for value and metadata parameters. This is not exposed
 * externally to users. Value and metadata parameters are exposed via the
 * different entry view facades.
 */
final class InternalEntry<V> implements MetaParam.Lookup {
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

   @Override
   public <T> T getMetaParam(MetaParam.Id<T> id) throws NoSuchElementException {
      return metaParams.get(id);
   }

   @Override
   public <T> Optional<T> findMetaParam(MetaParam.Id<T> id) {
      return metaParams.find(id);
   }

}

package org.infinispan.api.v8.impl;

import net.jcip.annotations.NotThreadSafe;
import org.infinispan.api.v8.MetaParam;
import org.infinispan.api.v8.MetaParam.Id;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * TODO: Why id cannot be used as index
 * TODO: Why sequential lookup is just fine...
 */
@NotThreadSafe
final class MetaParams {

   private MetaParam<?>[] metas;

   private MetaParams(MetaParam<?>[] metas) {
      this.metas = metas;
   }

   boolean isEmpty() {
      return metas.length == 0;
   }

   int size() {
      return metas.length;
   }

   <T> Optional<T> find(Id<T> id) {
      return Optional.ofNullable(findNullable(id));
   }

   <T> T get(Id<T> id) throws NoSuchElementException {
      T param = findNullable(id);
      if (param == null)
         throw new NoSuchElementException("Metadata with id=" + id + " not found");

      return param;
   }

   @SuppressWarnings("unchecked")
   private <T> T findNullable(Id<T> id) {
      for (MetaParam<?> meta : metas) {
         if (meta.id().equals(id))
            return (T) meta;
      }

      return null;
   }

   void add(MetaParam.Writable meta) {
      if (metas.length == 0)
         metas = new MetaParam[]{meta};
      else {
         boolean found = false;
         for (int i = 0; i < metas.length; i++) {
            if (metas[i].id().equals(meta.id())) {
               metas[i] = meta;
               found = true;
            }
         }

         if (!found) {
            MetaParam<?>[] newMetas = Arrays.copyOf(metas, metas.length + 1);
            newMetas[newMetas.length - 1] = meta;
            metas = newMetas;
         }
      }
   }

   void addMany(MetaParam.Writable... metaParams) {
      if (metas.length == 0) metas = metaParams;
      //else if (metas.length == 1) add(metaParams[0]);
      else {
         List<MetaParam<?>> notFound = new ArrayList<>(metaParams.length);
         for (MetaParam.Writable newMeta : metaParams) {
            boolean found = false;
            for (int i = 0; i < metas.length; i++) {
               if (metas[i].id().equals(newMeta.id())) {
                  metas[i] = newMeta;
                  found = true;
               }
            }
            if (!found)
               notFound.add(newMeta);
         }

         if (!notFound.isEmpty()) {
            List<MetaParam<?>> allMetasList = new ArrayList<>(Arrays.asList(metas));
            allMetasList.addAll(notFound);
            metas = allMetasList.toArray(new MetaParam[metas.length + notFound.size()]);
         }
      }
   }

   static MetaParams of(MetaParam... metas) {
      return new MetaParams(metas);
   }

   static MetaParams of(MetaParam meta) {
      return new MetaParams(new MetaParam[]{meta});
   }

   static MetaParams empty() {
      return new MetaParams(new MetaParam[]{});
   }

}

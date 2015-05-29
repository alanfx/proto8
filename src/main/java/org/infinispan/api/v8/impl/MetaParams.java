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
 * Represents a {@link MetaParam} collection.
 *
 * DESIGN RATIONALES:
 * <ul>
 *    <il>In {@link org.infinispan.api.v8.impl.Params}, the internal array
 *    where each parameter was stored was indexed by
 *    {@link org.infinispan.api.v8.Param#id()}. This worked fine because the
 *    available parameters are exclusively controlled by the Infinispan.
 *    This is not the case with {@link org.infinispan.api.v8.MetaParam}
 *    instances where we expect users to add their own types.
 *    For MetaParams, an array is still used but each metadata parameters
 *    index has nothing to do with a metadata parameter's id. So, that means
 *    when looking up metadata parameters, the lookup is sequential.
 *    </il>
 *    <il>Metadata parameter lookup is sequential, which is O(n), isn't that a problem?
 *    Not really, we expect that the number of metadata parameters to be stored
 *    along with a cached entry to be small, less than 10 metadata parameters
 *    per collection. So, looking up a metadata parameter array sequentially would
 *    have a small impact performance wise.
 *    </il>
 *    <li>Why are you obsessed with storing Metadata parameters within an array?
 *    Because we want metadata parameter storage to consume as little memory
 *    as possible while retaining flexibility when adding/removing new metadata
 *    parameters. We want to consume as little memory as possible because each
 *    cached entry will have a reference to the metadata parameters.
 *    </li>
 *    <li>Why is metadata parameters class not thread safe? Because we expect
 *    any updates to it to acquire write locks on the entire
 *    {@link InternalEntry} which references the
 *    metadata parameters collection, and hence any updates could be done
 *    without the need to keep metadata parameters concurrently safe. Also,
 *    remember that metadata parameters is internal only. Users can retrieve
 *    or update individual metadata parameters but they cannot act on the
 *    globally at the metadata parameter collection level.</li>
 * </ul>
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

package org.infinispan.api.v8;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Entry views expose cached entry information to the user. Depending on the
 * type of entry view, different operations are available. Currently, three
 * type of entry views are supported:
 *
 * <ul>
 *    <il>{@link ReadEntryView}: read-only entry view</il>
 *    <il>{@link WriteEntryView}: write-only entry view</il>
 *    <il>{@link ReadWriteEntryView}: read-write entry view</il>
 * </ul>
 */
public class EntryView {

   /**
    * Expose read-only information about a cache entry potentially associated
    * with a key in the functional map. Typically, if the key is associated
    * with a cache entry, that information will include value and optional
    * {@link MetaParam} information.
    *
    * DESIGN RATIONALES:
    * <ul>
    *    <li>Why does ReadEntryView expose both get() and find() methods for
    *    retrieving the value? Convenience. If the caller knows for sure
    *    that the value will be present, get() offers the convenience of
    *    retrieving the value directly without having to get an {@link Optional}
    *    first.
    *    </li>
    *    <li>Why have find() return {@link Optional}? Why not get rid of it and only have
    *    get() method return null? Because nulls are evil. If a value might not
    *    be there, the user can use {@link Optional} to find out if the value might
    *    be there and deal with non-present values in a more functional way.
    *    </li>
    *    <li>Why does get() throw NoSuchElementException? Because you should only
    *    use it if you know for sure that the value will be there. If unsure,
    *    use find(). We don't want to return null to avoid people doing null checks.
    *    </li>
    * </ul>
    */
   public interface ReadEntryView<K, V> extends MetaParam.Lookup {
      /**
       * Key of the read-only entry view. Guaranteed to return a non-null value.
       */
      K key();

      /**
       * Returns a non-null value if the key has a value associated with it or
       * throws {@link NoSuchElementException} if no value is associated with
       * the.
       *
       * @throws NoSuchElementException if no value is associated with the key.
       */
      V get() throws NoSuchElementException;

      /**
       * Optional value. It'll return a non-empty value when the value is present,
       * and empty when the value is not present.
       */
      Optional<V> find();
   }

   public interface WriteEntryView<V> {
      /**
       * Set this value.
       *
       * It returns 'Void' instead of 'void' in order to avoid, as much as possible,
       * the need to add `Consumer` overloaded methods in FunctionalCache.
       */
      Void set(V value, MetaParam.Writable... metas);

      /**
       * Removes the value.
       *
       * Instead of creating set(Optional<V>...), add a simple remove() method that
       * removes the value. This feels cleaner and less cumbersome than having to
       * always pass in Optional to set()
       */
      Void remove();
   }

   public interface ReadWriteEntryView<K, V> extends ReadEntryView<K, V>, WriteEntryView<V> {}

}

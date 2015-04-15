package org.infinispan.api.v8;

import java.io.InputStream;
import java.util.concurrent.locks.Lock;
import org.infinispan.api.v8.impl.FunctionalMapImpl;
//import org.infinispan.api.v8.impl.MapDecorator;

/**
 * Infinispan. This class represents the main entry point for creating Infinispan objects
 * (Caches, Locks, etc) and for managing common resources. An Infinispan instance can only
 * be created through a set of builder methods which provide the initial configuration.
 * Typically an Infinispan instance creates and manages all of the components it needs based
 * on the configuration, however it is possible for the user to provide such objects manually,
 * e.g. so that they can be shared between multiple Infinispan instances or so that they can be
 * constructed in a special way.
 *
 *
 * @author Galder Zamarreño
 * @author Tristan Tarrant
 *
 * @since 8.0
 */
public class Infinispan implements AutoCloseable {

   // Needs to be instantiated by builder
   private Infinispan() {
   }

   // Builder methods
   public static Infinispan fromConfiguration(String fileName) { return null; }
   public static Infinispan fromConfiguration(InputStream resourceName) { return null; }
   public static Infinispan fromConfiguration(Configuration configuration) { return null; }

   // Component override
   public Infinispan override(Class<?>... component) { return this; }

   // Lifecycle methods
   public Infinispan start() { return this; }
   public Infinispan stop() { return this; }
   @Override
   public void close() throws Exception {
      stop();
   }

   // Obtain named Infinispan objects

   /**
    * Retrieves a named cache using the {@link Cache} interface
    *
    * @param name
    * @return
    */
   public <K, V> Cache<K, V> cache(String name) { return null; }
   /**
    *
    * Retrieves a named cache using the {@link FunctionalMap} interface
    *
    * @param name
    * @return
    */
   public <K, V> FunctionalMap<K, V> functionalCache(String name) { return null; }
   /**
    * Retrieves a named cache using the JSR-107 {@link javax.cache.Cache} interface
    *
    * @param name
    * @return
    */
   public <K, V> javax.cache.Cache<K, V> jcache(String name) { return null; }
   /**
    * Retrieves a named remote cache interface
    *
    * @param name
    * @return
    */
   public <K, V> RemoteCache<K, V> remoteCache(String name) { return null; }

   public <E> Queue<E> queue(String name) { return null; }
   public Counter counter(String name) { return null; }
   public Lock lock(String name) { return null; }
   public Topic topic(String name) { return null; }

   // Static Service methods

   /**
    * Retrieves the Infinispan object which has created the specified object
    * @param object an object created by Infinispan (cache, queue, etc)
    * @return
    */
   public static Infinispan getOwner(Object object) { return null; }
   /**
    * Retrieves a specific component from an Infinispan object
    *
    * @return
    */
   public static <T> T getComponent(Object object, Class<T> componentClass) { return null; }

}

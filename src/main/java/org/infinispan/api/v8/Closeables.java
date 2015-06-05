package org.infinispan.api.v8;

import java.util.Iterator;
import java.util.Spliterator;

public class Closeables {

   private Closeables() {
      // Cannot be instantiated, it's just a holder class
   }

   /**
    * Auto closeable iterator. Being able to close an iterator is useful
    * for situations where the iterator might consume considerable resources,
    * e.g. in a distributed environment. Hence, being able to close it allows
    * for resources to be reclaimed if the iterator won't be iterated any more.
    */
   public interface CloseableIterator<T> extends Iterator<T>, AutoCloseable {
      @Override void close();
   }

   /**
    * Auto closeable spliterator. Being able to close an spliterator is useful
    * for situations where the iterator might consume considerable resources,
    * e.g. in a distributed environment. Hence, being able to close it allows
    * for resources to be reclaimed if the iterator won't be iterated any more.
    */
   public interface CloseableSpliterator<T> extends Spliterator<T>, AutoCloseable {
      @Override void close();
   }

}

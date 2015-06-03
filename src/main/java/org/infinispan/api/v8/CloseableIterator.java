package org.infinispan.api.v8;

import java.util.Iterator;

/**
 * Auto closeable iterator.
 */
public interface CloseableIterator<E> extends Iterator<E>, AutoCloseable {
   @Override void close();
}

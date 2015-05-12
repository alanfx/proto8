package org.infinispan.api.v8;

public interface Value<V> extends MetaParam.Lookup {

   V get();

}

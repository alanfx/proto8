package java8.proto;

import org.infinispan.api.v8.FunctionalMap;
import org.infinispan.api.v8.Mode;
import org.infinispan.api.v8.Param;
import org.infinispan.api.v8.impl.FunctionalMapImpl;

public class ProtoTest {

   public void testMultipleParams() {
      FunctionalMap<Integer, String> fcache = new FunctionalMapImpl<>();
      fcache.withParams(Param.LocalOnly.LOCAL_ONLY, new Param.Lifespan(1000))
         .eval(1, Mode.AccessMode.WRITE_ONLY, v -> v.set("one"));

   }

}

package org.infinispan.api.v8;

import org.infinispan.api.v8.impl.FunctionalMapImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.infinispan.api.v8.Param.AccessMode.Lifespan;
import static org.infinispan.api.v8.Param.AccessMode.WRITE_ONLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FunctionalMapTest {

   @Test
   public void testGet() throws Exception {
      try(FunctionalMap<Integer, ?> readOnly = FunctionalMapImpl.create()) {
         await(
            readOnly.eval(1, Value::get).thenAccept(v -> assertEquals(v, Optional.empty()))
         );
      }
   }

   @Test
   public void testPut() throws Exception {
      try(FunctionalMap<Integer, String> writeOnly =
             FunctionalMapImpl.<Integer, String>create().withParams(WRITE_ONLY)) {
         await(
            writeOnly.eval(1, v -> v.set("one")).thenAccept(Assert::assertNull)
         );
      }
   }

   @Test
   public void testPutThenGet() throws Exception {
      try(FunctionalMap<Integer, String> readOnly = FunctionalMapImpl.create()) {
         FunctionalMap<Integer, String> writeOnly = readOnly.withParams(WRITE_ONLY);
         await(
            writeOnly.eval(1, v -> v.set("one")).thenCompose(r ->
               readOnly.eval(1, Value::get).thenAccept(v -> {
                     assertNull(r);
                     assertEquals(v, Optional.of("one"));
                  }
               )));
      }
   }

   @Test
   public void testPutWithLifespan() throws Exception {
      try(FunctionalMap<Integer, String> writeOnly =
             FunctionalMapImpl.<Integer, String>create().withParams(WRITE_ONLY)) {
         await(
            writeOnly.withParams(new Lifespan(2000))
               .eval(1, v -> v.set("one")).thenAccept(Assert::assertNull)
         );
      }
   }

//   public void test000() {
//      Infinispan infinispan = new Infinispan();
//      Map<Integer, String> local = infinispan.local(Infinispan.DataStructures.MAP);
//   }

   public static <T> T await(CompletableFuture<T> cf) {
      try {
         return cf.get();
      } catch (InterruptedException | ExecutionException e) {
         throw new Error(e);
      }
   }

}

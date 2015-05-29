package org.infinispan.api.v8;

import org.infinispan.api.v8.EntryVersion.NumericEntryVersion;
import org.infinispan.api.v8.EntryView.ReadEntryView;
import org.infinispan.api.v8.EntryView.ReadWriteEntryView;
import org.infinispan.api.v8.FunctionalMap.ReadOnlyMap;
import org.infinispan.api.v8.FunctionalMap.ReadWriteMap;
import org.infinispan.api.v8.FunctionalMap.WriteOnlyMap;
import org.infinispan.api.v8.MetaParam.EntryVersionParam;
import org.infinispan.api.v8.MetaParam.Lifespan;
import org.infinispan.api.v8.impl.FunctionalMapImpl;
import org.infinispan.api.v8.impl.ReadOnlyMapImpl;
import org.infinispan.api.v8.impl.ReadWriteMapImpl;
import org.infinispan.api.v8.impl.WriteOnlyMapImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.infinispan.api.v8.EntryVersion.CompareResult.EQUAL;
import static org.junit.Assert.*;

/**
 * Test suite for verifying basic functional map functionality,
 * and for testing out functionality that is not available via standard
 * {@link java.util.concurrent.ConcurrentMap} nor {@link javax.cache.Cache}
 * APIs, such as atomic conditional metadata-based replace operations, which
 * are required by Hot Rod.
 */
public class FunctionalMapTest {

   private ReadOnlyMap<Integer, String> readOnlyMap;
   private WriteOnlyMap<Integer, String> writeOnlyMap;
   private ReadWriteMap<Integer, String> readWriteMap;

   @Before
   public void setUp() {
      FunctionalMapImpl<Integer, String> functionalMap = FunctionalMapImpl.create();
      readOnlyMap = ReadOnlyMapImpl.create(functionalMap);
      writeOnlyMap = WriteOnlyMapImpl.create(functionalMap);
      readWriteMap = ReadWriteMapImpl.create(functionalMap);
   }

   /**
    * Read-only allows to retrieve an empty cache entry.
    */
   @Test
   public void testReadOnlyGetsEmpty() {
      await(readOnlyMap.eval(1, ReadEntryView::find).thenAccept(v -> assertEquals(Optional.empty(), v)));
   }

   /**
    * Write-only allows for constant, non-capturing, values to be written,
    * and read-only allows for those values to be retrieved.
    */
   @Test
   public void testWriteOnlyNonCapturingConstantValueAndReadOnlyGetsValue() {
      await(
         writeOnlyMap.eval(1, writeView -> writeView.set("one")).thenCompose(r ->
               readOnlyMap.eval(1, ReadEntryView::get).thenAccept(v -> {
                     assertNull(r);
                     assertEquals("one", v);
                  }
               )
         )
      );
   }

   /**
    * Write-only allows for non-capturing values to be written along with metadata,
    * and read-only allows for both values and metadata to be retrieved.
    */
   @Test
   public void testWriteOnlyNonCapturingValueAndMetadataReadOnlyValueAndMetadata() {
      await(
         writeOnlyMap.eval(1, "one", (v, writeView) -> writeView.set(v, new Lifespan(1000))).thenCompose(r ->
               readOnlyMap.eval(1, ro -> ro).thenAccept(ro -> {
                     assertNull(r);
                     assertEquals(Optional.of("one"), ro.find());
                     assertEquals("one", ro.get());
                     assertEquals(Optional.of(new Lifespan(1000)), ro.findMetaParam(Lifespan.ID));
                     assertEquals(new Lifespan(1000), ro.getMetaParam(Lifespan.ID));
                  }
               )
         )
      );
   }

   /**
    * Read-write allows to retrieve an empty cache entry.
    */
   @Test
   public void testReadWriteGetsEmpty() {
      await(readWriteMap.eval(1, ReadWriteEntryView::find).thenAccept(v -> assertEquals(Optional.empty(), v)));
   }

   /**
    * Read-write allows for constant, non-capturing, values to be written,
    * returns previous value, and also allows values to be retrieved.
    */
   @Test
   public void testReadWriteValuesReturnPreviousAndGet() {
      await(
         readWriteMap.eval(1, readWrite -> {
            Optional<String> prev = readWrite.find();
            readWrite.set("one");
            return prev;
         }).thenCompose(r ->
               readWriteMap.eval(1, ReadWriteEntryView::get).thenAccept(v -> {
                     assertFalse(r.isPresent());
                     assertEquals("one", v);
                  }
               )
         )
      );
   }

   /**
    * Read-write allows for replace operations to happen based on version
    * comparison, and update version information if replace is versions are
    * equals.
    *
    * This is the kind of advance operation that Hot Rod prototocol requires
    * but the current Infinispan API is unable to offer without offering
    * atomicity at the level of the function that compares the version
    * information.
    */
   @Test
   public void testReadWriteAllowsForConditionalParameterBasedReplace() {
      replaceWithVersion(100, rw -> {
            assertEquals("uno", rw.get());
            assertEquals(new EntryVersionParam<>(new NumericEntryVersion(200)),
               rw.getMetaParam(EntryVersionParam.ID()));
         }
      );
      replaceWithVersion(900, rw -> {
         assertEquals(Optional.of("one"), rw.find());
         assertEquals(Optional.of(new EntryVersionParam<>(new NumericEntryVersion(100))),
            rw.findMetaParam(EntryVersionParam.ID()));
      });
   }

   private void replaceWithVersion(long version, Consumer<ReadWriteEntryView<Integer, String>> asserts) {
      await(
         readWriteMap.eval(1, rw -> rw.set("one", new EntryVersionParam<>(new NumericEntryVersion(100)))).thenCompose(r ->
            readWriteMap.eval(1, rw -> {
               EntryVersionParam<Long> versionParam = rw.getMetaParam(EntryVersionParam.ID());
               if (versionParam.get().compareTo(new NumericEntryVersion(version)) == EQUAL)
                  rw.set("uno", new EntryVersionParam<>(new NumericEntryVersion(200)));
               return rw;
            }).thenAccept(rw -> {
                  assertNull(r);
                  asserts.accept(rw);
               }
            )
         )
      );
   }

   @Test
   public void testAutoClose() throws Exception {
      try(ReadOnlyMap<?, ?> ro = ReadOnlyMapImpl.create(FunctionalMapImpl.create())) {
         assertNotNull(ro); // No-op, just verify that it implements AutoCloseable
      }
      try(WriteOnlyMap<?, ?> wo = WriteOnlyMapImpl.create(FunctionalMapImpl.create())) {
         assertNotNull(wo); // No-op, just verify that it implements AutoCloseable
      }
      try(ReadWriteMap<?, ?> rw = ReadWriteMapImpl.create(FunctionalMapImpl.create())) {
         assertNotNull(rw); // No-op, just verify that it implements AutoCloseable
      }
   }

   public static <T> T await(CompletableFuture<T> cf) {
      try {
         return cf.get();
      } catch (InterruptedException | ExecutionException e) {
         throw new Error(e);
      }
   }

}

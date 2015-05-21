package org.infinispan.api.v8;

import org.infinispan.api.v8.EntryView.ReadEntryView;
import org.infinispan.api.v8.EntryView.ReadWriteEntryView;
import org.infinispan.api.v8.EntryView.WriteEntryView;
import org.infinispan.api.v8.Listeners.ReadWriteListeners.ReadWriteListener;
import org.infinispan.api.v8.Listeners.WriteListeners.WriteListener;
import org.infinispan.api.v8.impl.FunctionalMapImpl;
import org.infinispan.api.v8.impl.ReadWriteMapImpl;
import org.infinispan.api.v8.impl.WriteOnlyMapImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class FunctionalMapListenersTest {

   private FunctionalMap.WriteOnlyMap<Integer, String> writeOnlyMap;
   private FunctionalMap.ReadWriteMap<Integer, String> readWriteMap;

   @Before
   public void setUp() {
      FunctionalMapImpl<Integer, String> functionalMap = FunctionalMapImpl.create();
      writeOnlyMap = WriteOnlyMapImpl.create(functionalMap);
      readWriteMap = ReadWriteMapImpl.create(functionalMap);
   }

   @Test
   public void testLambdaReadWriteListeners() throws Exception {
      List<CountDownLatch> latches = new ArrayList<>();
      latches.addAll(Arrays.asList(new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1)));
      AutoCloseable onCreate = readWriteMap.listeners().onCreate(created -> {
         assertEquals("created", created.get());
         latches.get(0).countDown();
      });
      AutoCloseable onModify = readWriteMap.listeners().onModify((before, after) -> {
         assertEquals("created", before.get());
         assertEquals("modified", after.get());
         latches.get(1).countDown();
      });
      AutoCloseable onRemove = readWriteMap.listeners().onRemove(removed -> {
         assertEquals("modified", removed.get());
         latches.get(2).countDown();
      });

      awaitNoEvent(writeOnlyMap.eval(1, writeView -> writeView.set("created")), latches.get(0));
      awaitNoEvent(writeOnlyMap.eval(1, writeView -> writeView.set("modified")), latches.get(1));
      awaitNoEvent(writeOnlyMap.eval(1, WriteEntryView::remove), latches.get(2));

      awaitEvent(readWriteMap.eval(2, rwView -> rwView.set("created")), latches.get(0));
      awaitEvent(readWriteMap.eval(2, rwView -> rwView.set("modified")), latches.get(1));
      awaitEvent(readWriteMap.eval(2, ReadWriteEntryView::remove), latches.get(2));

      onCreate.close();
      onModify.close();
      onRemove.close();

      launderLatches(latches, 3);

      awaitNoEvent(writeOnlyMap.eval(3, writeView -> writeView.set("tres")), latches.get(0));
      awaitNoEvent(writeOnlyMap.eval(3, writeView -> writeView.set("three")), latches.get(1));
      awaitNoEvent(writeOnlyMap.eval(3, WriteEntryView::remove), latches.get(2));

      awaitNoEvent(readWriteMap.eval(4, rwView -> rwView.set("cuatro")), latches.get(0));
      awaitNoEvent(readWriteMap.eval(4, rwView -> rwView.set("four")), latches.get(1));
      awaitNoEvent(readWriteMap.eval(4, ReadWriteEntryView::remove), latches.get(2));
   }

   @Test
   public void testLambdaWriteListeners() throws Exception {
      List<CountDownLatch> latches = launderLatches(new ArrayList<>(), 1);
      AutoCloseable onWrite = writeOnlyMap.listeners().onWrite(read -> {
         assertEquals("write", read.get());
         latches.get(0).countDown();
      });

      awaitEventAndLaunderLatch(writeOnlyMap.eval(1, writeView -> writeView.set("write")), latches);
      awaitEventAndLaunderLatch(writeOnlyMap.eval(1, writeView -> writeView.set("write")), latches);
      onWrite.close();
      awaitNoEvent(writeOnlyMap.eval(2, writeView -> writeView.set("write")), latches.get(0));
      awaitNoEvent(writeOnlyMap.eval(2, writeView -> writeView.set("write")), latches.get(0));

      AutoCloseable onWriteRemove = writeOnlyMap.listeners().onWrite(read -> {
         assertFalse(read.find().isPresent());
         latches.get(0).countDown();
      });

      awaitEventAndLaunderLatch(writeOnlyMap.eval(1, WriteEntryView::remove), latches);
      onWriteRemove.close();
      awaitNoEvent(writeOnlyMap.eval(2, WriteEntryView::remove), latches.get(0));
   }

   @Test
   public void testObjectReadWriteListeners() throws Exception {
      TrackingReadWriteListener<Integer, String> listener = new TrackingReadWriteListener<>();
      AutoCloseable closeable = readWriteMap.listeners().add(listener);

      awaitNoEvent(writeOnlyMap.eval(1, writeView -> writeView.set("created")), listener.latch);
      awaitNoEvent(writeOnlyMap.eval(1, writeView -> writeView.set("modified")), listener.latch);
      awaitNoEvent(writeOnlyMap.eval(1, WriteEntryView::remove), listener.latch);

      awaitEvent(readWriteMap.eval(2, rwView -> rwView.set("created")), listener.latch);
      awaitEvent(readWriteMap.eval(2, rwView -> rwView.set("modified")), listener.latch);
      awaitEvent(readWriteMap.eval(2, ReadWriteEntryView::remove), listener.latch);

      closeable.close();
      awaitNoEvent(writeOnlyMap.eval(3, writeView -> writeView.set("tres")), listener.latch);
      awaitNoEvent(writeOnlyMap.eval(3, writeView -> writeView.set("three")), listener.latch);
      awaitNoEvent(writeOnlyMap.eval(3, WriteEntryView::remove), listener.latch);

      awaitNoEvent(readWriteMap.eval(4, rwView -> rwView.set("cuatro")), listener.latch);
      awaitNoEvent(readWriteMap.eval(4, rwView -> rwView.set("four")), listener.latch);
      awaitNoEvent(readWriteMap.eval(4, ReadWriteEntryView::remove), listener.latch);
   }

   @Test
   public void testObjectWriteListeners() throws Exception {
      TrackingWriteListener<Integer, String> writeListener = new TrackingWriteListener<>();
      AutoCloseable writeListenerCloseable = writeOnlyMap.listeners().add(writeListener);

      awaitEvent(writeOnlyMap.eval(1, writeView -> writeView.set("write")), writeListener.latch);
      awaitEvent(writeOnlyMap.eval(1, writeView -> writeView.set("write")), writeListener.latch);
      writeListenerCloseable.close();
      awaitNoEvent(writeOnlyMap.eval(2, writeView -> writeView.set("write")), writeListener.latch);
      awaitNoEvent(writeOnlyMap.eval(2, writeView -> writeView.set("write")), writeListener.latch);

      TrackingRemoveOnWriteListener<Integer, String> writeRemoveListener = new TrackingRemoveOnWriteListener<>();
      AutoCloseable writeRemoveListenerCloseable = writeOnlyMap.listeners().add(writeRemoveListener);

      awaitEvent(writeOnlyMap.eval(1, WriteEntryView::remove), writeRemoveListener.latch);
      writeRemoveListenerCloseable.close();
      awaitNoEvent(writeOnlyMap.eval(2, WriteEntryView::remove), writeRemoveListener.latch);
   }

   private static List<CountDownLatch> launderLatches(List<CountDownLatch> latches, int numLatches) {
      latches.clear();
      for (int i = 0; i < numLatches; i++)
         latches.add(new CountDownLatch(1));

      return latches;
   }

   public static <T> T awaitEvent(CompletableFuture<T> cf, CountDownLatch eventLatch) {
      try {
         T t = cf.get();
         assertTrue(eventLatch.await(500, TimeUnit.MILLISECONDS));
         return t;
      } catch (InterruptedException | ExecutionException e) {
         throw new Error(e);
      }
   }

   public static <T> T awaitNoEvent(CompletableFuture<T> cf, CountDownLatch eventLatch) {
      try {
         T t = cf.get();
         assertFalse(eventLatch.await(500, TimeUnit.MILLISECONDS));
         return t;
      } catch (InterruptedException | ExecutionException e) {
         throw new Error(e);
      }
   }

   public static <T> T awaitEventAndLaunderLatch(CompletableFuture<T> cf, List<CountDownLatch> latches) {
      T t = awaitEvent(cf, latches.get(0));
      launderLatches(latches, 1);
      return t;
   }

   private static final class TrackingReadWriteListener<K, V> implements ReadWriteListener<K, V> {
      CountDownLatch latch = new CountDownLatch(1);

      @Override
      public void onCreate(ReadEntryView<K, V> created) {
         assertEquals("created", created.get());
         latchCountAndLaunder();
      }

      @Override
      public void onModify(ReadEntryView<K, V> before, ReadEntryView<K, V> after) {
         assertEquals("created", before.get());
         assertEquals("modified", after.get());
         latchCountAndLaunder();
      }

      @Override
      public void onRemove(ReadEntryView<K, V> removed) {
         assertEquals("modified", removed.get());
         latchCountAndLaunder();
      }

      private void latchCountAndLaunder() {
         latch.countDown();
         latch = new CountDownLatch(1);
      }
   }

   public static final class TrackingWriteListener<K, V> implements WriteListener<K, V> {
      CountDownLatch latch = new CountDownLatch(1);

      @Override
      public void onWrite(ReadEntryView<K, V> write) {
         assertEquals("write", write.get());
         latchCountAndLaunder();
      }

      private void latchCountAndLaunder() {
         latch.countDown();
         latch = new CountDownLatch(1);
      }
   }

   public static final class TrackingRemoveOnWriteListener<K, V> implements WriteListener<K, V> {
      CountDownLatch latch = new CountDownLatch(1);

      @Override
      public void onWrite(ReadEntryView<K, V> write) {
         assertFalse(write.find().isPresent());
         latchCountAndLaunder();
      }

      private void latchCountAndLaunder() {
         latch.countDown();
         latch = new CountDownLatch(1);
      }
   }

}

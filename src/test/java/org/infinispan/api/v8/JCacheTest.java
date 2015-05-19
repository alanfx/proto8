package org.infinispan.api.v8;

import org.infinispan.api.v8.impl.FunctionalMapImpl;
import org.infinispan.api.v8.impl.JCacheDecorator;
import org.junit.Test;

import javax.cache.*;
import javax.cache.Cache;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import javax.cache.processor.MutableEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

public class JCacheTest {

   javax.cache.Cache<Integer, String> jcache = new JCacheDecorator<>(
      FunctionalMapImpl.<Integer, String>create());

   @Test
   public void testEmptyGetThenPut() {
      assertEquals(null, jcache.get(1));
      jcache.put(1, "one");
      assertEquals("one", jcache.get(1));
   }

   @Test
   public void testPutGet() {
      assertEquals(null, jcache.getAndPut(1, "one"));
      assertEquals("one", jcache.get(1));
   }

   @Test
   public void testGetAndPut() {
      assertEquals(null, jcache.getAndPut(1, "one"));
      assertEquals("one", jcache.getAndPut(1, "uno"));
      assertEquals("uno", jcache.get(1));
   }

   @Test
   public void testGetAndRemove() {
      assertFalse(jcache.remove(1));
      assertEquals(null, jcache.getAndRemove(1));
      assertEquals(null, jcache.getAndPut(1, "one"));
      assertEquals("one", jcache.get(1));
      assertTrue(jcache.remove(1));
      assertEquals(null, jcache.get(1));
      assertEquals(null, jcache.getAndPut(2, "two"));
      assertEquals("two", jcache.get(2));
      assertEquals("two", jcache.getAndRemove(2));
      assertEquals(null, jcache.get(2));
   }

   @Test
   public void testContainsKey() {
      assertEquals(false, jcache.containsKey(1));
      assertEquals(null, jcache.getAndPut(1, "one"));
      assertEquals(true, jcache.containsKey(1));
   }

   @Test
   public void testClear() {
      Map<Integer, String> data = new HashMap<>();
      data.put(1, "one");
      data.put(2, "two");
      data.put(3, "three");
      jcache.putAll(data);
      jcache.clear();
      assertEquals(null, jcache.get(1));
      assertEquals(null, jcache.get(2));
      assertEquals(null, jcache.get(3));
   }

   @Test
   public void testPutIfAbsent() {
      assertEquals(null, jcache.get(1));
      assertTrue(jcache.putIfAbsent(1, "one"));
      assertEquals("one", jcache.get(1));
      assertFalse(jcache.putIfAbsent(1, "uno"));
      assertEquals("one", jcache.get(1));
      assertTrue(jcache.remove(1));
      assertEquals(null, jcache.get(1));
   }

   @Test
   public void testConditionalRemove() {
      assertEquals(null, jcache.get(1));
      assertFalse(jcache.remove(1, "xxx"));
      assertEquals(null, jcache.getAndPut(1, "one"));
      assertEquals("one", jcache.get(1));
      assertFalse(jcache.remove(1, "xxx"));
      assertEquals("one", jcache.get(1));
      assertTrue(jcache.remove(1, "one"));
      assertEquals(null, jcache.get(1));
   }

   @Test
   public void testReplace() {
      assertEquals(null, jcache.get(1));
      assertFalse(jcache.replace(1, "xxx"));
      assertEquals(null, jcache.getAndPut(1, "one"));
      assertEquals("one", jcache.get(1));
      assertTrue(jcache.replace(1, "uno"));
      assertEquals("uno", jcache.get(1));
      assertTrue(jcache.remove(1));
      assertEquals(null, jcache.get(1));
   }

   @Test
   public void testGetAndReplace() {
      assertEquals(null, jcache.get(1));
      assertEquals(null, jcache.getAndReplace(1, "xxx"));
      assertEquals(null, jcache.getAndPut(1, "one"));
      assertEquals("one", jcache.get(1));
      assertEquals("one", jcache.getAndReplace(1, "uno"));
      assertEquals("uno", jcache.get(1));
      assertTrue(jcache.remove(1));
      assertEquals(null, jcache.get(1));
   }

   @Test
   public void testReplaceWithValue() {
      assertEquals(null, jcache.get(1));
      assertFalse(jcache.replace(1, "xxx", "uno"));
      assertEquals(null, jcache.getAndPut(1, "one"));
      assertEquals("one", jcache.get(1));
      assertFalse(jcache.replace(1, "xxx", "uno"));
      assertEquals("one", jcache.get(1));
      assertTrue(jcache.replace(1, "one", "uno"));
      assertEquals("uno", jcache.get(1));
      assertTrue(jcache.remove(1));
      assertEquals(null, jcache.get(1));
   }

   @Test
   public void testPutAllGetAllRemoveAll() {
      assertTrue(jcache.getAll(new HashSet<>(Arrays.asList(1, 2, 3))).isEmpty());
      assertTrue(jcache.getAll(new HashSet<>()).isEmpty());

      Map<Integer, String> data = new HashMap<>();
      data.put(1, "one");
      data.put(2, "two");
      data.put(3, "three");
      data.put(4, "four");
      data.put(5, "five");
      data.put(55, "five");
      jcache.putAll(data);

      assertEquals("one", jcache.get(1));
      assertEquals("two", jcache.get(2));
      assertEquals("three", jcache.get(3));
      assertEquals("four", jcache.get(4));
      assertEquals("five", jcache.get(5));
      assertEquals("five", jcache.get(55));

      // Get all no keys
      Map<Integer, String> res0 = jcache.getAll(new HashSet<>());
      assertTrue(res0.isEmpty());
      assertEquals(0, res0.size());

      // Get all for a subset of keys
      Map<Integer, String> res1 = jcache.getAll(new HashSet<>(Arrays.asList(1, 2, 5, 55)));
      assertFalse(res1.isEmpty());
      assertEquals(4, res1.size());
      assertEquals("one", res1.get(1));
      assertEquals("two", res1.get(2));
      assertEquals("five", res1.get(5));
      assertEquals("five", res1.get(55));

      // Get all for entire keys set
      Map<Integer, String> res2 = jcache.getAll(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 55)));
      assertFalse(res2.isEmpty());
      assertEquals(6, res2.size());
      assertEquals("one", res2.get(1));
      assertEquals("two", res2.get(2));
      assertEquals("three", res2.get(3));
      assertEquals("four", res2.get(4));
      assertEquals("five", res2.get(5));
      assertEquals("five", res2.get(55));

      // Remove all passing no keys
      jcache.removeAll(new HashSet<>());
      Map<Integer, String> res3 = jcache.getAll(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 55)));
      assertFalse(res3.isEmpty());
      assertEquals(6, res3.size());

      // Remove all passing subset of keys
      jcache.removeAll(new HashSet<>(Arrays.asList(3, 4, 5, 55)));
      Map<Integer, String> res4 = jcache.getAll(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 55)));
      assertFalse(res4.isEmpty());
      assertEquals(2, res4.size());
      assertEquals("one", res4.get(1));
      assertEquals("two", res4.get(2));

      // Remove all the keys
      jcache.removeAll();
      Map<Integer, String> res5 = jcache.getAll(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 55)));
      assertTrue(res5.isEmpty());
      assertEquals(0, res5.size());
   }

   @Test
   public void testIterator() {
      assertFalse(jcache.iterator().hasNext());
      Map<Integer, String> data = new HashMap<>();
      data.put(1, "one");
      data.put(2, "two");
      data.put(3, "three");
      data.put(4, "four");
      data.put(5, "five");
      data.put(55, "five");
      jcache.putAll(data);

      Map<Integer, String> res0 = new HashMap<>();
      for (Cache.Entry<Integer, String> e : jcache)
         res0.put(e.getKey(), e.getValue());

      assertEquals(data, res0);
   }

   @Test
   public void testInvoke() {
      // Get via invoke
      String res0 = jcache.invoke(1, (entry, args) -> entry.getValue());
      assertEquals(null, res0);

      // Put via invoke
      jcache.invoke(1, (entry, args) -> {
         entry.setValue((String) args[0]);
         return null;
      }, "one");

      // Get via invoke
      String res1 = jcache.invoke(1, (entry, args) -> entry.getValue());
      assertEquals("one", res1);

      // Remove via invoke
      jcache.invoke(1, (entry, args) -> {
         entry.remove();
         return null;
      });

      // Get via invoke
      String res2 = jcache.invoke(1, (entry, args) -> entry.getValue());
      assertEquals(null, res2);
   }

   @Test
   public void testInvokeAll() {
      HashSet<Integer> keys = new HashSet<>(Arrays.asList(1, 2, 3));

      // Get multi via invokeAll
      Map<Integer, EntryProcessorResult<String>> res0 = jcache.invokeAll(keys, (entry, args) -> entry.getValue());
      assertEquals(null, res0.get(1).get());
      assertEquals(null, res0.get(2).get());
      assertEquals(null, res0.get(3).get());

      // Put multi via invokeAll
      jcache.invokeAll(keys, (entry, args) -> {
         entry.setValue((String) args[entry.getKey() - 1]);
         return null;
      }, "one", "two", "three");

      // Get multi via invokeAll
      Map<Integer, EntryProcessorResult<String>> res1 = jcache.invokeAll(keys, (entry, args) -> entry.getValue());
      assertEquals("one", res1.get(1).get());
      assertEquals("two", res1.get(2).get());
      assertEquals("three", res1.get(3).get());

      // Remove multi via invokeAll
      jcache.invokeAll(keys, (entry, args) -> {
         entry.remove();
         return null;
      });

      // Get multi via invokeAll
      Map<Integer, EntryProcessorResult<String>> res2 = jcache.invokeAll(keys, (entry, args) -> entry.getValue());
      assertEquals(null, res2.get(1).get());
      assertEquals(null, res2.get(2).get());
      assertEquals(null, res2.get(3).get());
   }

   @Test
   public void testClose() {
      JCacheDecorator<Integer, String> local = new JCacheDecorator<>(FunctionalMapImpl.<Integer, String>create());
      assertFalse(local.isClosed());
      local.close();
      assertTrue(local.isClosed());
   }

   @Test
   public void testGetName() {
      assertEquals("", jcache.getName());
   }

}

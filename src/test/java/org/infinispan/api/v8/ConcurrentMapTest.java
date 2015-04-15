package org.infinispan.api.v8;

import org.infinispan.api.v8.impl.ConcurrentMapDecorator;
import org.infinispan.api.v8.impl.FunctionalMapImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConcurrentMapTest {

   ConcurrentMap<Integer, String> map = new ConcurrentMapDecorator<>(
      FunctionalMapImpl.<Integer, String>create());

   @Test
   public void testEmptyGetThenPut() {
      assertEquals(null, map.get(1));
      assertEquals(null, map.put(1, "one"));
      assertEquals("one", map.get(1));
   }

   @Test
   public void testPutGet() {
      assertEquals(null, map.put(1, "one"));
      assertEquals("one", map.get(1));
   }

   @Test
   public void testGetAndPut() {
      assertEquals(null, map.put(1, "one"));
      assertEquals("one", map.put(1, "uno"));
      assertEquals("uno", map.get(1));
   }

   @Test
   public void testGetAndRemove() {
      assertEquals(null, map.put(1, "one"));
      assertEquals("one", map.get(1));
      assertEquals("one", map.remove(1));
      assertEquals(null, map.get(1));
   }

   @Test
   public void testContainsKey() {
      assertEquals(false, map.containsKey(1));
      assertEquals(null, map.put(1, "one"));
      assertEquals(true, map.containsKey(1));
   }

   @Test
   public void testContainsValue() {
      assertEquals(false, map.containsValue("one"));
      assertEquals(null, map.put(1, "one"));
      assertEquals(true, map.containsValue("one"));
      assertEquals(false, map.containsValue("uno"));
   }

   @Test
   public void testSize() {
      assertEquals(0, map.size());
      assertEquals(null, map.put(1, "one"));
      assertEquals(1, map.size());
      assertEquals(null, map.put(2, "two"));
      assertEquals(null, map.put(3, "three"));
      assertEquals(3, map.size());
   }

   @Test
   public void testEmpty() {
      assertEquals(true, map.isEmpty());
      assertEquals(null, map.put(1, "one"));
      assertEquals("one", map.get(1));
      assertEquals(false, map.isEmpty());
      assertEquals("one", map.remove(1));
      assertEquals(true, map.isEmpty());
   }

   @Test
   public void testPutAll() {
      assertEquals(true, map.isEmpty());
      Map<Integer, String> data = new HashMap<>();
      data.put(1, "one");
      data.put(2, "two");
      data.put(3, "three");
      map.putAll(data);
      assertEquals("one", map.get(1));
      assertEquals("two", map.get(2));
      assertEquals("three", map.get(3));
   }

   @Test
   public void testClear() {
      assertEquals(true, map.isEmpty());
      Map<Integer, String> data = new HashMap<>();
      data.put(1, "one");
      data.put(2, "two");
      data.put(3, "three");
      map.putAll(data);
      map.clear();
      assertEquals(null, map.get(1));
      assertEquals(null, map.get(2));
      assertEquals(null, map.get(3));
   }

   @Test
   public void testKeyValueAndEntrySets() {
      assertEquals(true, map.isEmpty());
      Map<Integer, String> data = new HashMap<>();
      data.put(1, "one");
      data.put(2, "two");
      data.put(3, "three");
      map.putAll(data);

      Set<Integer> keys = map.keySet();
      assertEquals(3, keys.size());
      Set<Integer> expectedKeys = new HashSet<>(Arrays.asList(1, 2, 3));
      keys.forEach(expectedKeys::remove);
      assertEquals(true, expectedKeys.isEmpty());

      assertEquals(false, map.isEmpty());
      Collection<String> values = map.values();
      assertEquals(3, values.size());
      Set<String> expectedValues = new HashSet<>(Arrays.asList("one", "two", "three"));
      values.forEach(expectedValues::remove);
      assertEquals(true, expectedValues.isEmpty());

      Set<Map.Entry<Integer, String>> entries = map.entrySet();
      assertEquals(3, entries.size());
      entries.removeAll(data.entrySet());
      assertEquals(true, entries.isEmpty());
   }

   @Test
   public void testPutIfAbsent() {
      assertEquals(null, map.get(1));
      assertEquals(null, map.putIfAbsent(1, "one"));
      assertEquals("one", map.get(1));
      assertEquals("one", map.putIfAbsent(1, "uno"));
      assertEquals("one", map.get(1));
      assertEquals("one", map.remove(1));
      assertEquals(null, map.get(1));
   }

   @Test
   public void testConditionalRemove() {
      assertEquals(null, map.get(1));
      assertFalse(map.remove(1, "xxx"));
      assertEquals(null, map.put(1, "one"));
      assertEquals("one", map.get(1));
      assertFalse(map.remove(1, "xxx"));
      assertEquals("one", map.get(1));
      assertTrue(map.remove(1, "one"));
      assertEquals(null, map.get(1));
   }

   @Test
   public void testReplace() {
      assertEquals(null, map.get(1));
      assertEquals(null, map.replace(1, "xxx"));
      assertEquals(null, map.put(1, "one"));
      assertEquals("one", map.get(1));
      assertEquals("one", map.replace(1, "uno"));
      assertEquals("uno", map.get(1));
      assertEquals("uno", map.remove(1));
      assertEquals(null, map.get(1));
   }

   @Test
   public void testReplaceWithValue() {
      assertEquals(null, map.get(1));
      assertFalse(map.replace(1, "xxx", "uno"));
      assertEquals(null, map.put(1, "one"));
      assertEquals("one", map.get(1));
      assertFalse(map.replace(1, "xxx", "uno"));
      assertEquals("one", map.get(1));
      assertTrue(map.replace(1, "one", "uno"));
      assertEquals("uno", map.get(1));
      assertEquals("uno", map.remove(1));
      assertEquals(null, map.get(1));
   }

}

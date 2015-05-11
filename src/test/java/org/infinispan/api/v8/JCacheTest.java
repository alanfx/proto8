package org.infinispan.api.v8;

import org.infinispan.api.v8.impl.ConcurrentMapDecorator;
import org.infinispan.api.v8.impl.FunctionalMapImpl;
import org.junit.Test;

import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.assertEquals;

public class JCacheTest {

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

}

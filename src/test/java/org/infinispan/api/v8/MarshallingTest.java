package org.infinispan.api.v8;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.river.RiverMarshallerFactory;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MarshallingTest {

   @Test
   public void testLambdaEquality() throws Exception {
      Function<String, Integer> f1 = Integer::valueOf;
      Function<String, Integer> f2 = Integer::valueOf;
      assertFalse(f1 == f2);
      assertFalse(f1.equals(f2));
   }

   @Test @Ignore("Does not work since lambdas are not serializable by default")
   public void testFunction() throws Exception {
      Function<String, Integer> fm = Integer::valueOf;
      assertMarshalling(
         m -> m.writeObject(fm),
         u -> {
            Function<String, Integer> fu = (Function<String, Integer>) u.readObject();
            assertEquals(1, fu.apply("1").intValue());
         }
      );
   }

   @Test
   public void testSerializableApplyLambda() throws Exception {
      SerializableApply<String, Integer> fm = s -> Integer.valueOf(s);
      assertMarshalling(
         m -> m.writeObject(fm),
         u -> {
            SerializableApply<String, Integer> fu = (SerializableApply<String, Integer>) u.readObject();
            assertEquals(1, fu.apply("1").intValue());
         }
      );
   }

   @Test
   public void testSerializableFunction() throws Exception {
      SerializableFunction<String, Integer> fm = s -> Integer.valueOf(s);
      // Simple verification that a user can provide a Serializable function
      // that extends java.util.Function, and we can pass in wherever we take
      // a function. This verifies standard contravariant input parameter rules.
      assertEquals(1, applyFunction(fm, "1").intValue());
      assertMarshalling(
         m -> m.writeObject(fm),
         u -> {
            SerializableFunction<String, Integer> fu = (SerializableFunction<String, Integer>) u.readObject();
            assertEquals(1, applyFunction(fu, "1").intValue());
         }
      );
   }

   static <T, R> R applyFunction(Function<T, R> f, T t) {
      return f.apply(t);
   }

   @Test @Ignore("Does not work because you can't detect if you lambdas are the same using equals()")
   public void testRegisteredNonCapturingFunction() {
      MarshallingConfiguration cfg = new MarshallingConfiguration();
      LambdaRegistry lambdas = new LambdaRegistry();
      lambdas.add(1, (Function<String, Integer>) Integer::valueOf);
      cfg.setObjectTable(lambdas);

      assertMarshalling(cfg,
         m -> {
            Function<String, Integer> f = Integer::valueOf;
            m.writeObject(f);
         },
         u -> {
            Function<String, Integer> fu = (Function<String, Integer>) u.readObject();
            assertEquals(1, fu.apply("1").intValue());
         }
      );
   }

   @Test
   public void testUTFString() throws IOException {
      assertMarshalling(
         m -> m.writeUTF("basic test"),
         u -> assertEquals("basic test", u.readUTF())
      );
   }

   void assertMarshalling(IOConsumer<Marshaller> mf, IOConsumer<Unmarshaller> uf) {
      assertMarshalling(new MarshallingConfiguration(), mf, uf);
   }

   void assertMarshalling(MarshallingConfiguration cfg, IOConsumer<Marshaller> mf, IOConsumer<Unmarshaller> uf) {
      MarshallerFactory factory = new RiverMarshallerFactory();
      try {
         ByteArrayOutputStream os = new ByteArrayOutputStream();
         Marshaller marshaller = factory.createMarshaller(cfg);
         marshaller.start(Marshalling.createByteOutput(os));
         try {
            mf.accept(marshaller);
         } finally {
            marshaller.finish();
         }

         ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
         Unmarshaller unmarshaller = factory.createUnmarshaller(cfg);
         unmarshaller.start(Marshalling.createByteInput(is));
         try {
            uf.accept(unmarshaller);
         } finally {
            unmarshaller.finish();
         }
      } catch (Exception e) {
         throw new AssertionError(e);
      }
   }

   @FunctionalInterface
   interface IOConsumer<T> {
      void accept(T t) throws IOException, ClassNotFoundException;
   }

   @FunctionalInterface
   interface SerializableApply<T, R> extends Serializable {
      R apply(T t);
   }

   @FunctionalInterface
   interface SerializableFunction<T, R> extends Function<T, R>, Serializable {
      R apply(T t);
   }

   static class LambdaRegistry implements ObjectTable {
      Map<Object, Integer> lambdas = new HashMap<>();
      Map<Integer, Object> ids = new HashMap<>();

      void add(int id, Object lambda) {
         lambdas.put(lambda, id);
         ids.put(id, lambda);
      }

      @Override
      public Writer getObjectWriter(Object object) throws IOException {
         Integer id = lambdas.get(object);
         if (id != null)
            return (marshaller, obj) -> marshaller.write(id);

         throw new NotSerializableException(object + " is not serializable");
      }

      @Override
      public Object readObject(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
         int id = unmarshaller.read();
         return ids.get(id);
      }
   }
}

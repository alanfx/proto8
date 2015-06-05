package org.infinispan.api.v8;

import org.jboss.marshalling.ClassExternalizerFactory;
import org.jboss.marshalling.Creator;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.river.RiverMarshallerFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class MarshallingTest {

   @Rule public TestName name = new TestName();

   @Test
   public void testLambdaEquality() throws Exception {
      Function<String, Integer> f1 = Integer::valueOf;
      Function<String, Integer> f2 = Integer::valueOf;
      assertFalse(f1 == f2);
      assertFalse(f1.equals(f2));
   }

   /**
    * Does not work since lambdas are not serializable by default.
    */
   @Test(expected = NotSerializableException.class)
   public void testNonCaptutingLambda() throws IOException {
      Function<String, Integer> fm = Integer::valueOf;
      assertNotSerializable(
         m -> m.writeObject(fm),
         u -> {
            Function<String, Integer> fu = (Function<String, Integer>) u.readObject();
            assertEquals(1, fu.apply("1").intValue());
         }
      );
   }

   /**
    * Does not work since lambdas are not serializable by default
    */
   @Test(expected = NotSerializableException.class)
   public void testCapturingLambda() throws IOException {
      final Optional<String> random = Optional.of(UUID.randomUUID().toString());
      Predicate<Optional<String>> guessRandom = s -> s.equals(random);
      assertNotSerializable(
         m -> m.writeObject(guessRandom),
         u -> {
            Predicate<Optional<String>> p = (Predicate<Optional<String>>) u.readObject();
            assertTrue(p.test(random));
            assertFalse(p.test(Optional.of("x")));
         }
      );
   }

   /**
    * Payload size 587 bytes, very bulky.
    */
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

   /**
    * Payload size 587 bytes, very bulky.
    */
   @Test
   public void testSerializableNonCapturingLambda() throws Exception {
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

   /**
    * Lambda is serializable now, but it captures a non-serializable object, so it fails.
    */
   @Test(expected = NotSerializableException.class)
   public void testSerializableCapturingLambda() throws IOException {
      final Optional<String> random = Optional.of(UUID.randomUUID().toString());
      SerializablePredicate<Optional<String>> guessRandom = s -> s.equals(random);
      assertNotSerializable(
         m -> m.writeObject(guessRandom),
         u -> {
            Predicate<Optional<String>> p = (Predicate<Optional<String>>) u.readObject();
            assertTrue(p.test(random));
            assertFalse(p.test(Optional.of("xxx")));
         }
      );
   }

   /**
    * Does not work because you can't detect if you lambdas are the same using equals()
    */
   @Test(expected = NotSerializableException.class)
   public void testRegisteredNonCapturingLambda() throws IOException {
      MarshallingConfiguration cfg = new MarshallingConfiguration();
      LambdaInstanceRegistry lambdas = new LambdaInstanceRegistry();
      lambdas.add(1, (Function<String, Integer>) Integer::valueOf);
      cfg.setObjectTable(lambdas);

      assertNotSerializable(cfg,
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

   /**
    * This is a relatively efficient option for marshalling non-capturing lambdas.
    * Create a fully dedicated class for the lambda function you want to execute,
    * then register a externalizer which returns the function that you want to
    * be applied on the reading side.
    *
    * Payload size 160 bytes, so still a bit bulky.
    */
   @Test
   public void testExternalizerNonCapturingLambda() {
      MarshallingConfiguration cfg = new MarshallingConfiguration();
      cfg.setClassExternalizerFactory(new LambdaExternalizerFactory());
      assertMarshalling(cfg,
         m -> {
            m.writeObject(new IntegerValueOfFunction());
         },
         u -> {
            Function<String, Integer> fu = (Function<String, Integer>) u.readObject();
            assertEquals(1, fu.apply("1").intValue());
         }
      );
   }

   /**
    * A way to serialize capturing lambdas, using designated class implementations
    * for the lambdas and the captured variables.
    *
    * Payload is 192 bytes, which is reasonable but can be improved.
    */
   @Test
   public void testExternalizerCapturingLambda() {
      MarshallingConfiguration cfg = new MarshallingConfiguration();
      final Optional<String> random = Optional.of(UUID.randomUUID().toString());
      IsRandomPredicate guessRandom = new IsRandomPredicate(random);
      cfg.setClassExternalizerFactory(new LambdaExternalizerFactory());
      assertMarshalling(cfg,
         m -> {
            m.writeObject(guessRandom);
         },
         u -> {
            Predicate<Optional<String>> p = (Predicate<Optional<String>>) u.readObject();
            assertTrue(p.test(random));
            assertFalse(p.test(Optional.of("x")));
         }
      );
   }

   /**
    * The most efficient way to transfer around a non-capturing lambda,
    * taking advantage of the object table.
    *
    * Payload size is 3 bytes.
    */
   @Test
   public void testObjectTableNonCapturingLambda() {
      MarshallingConfiguration cfg = new MarshallingConfiguration();
      LambdaClassRegistry registry = new LambdaClassRegistry();
      registry.add(1, IntegerValueOfFunction.class);

      cfg.setObjectTable(registry);
      assertMarshalling(cfg,
         m -> {
            m.writeObject(new IntegerValueOfFunction());
         },
         u -> {
            Function<String, Integer> fu = (Function<String, Integer>) u.readObject();
            assertEquals(1, fu.apply("1").intValue());
         }
      );
   }

   /**
    * The most efficient way to serialize a capturing lambda, by registering
    * it in the object table and making any captured objects be passed in the
    * constructor.
    *
    * Payload is 43 bytes.
    */
   @Test
   public void testObjectTableCapturingLambda() {
      MarshallingConfiguration cfg = new MarshallingConfiguration();
      LambdaClassRegistry registry = new LambdaClassRegistry();
      registry.add(2, IsRandomPredicate.class);
      final Optional<String> random = Optional.of(UUID.randomUUID().toString());

      cfg.setObjectTable(registry);
      assertMarshalling(cfg,
         m -> {
            IsRandomPredicate guessRandom = new IsRandomPredicate(random);
            m.writeObject(guessRandom);
         },
         u -> {
            Predicate<Optional<String>> p = (Predicate<Optional<String>>) u.readObject();
            assertTrue(p.test(random));
            assertFalse(p.test(Optional.of("x")));
         }
      );
   }

   void assertNotSerializable(IOConsumer<Marshaller> mf, IOConsumer<Unmarshaller> uf) throws IOException {
      assertNotSerializable(new MarshallingConfiguration(), mf, uf);
   }

   void assertNotSerializable(MarshallingConfiguration cfg, IOConsumer<Marshaller> mf, IOConsumer<Unmarshaller> uf) throws IOException {
      marshallToUnmarshall(cfg, mf, uf);
   }

   void assertMarshalling(IOConsumer<Marshaller> mf, IOConsumer<Unmarshaller> uf) {
      assertMarshalling(new MarshallingConfiguration(), mf, uf);
   }

   void assertMarshalling(MarshallingConfiguration cfg, IOConsumer<Marshaller> mf, IOConsumer<Unmarshaller> uf) {
      try {
         marshallToUnmarshall(cfg, mf, uf);
      } catch (Exception e) {
         throw new AssertionError(e);
      }
   }

   private void marshallToUnmarshall(MarshallingConfiguration cfg,
         IOConsumer<Marshaller> mf, IOConsumer<Unmarshaller> uf) throws IOException {
      MarshallerFactory factory = new RiverMarshallerFactory();
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      Marshaller marshaller = factory.createMarshaller(cfg);
      marshaller.start(Marshalling.createByteOutput(os));
      try {
         mf.accept(marshaller);
      } catch (ClassNotFoundException e) {
         throw new AssertionError(e);
      } finally {
         marshaller.finish();
      }

      byte[] bytes = os.toByteArray();
      System.out.printf("%s payload is %d bytes %n", name.getMethodName(), bytes.length);

      ByteArrayInputStream is = new ByteArrayInputStream(bytes);
      Unmarshaller unmarshaller = factory.createUnmarshaller(cfg);
      unmarshaller.start(Marshalling.createByteInput(is));
      try {
         uf.accept(unmarshaller);
      } catch (ClassNotFoundException e) {
         throw new AssertionError(e);
      } finally {
         unmarshaller.finish();
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
   interface SerializableFunction<T, R> extends Function<T, R>, Serializable {}

   @FunctionalInterface
   interface SerializablePredicate<T> extends Predicate<T>, Serializable {}

   static final class LambdaInstanceRegistry implements ObjectTable {
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

   static final class IntegerValueOfFunction implements Function<String, Integer> {
      @Override
      public Integer apply(String s) {
         return Integer.valueOf(s);
      }

      static class Externalizer implements DefaultExternalizer {
         @Override
         public Object createExternal(Class<?> subjectType, ObjectInput input, Creator defaultCreator) {
            return (Function<String, Integer>) Integer::valueOf;
         }
      }
   }

   static final class IsRandomPredicate implements Predicate<Optional<String>> {
      final Optional<String> random;

      IsRandomPredicate(Optional<String> random) {
         this.random = random;
      }

      @Override
      public boolean test(Optional<String> s) {
         return s.equals(random);
      }

      static class Externalizer implements DefaultExternalizer {
         @Override
         public Object createExternal(Class<?> subjectType, ObjectInput input, Creator defaultCreator) throws IOException {
            return new IsRandomPredicate(Optional.of(input.readUTF()));
         }

         @Override
         public void writeExternal(Object subject, ObjectOutput output) throws IOException {
            IsRandomPredicate p = (IsRandomPredicate) subject;
            output.writeUTF(p.random.get());
         }
      }
   }

   static final class LambdaExternalizerFactory implements ClassExternalizerFactory {
      @Override
      public Externalizer getExternalizer(Class<?> type) {
         if (type.isAssignableFrom(IntegerValueOfFunction.class))
            return new IntegerValueOfFunction.Externalizer();
         else if (type.isAssignableFrom(IsRandomPredicate.class))
            return new IsRandomPredicate.Externalizer();

         return null;
      }
   }

   interface DefaultExternalizer extends Externalizer {
      @Override Object createExternal(Class<?> subjectType, ObjectInput input, Creator defaultCreator) throws IOException;

      @Override default void writeExternal(Object subject, ObjectOutput output) throws IOException {}
      @Override default void readExternal(Object subject, ObjectInput input) throws IOException {}
   }

   static final class LambdaClassRegistry implements ObjectTable {
      Map<Class<?>, Integer> lambdas = new HashMap<>();
      Map<Integer, Class<?>> ids = new HashMap<>();

      void add(int id, Class<?> lambdaClass) {
         lambdas.put(lambdaClass, id);
         ids.put(id, lambdaClass);
      }

      @Override
      public Writer getObjectWriter(Object object) throws IOException {
         Integer id = lambdas.get(object.getClass());
         switch (id) {
            case 1: return (marshaller, obj) -> marshaller.write(id);
            case 2:
               return (marshaller, obj) -> {
                  marshaller.write(id);
                  IsRandomPredicate p = (IsRandomPredicate) obj;
                  marshaller.writeUTF(p.random.get());
               };
            default:
               throw new NotSerializableException(object + " is not serializable");
         }
      }

      @Override
      public Object readObject(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
         int id = unmarshaller.read();
         Class<?> aClass = ids.get(id);
         try {
            if (aClass.equals(IntegerValueOfFunction.class))
               return aClass.newInstance();
            else if (aClass.equals(IsRandomPredicate.class)) {
               return new IsRandomPredicate(Optional.of(unmarshaller.readUTF()));
            }
            return null;
         } catch (InstantiationException | IllegalAccessException e) {
            throw new AssertionError(e);
         }
      }
   }
}

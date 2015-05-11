package org.infinispan.api.v8;

public interface MetaParam<P> {

   <T> Id<T> id();
   P get();

   final class Id<T> {
      final int id;

      public Id(int id) {
         this.id = id;
      }

      public int id() {
         return id;
      }

      @Override
      public String toString() {
         return "Id=" + id;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         Id<?> id1 = (Id<?>) o;

         return id == id1.id;

      }

      @Override
      public int hashCode() {
         return id;
      }
   }

   interface Writable<P> extends MetaParam<P> {}

   final class Lifespan extends LongMetadata implements Writable<Long> {
      public static final Id<Lifespan> ID = new Id<>(0);

      public Lifespan(long lifespan) {
         super(lifespan);
      }

      @Override
      public Id<Lifespan> id() {
         return ID;
      }

      @Override
      public String toString() {
         return "Lifespan=" + value;
      }
   }

   final class Created extends LongMetadata {
      public static final Id<Created> ID = new Id<>(1);

      public Created(long created) {
         super(created);
      }

      @Override
      public Id<Created> id() {
         return ID;
      }

      @Override
      public String toString() {
         return "Created=" + value;
      }
   }

   final class MaxIdle extends LongMetadata implements Writable<Long> {
      public static final Id<MaxIdle> ID = new Id<>(2);

      public MaxIdle(long maxIdle) {
         super(maxIdle);
      }

      @Override
      public Id<MaxIdle> id() {
         return ID;
      }

      @Override
      public String toString() {
         return "MaxIdle=" + value;
      }
   }

   final class LastUsed extends LongMetadata {
      public static final Id<LastUsed> ID = new Id<>(3);

      public LastUsed(long lastUsed) {
         super(lastUsed);
      }

      @Override
      public Id<LastUsed> id() {
         return ID;
      }

      @Override
      public String toString() {
         return "LastUsed=" + value;
      }
   }

   final class EntryVersionParam<V> implements Writable<EntryVersion<V>> {
      //public static final Id<EntryVersionParam<?>> ID = new Id<>(4);

      public static <V> Id<EntryVersionParam<V>> ID() {
         return new Id<>(4);
      }

      private final EntryVersion<V> entryVersion;

      public EntryVersionParam(EntryVersion<V> entryVersion) {
         this.entryVersion = entryVersion;
      }

      @Override
      public <T> Id<T> id() {
         return (Id<T>) ID();
      }

      @Override
      public EntryVersion<V> get() {
         return entryVersion;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         EntryVersionParam<?> that = (EntryVersionParam<?>) o;

         return entryVersion.equals(that.entryVersion);
      }

      @Override
      public int hashCode() {
         return entryVersion.hashCode();
      }

      @Override
      public String toString() {
         return "MetaParam=" + entryVersion;
      }
   }

   abstract class LongMetadata implements MetaParam<Long> {
      protected final long value;

      public LongMetadata(long value) {
         this.value = value;
      }

      @Override
      public Long get() {
         return value;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         LongMetadata longMeta = (LongMetadata) o;

         return value == longMeta.value;
      }

      @Override
      public int hashCode() {
         return (int) (value ^ (value >>> 32));
      }
   }

}

package org.infinispan.api.v8;

import java.util.function.Consumer;

public interface Observable<T> {

   Subscription subscribe(Observer<? super T> observer);
   Subscription subscribe(Subscriber<? super T> subscriber);

   interface Subscription {
      void unsubscribe();
      boolean isUnsubscribed();
   }

   interface Observer<T> {
      default void onCompleted() {}
      default void onError(Throwable e) {}
      default void onNext(T t) {}
   }

   default Subscription subscribe(Consumer<? super T> onNext, Consumer<Throwable> onError, Runnable onComplete) {
      return subscribe(new Observer<T>() {
         @Override public void onCompleted() { onComplete.run(); }
         @Override  public void onError(Throwable e) { onError.accept(e); }
         @Override  public void onNext(T t) { onNext.accept(t); }
      });
   }

   default Subscription subscribe(Consumer<? super T> onNext, Consumer<Throwable> onError) {
      return subscribe(new Observer<T>() {
         @Override public void onCompleted() { /** no-op */ }
         @Override  public void onError(Throwable e) { onError.accept(e); }
         @Override  public void onNext(T t) { onNext.accept(t); }
      });
   }

   default Subscription subscribe(Consumer<? super T> onNext) {
      return subscribe(new Observer<T>() {
         @Override public void onCompleted() { /** no-op */ }
         @Override  public void onError(Throwable e) { throw new IllegalStateException(); }
         @Override  public void onNext(T t) { onNext.accept(t); }
      });
   }

   abstract class Subscriber<T> implements Observer<T>, Subscription {
      private volatile boolean unsubscribed;

      @Override
      public void unsubscribe() {
         unsubscribed = true;
      }

      @Override
      public boolean isUnsubscribed() {
         return unsubscribed;
      }
   }

}

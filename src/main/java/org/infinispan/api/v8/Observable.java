package org.infinispan.api.v8;

import java.util.function.Consumer;

public interface Observable<T> {

   Subscription subscribe(Consumer<? super T> onNext);

   Subscription subscribe(Consumer<? super T> onNext, Consumer<Throwable> onError);

   Subscription subscribe(Consumer<? super T> onNext, Consumer<Throwable> onError, Runnable onComplete);

   Subscription subscribe(Observer observer);

   interface Subscription {
      void unsubscribe();
      boolean isUnsubscribed();
   }

   interface Observer<T> {
      void onCompleted();
      void onError(Throwable e);
      void onNext(T t);
   }
}

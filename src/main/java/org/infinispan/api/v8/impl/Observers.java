package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.Observable;
import org.infinispan.api.v8.Observable.Observer;

class Observers {

   public static <T> Observer<T> noop() {
      return new NoopObserver<>();
   }

   private final static class NoopObserver<T> implements Observer<T> {
      @Override public void onCompleted() {}
      @Override public void onError(Throwable e) {}
      @Override public void onNext(T o) {}
   }

}

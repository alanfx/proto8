package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.Observable.Observer;

/**
 * Factory class for observers
 */
final class Observers {

   public Observers() {
      // Cannot be instantiated, it's just a holder class
   }

   public static <T> Observer<T> noop() {
      return new NoopObserver<>();
   }

   private final static class NoopObserver<T> implements Observer<T> {
      @Override public void onCompleted() {}
      @Override public void onError(Throwable e) {}
      @Override public void onNext(T o) {}
   }

}

package org.infinispan.api.v8;

import java.util.Set;
import java.util.function.Consumer;

/**
 * An {@link Observable} offers an asynchronous, "push" style, way to subscribe
 * to operations returning multiple results that as they become available.
 *
 * TODO: Add more on usage...
 *
 * {@link java.util.concurrent.CompletableFuture} and {@link Observable} are
 * very similar but whereas {@link java.util.concurrent.CompletableFuture}
 * deals with single, asynchronous, computation result, {@link Observable}
 * offers the possibility to asynchronously receive multiple results.
 *
 * DESIGN RATIONALE:
 * <ul>
 *    <li>Why not use {@link java.util.stream.Stream}?
 *    java.util.stream best supports (multi-stage, possibly-parallel) "pull"
 *    style operations on the elements of collections. Streams work well with
 *    lazy collections but they're not well suited for when these collections
 *    are asynchronous.
 *    In other words, {@link java.util.stream.Stream} API design assumes that
 *    retrieving the next element via "pull" style won't block. If retrieving
 *    next element blocks, parallel stream iterations and other functionality
 *    gets badly affected.
 *    </li>
 *    <li>A crucial capability of {@link Observable} API compared to
 *    {@link java.util.stream.Stream} is that as elements get added to the
 *    functional map, still subsribed {@link Observer} instances continue
 *    processing data without any extra logic. With Streams, users would need
 *    to pull and wait for more data to do something similar.
 *    </li>
 *    <li>{@link java.util.stream.Stream} works for my use case since it offers
 *    a lot of high order functions such as filter/reduce that I could apply
 *    to the cached data very cleanly, why not use that?
 *    If you want to, you can use {@link java.util.stream.Stream} API with
 *    functional map, because using {@link Observable<T>}, key/value/entry
 *    {@link java.util.Set} instances can be build and then call
 *    {@link Set#stream()}.
 *    </li>
 *    <li>With an asynchronous {@link Observable} API, it's easy to build a
 *    blocking {@link java.util.stream.Stream} API, while doing the opposite
 *    is not so straightforward. The same happens with completable future.
 *    Given a {@link java.util.concurrent.CompletableFuture}, you can easily
 *    build a sync and an async API, while building an asynchronous API with
 *    only an synchronous API is quite complex.</li>
 * </ul>
 *
 * @param <T>
 */
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

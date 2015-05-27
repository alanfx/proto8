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
 *    functional map, still subscribed {@link Observer} instances continue
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
 *    <li>TODO: Add relationship with RxJava...
 *    </li>
 * </ul>
 *
 * @param <T>
 */
public interface Observable<T> {

   /**
    * Subscribe a {@link Consumer} to be executed for each element emitted by
    * this observable, and return a {@link Subscription} which can be used to
    * unsubscribe.
    *
    * @param onNext callback to be executed for each emitted element
    * @return a subscription which allows to be unsubscribed
    */
   default Subscription subscribe(Consumer<? super T> onNext) {
      return subscribe(new Observer<T>() {
         @Override public void onCompleted() { /** no-op */ }
         @Override public void onError(Throwable e) { throw new IllegalStateException(); }
         @Override public void onNext(T t) { onNext.accept(t); }
      });
   }

   /**
    * Subscribe a {@link Consumer} to be executed for each element emitted by
    * this observable and a {@link Consumer} to be executed if any error is
    * reported. It returns a {@link Subscription} which can be used to
    * unsubscribe.
    *
    * @param onNext callback to be executed for each emitted element
    * @param onError callback when there's an error
    * @return a subscription which allows to be unsubscribed
    */
   default Subscription subscribe(Consumer<? super T> onNext, Consumer<Throwable> onError) {
      return subscribe(new Observer<T>() {
         @Override public void onCompleted() { /** no-op */ }
         @Override public void onError(Throwable e) { onError.accept(e); }
         @Override public void onNext(T t) { onNext.accept(t); }
      });
   }

   /**
    * Subscribe a {@link Consumer} to be executed for each element emitted by
    * this observable, a {@link Consumer} to be executed if any error is
    * reported, and finally a {@link Runnable} indicating that the observable
    * has completed the emission of all elements. It returns a
    * {@link Subscription} which can be used to unsubscribe.
    *
    * @param onNext callback to be executed for each emitted element
    * @param onError callback when there's an error
    * @param onComplete callback for when the observable has emitted all elements
    * @return a subscription which allows to be unsubscribed
    */
   default Subscription subscribe(Consumer<? super T> onNext, Consumer<Throwable> onError, Runnable onComplete) {
      return subscribe(new Observer<T>() {
         @Override public void onCompleted() { onComplete.run(); }
         @Override  public void onError(Throwable e) { onError.accept(e); }
         @Override  public void onNext(T t) { onNext.accept(t); }
      });
   }

   /**
    * Subscribe an {@link Observer} which can get callbacks for each element
    * emitted, callbacks for when errors happen, and finally a callback for
    * to mark that the {@link Observable} has completed emitting all elements.
    *
    * @param observer an observer instance implementing callbacks
    * @return a subscription which allows to be unsubscribed
    */
   Subscription subscribe(Observer<? super T> observer);

   /**
    /**
    * Subscribe an {@link Subscriber} which can get callbacks for each element
    * emitted, callbacks for when errors happen, and finally a callback for
    * to mark that the {@link Subscriber} has completed emitting all elements.
    *
    * {@link Subscriber} allows implementations to directly unsubscribe from the
    * {@link Observable}, for example, if certain condition is met as elements
    * are consumed. This is particularly helpful for implementing methods such
    * as {@link java.util.concurrent.ConcurrentMap#containsValue(Object)}.
    *
    * @param subscriber an subscriber instance implementing callbacks with the
    *                   ability to unsubscribe
    * @return a subscription which allows to be unsubscribed
    */
   Subscription subscribe(Subscriber<? super T> subscriber);

   /**
    * Allows {@link Observable} subscriptions to be unsubscribed.
    */
   interface Subscription {
      /**
       * Unsuscribe the subscription.
       */
      void unsubscribe();

      /**
       * Find out whether the subscription is active or not.

       * @return true if the subscription has been unsubscribed, false otherwise
       */
      boolean isUnsubscribed();
   }

   /**
    * Observer can be registered with {@link Observable} to work with its
    * emitted elements, deal with errors and find out when {@link Observable}
    * has completed emitting all elements.
    */
   interface Observer<T> {
      /**
       * Callback for each element emitted by {@link Observable}.
       *
       * @param t emitted element
       */
      default void onNext(T t) {}

      /**
       * Callback if there's an error while emitting elements.
       *
       * @param t error reported
       */
      default void onError(Throwable t) {}

      /**
       * Callback when the {@link Observable} has completed emitting
       * all elements.
       */
      default void onCompleted() {}
   }

   /**
    * An {@link Observer} which can interact with its own {@link Subscription},
    * allowing it to query the subscription status or unsubscribe.
    */
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

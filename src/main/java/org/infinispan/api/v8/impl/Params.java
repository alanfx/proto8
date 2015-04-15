package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.Param;

import java.util.Arrays;
import java.util.List;

class Params {

   private static final Param<?>[] DEFAULTS = new Param<?>[]{
      Param.AccessMode.defaultValue(),
      Param.StreamMode.defaultValue(),
      Param.WaitMode.defaultValue(),
      Param.Lifespan.defaultValue()
   };

   final Param<?>[] params;

   private Params(Param<?>[] params) {
      this.params = params;
   }

   public boolean containsAll(Param<?>... ps) {
      List<Param<?>> paramsToCheck = Arrays.asList(ps);
      List<Param<?>> paramsCurrent = Arrays.asList(params);
      return paramsCurrent.containsAll(paramsToCheck);
   }

   public Params addAll(Param<?>... ps) {
      List<Param<?>> paramsToAdd = Arrays.asList(ps);
      Param<?>[] paramsAll = Arrays.copyOf(params, params.length);
      paramsToAdd.forEach(p -> paramsAll[p.id()] = p);
      return new Params(paramsAll);
   }

   @SuppressWarnings("unchecked")
   public <T> Param<T> get(int index) {
      return (Param<T>) params[index];
   }

   @Override
   public String toString() {
      return "Params=" + Arrays.toString(params);
   }

   public static Params create() {
      return new Params(DEFAULTS);
   }

   public static Params from(Param<?>... ps) {
      List<Param<?>> paramsToAdd = Arrays.asList(ps);
      List<Param<?>> paramsDefaults = Arrays.asList(DEFAULTS);
      if (paramsDefaults.containsAll(paramsToAdd))
         return create(); // All parameters are defaults, don't do more work

      Param<?>[] paramsAll = Arrays.copyOf(DEFAULTS, DEFAULTS.length);
      paramsToAdd.forEach(p -> paramsAll[p.id()] = p);
      return new Params(paramsAll);
   }

}

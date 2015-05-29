package org.infinispan.api.v8;

/**
 * Component status.
 */
public enum Status {
   // These options are just for guidance, there's no reason to change
   // Infinispan's status options except for maybe adding a few more,
   // as indicated in https://issues.jboss.org/browse/ISPN-5408
   STARTED, STOPPED;
}

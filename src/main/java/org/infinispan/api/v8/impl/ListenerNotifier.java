package org.infinispan.api.v8.impl;

import org.infinispan.api.v8.EntryView.ReadEntryView;
import org.infinispan.api.v8.Listeners.ReadWriteListeners;
import org.infinispan.api.v8.Listeners.WriteListeners;

interface ListenerNotifier<K, V> extends ReadWriteListeners<K, V>, WriteListeners<K, V> {

   void notifyOnCreate(ReadEntryView<K, V> created);
   void notifyOnModify(ReadEntryView<K, V> before, ReadEntryView<K, V> after);
   void notifyOnRemove(ReadEntryView<K, V> removed);

   void notifyOnWrite(ReadEntryView<K, V> write);

}

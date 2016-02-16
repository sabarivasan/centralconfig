package com.cvent.kvstore;

import com.google.common.base.Optional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A logical interface that performs storage operations for a given region.
 * Changes to values are audit-trailed in the special "audit" region.
 * The keys that it receives are expected to follow our naming conventions for
 * our hierarchical config store.
 *
 * Note that the actual transport is handled by the KVStoreDao interface.
 *
 * Different implementations of this interface might have different performance characteristics
 * , for example.
 *
 * Created by sviswanathan on 2/10/16.
 */
public interface KVStore {
   String DEFAULT_REGION = "default";
   String AUDIT_REGION = "audit";
   String DOCUMENT_REGION = "document";
   String NO_VALUE = "<None>";

   // Care should be taken to ensure that this separator is neutral to any key-value store
   String HIERARCHY_SEPARATOR = "/";
   String ARRAY_PREFIX = "...";

   String TRUE = "true";
   String FALSE = "false";
   Set<String> BOOLEAN_VALUES = new HashSet<String>() {{ add(TRUE);  add(FALSE); }};

   // The region for which this KVStore was created
   String region();

   // The KVStoreDAO for which this KVStore was created
   KVSStoreDao dao();

   /**
    * Adds a new key-value entry in the store.
    * If force flag is not set, it sets the value only if the it differs
    * from the value for the same key the default region.
    * Note that if the force flag is not set and the default value for the key changes
    * at a future time, you may "lose" this put.
    * @param key        the key (without the region)
    * @param value      the value
    * @param force      put value irrespective of value for key in default region
    * @return true if a change was made to the store
    * @throws KVStoreException
    */
   boolean put(String key, String value, String author, boolean force) throws KVStoreException;


   /**
    * Adds a new key-value entry in the store.
    * If force flag is not set, it sets the value only if the it differs
    * from the value for the same key the default region.
    * Note that if the force flag is not set and the default value for the key changes
    * at a future time, you may "lose" this put.
    * @param keyValue   the key-value pair
    * @param force      put value irrespective of value for key in default region
    * @return true if a change was made to the store
    * @throws KVStoreException
    */
   boolean put(KeyValue keyValue, String author, boolean force) throws KVStoreException;

   /**
    * Gets the primitive value for a given key.
    * @param key key to get value for
    * @return the proprietary string representation of the value (supports strings, number and boolean)
    */
   Optional<String> getValueAt(String key);

   Map<String, String> getHierarchyAt(String key);

   /**
    * Delete all keys
    */
   void destroy();
}

package com.cvent.kvstore;

import com.google.common.base.Optional;

import java.util.Collection;
import java.util.Map;

/**
 * The interface that represents CRUD operations with the KV store.
 * This interface allows us to have different KV Storage implementations like Consul
 * and also different clients.
 * It also protects us from storage eccentricities of particular kv databases like
 * base64 encoding, etc.
 *
 * Created by sviswanathan on 2/11/16.
 */
public interface KVSStoreDao {

   /**
    * The key includes region
    * @param key     the key to store
    * @param value   the value to store
    * @throws KVStoreException  if something went wrong
    */
   void put(String key, String value) throws KVStoreException;

   /**
    * Gets a single value at a key
    * @param key the key to get
    * @return the value for the given key
    */
   Optional<String> getValueAt(String key);

   /**
    * Retrieves a tree of key-value pair at a given key
    * @param key the key
    * @return a collection of key-value pairs
    */
   Optional<Collection<KeyValue>> getHierarchyAt(String key);

   Map<String, String> getHierarchyAsMap(String key);


   Optional<Collection<String>> getKeysAt(String key);

   void deleteKey(String key);

   void deleteHierarchyAt(String key);

}

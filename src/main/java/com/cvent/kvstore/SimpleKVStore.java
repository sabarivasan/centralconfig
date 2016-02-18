package com.cvent.kvstore;

import com.cvent.JsonSerializer;
import com.cvent.kvstore.model.AuditLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * A Consul-backed key-store
 * Created by sviswanathan on 2/10/16.
 */
public class SimpleKVStore implements KVStore {

   private final String document;
   private final String region;
   private final KVSStoreDao dao;

   SimpleKVStore(String document, String region, KVSStoreDao dao) {
      this.document = document;
      this.region = region;
      this.dao = dao;
   }

   @Override
   public String documentName() {
      return document;
   }

   public static KVStore forDefaultRegion(String documentName, KVSStoreDao dao) {
      return new SimpleKVStore(documentName, DEFAULT_REGION, dao);
   }

   public static KVStore forAuditRegion(String documentName, KVSStoreDao dao) {
      return new SimpleKVStore(documentName, AUDIT_REGION, dao);
   }

   public static KVStore forRegion(String documentName, String region, KVSStoreDao dao) {
      return new SimpleKVStore(documentName, region, dao);
   }

   // Documents will be stored in the default region of the special "document" document
   public static KVStore documentStoreFor(KVSStoreDao dao) {
      return new SimpleKVStore(DOCUMENT_REGION, DEFAULT_REGION, dao);
   }

   private boolean isDefaultRegion() {
      return KVStore.DEFAULT_REGION.equals(region);
   }

   public String region() {
      return region;
   }

   @Override
   public KVSStoreDao dao() {
      return dao;
   }

   public boolean put(String key, String value, String author, boolean force) throws KVStoreException {
      Preconditions.checkArgument(isNotBlank(key) && isNotBlank(value) && isNotBlank(author), "key, value and author are required");
      if (!force && !isDefaultRegion()) {
         Optional<String> val = dao.getValueAt(KeyProvider.defaultRegionKeyFor(document, key));
         if (!val.isPresent()) {
            throw KVStoreException.keyAbsentInDefault(key);
         } else if (val.get().equals(value)) {
            // If the value in the default region matches
            return false;
         }
      }

      String keyToUpsert = KeyProvider.keyFor(document, region, key);
      Optional<String> val = dao.getValueAt(keyToUpsert);
      String oldVal = val.or(NO_VALUE);
      if (!value.equals(oldVal)) {
         // Audit trail: Create an entry in audit region for the same key with an incremented revision number at the end
         Optional<Collection<String>> keys = dao.getKeysAt(KeyProvider.auditHierarchyFor(document, region, key));
         int newRevision = 1 + keys.or(Collections.emptyList()).stream().map(
               k -> Integer.valueOf(KeyProvider.auditRevisionFromAuditKey(k))).max(Comparator.naturalOrder()).orElse(0);
         String auditKey = KeyProvider.auditKeyForRevision(document, region, key, newRevision);

         AuditLog auditLog = new AuditLog(document, author, region, key, oldVal, value, newRevision);

         // Create the audit trail entry
         dao.put(auditKey, auditRecord(auditLog));

         // Create the actual entry
         dao.put(keyToUpsert, value);
         return true;
      } else {
         return false;
      }
   }

   public boolean put(KeyValue keyValue, String author, boolean force) throws KVStoreException {
      return put(keyValue.key(), keyValue.value(), author, force);
   }

   public Optional<String> getValueAt(String key) {
      Optional<String> val = dao.getValueAt(KeyProvider.keyFor(document, region, key));
      return val.isPresent() ? val : dao.getValueAt(KeyProvider.defaultRegionKeyFor(document, key));
   }

   public Map<String, String> getHierarchyAt(String key) {
      if (isDefaultRegion()) {
         // Optimization: no need to merge
         return dao.getHierarchyAsMap(KeyProvider.defaultRegionKeyFor(document, key), KeyProvider::keyFromDocumentRegionDbKey);
      } else {
         Map<String, String> regionVals = dao.getHierarchyAsMap(KeyProvider.keyFor(document, region, key),
               KeyProvider::keyFromDocumentRegionDbKey);
         Map<String, String> defaultVals = dao.getHierarchyAsMap(KeyProvider.defaultRegionKeyFor(document, key),
               KeyProvider::keyFromDocumentRegionDbKey);
         defaultVals.forEach((k, v) -> {
            if (!regionVals.containsKey(k)) regionVals.put(k, v);
         });
         return Collections.unmodifiableMap(regionVals);
      }
   }

   @Override
   public void destroy() {
      dao.deleteHierarchyAt(KeyProvider.keyForEntireRegion(document, region));
      dao.deleteHierarchyAt(KeyProvider.auditKeyForEntireRegion(document, region));
   }

   private static String auditRecord(AuditLog auditLog) {
      try {
         return JsonSerializer.toJson(auditLog);
      } catch (JsonProcessingException e) {
         throw new RuntimeException("Could not write audit log", e);
      }
   }


}

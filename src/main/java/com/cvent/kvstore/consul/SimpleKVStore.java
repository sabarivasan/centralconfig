package com.cvent.kvstore.consul;

import com.cvent.kvstore.KVSStoreDao;
import com.cvent.kvstore.KVStore;
import com.cvent.kvstore.KVStoreException;
import com.cvent.kvstore.KeyValue;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * A Consul-backed key-store
 * Created by sviswanathan on 2/10/16.
 */
public class SimpleKVStore implements KVStore {
   private static final String CONSUL_HIERARCHY_SEPARATOR = "/";

   private static final String DEFAULT_REGION_PREFIX = DEFAULT_REGION + CONSUL_HIERARCHY_SEPARATOR;
   private static final String AUDIT_REGION_PREFIX = AUDIT_REGION + CONSUL_HIERARCHY_SEPARATOR;
   private String region;
   private String regionPrefix;
   private KVSStoreDao dao;

   SimpleKVStore(String region, KVSStoreDao dao) {
      this.region = region;
      this.regionPrefix = region + CONSUL_HIERARCHY_SEPARATOR;
      this.dao = dao;
   }

   public static KVStore kvStoreForDefaultRegion(KVSStoreDao dao) {
      return new SimpleKVStore(DEFAULT_REGION, dao);
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
         Optional<String> val = dao.getValueAt(DEFAULT_REGION_PREFIX + key);
         if (!val.isPresent()) {
            throw KVStoreException.keyAbsentInDefault(key);
         } else if (val.get().equals(value)) {
            // If the value in the default region matches
            return false;
         }
      }

      Optional<String> val = dao.getValueAt(regionPrefix + key);
      String oldVal = val.or(NO_VALUE);
      if (!value.equals(oldVal)) {
         // Audit trail: Create an entry in audit region for the same key with an incremented revision number at the end
         Optional<Collection<String>> keys = dao.getKeysAt(Util.keyFor(AUDIT_REGION_PREFIX, region, key));
         int rev = keys.or(Collections.emptyList()).stream().map(
               k -> Integer.valueOf(Util.lastPart(k))).max(Comparator.naturalOrder()).orElse(0);
         String auditKey = Util.keyFor(AUDIT_REGION_PREFIX, region, key, rev + 1);

         // Create the audit trail entry
         dao.put(auditKey, Util.auditRecord(region, key, oldVal, value, author));

         // Create the actual entry
         dao.put(regionPrefix + key, value);
         return true;
      } else {
         return false;
      }
   }

   public boolean put(KeyValue keyValue, String author, boolean force) throws KVStoreException {
      return put(keyValue.key(), keyValue.value(), author, force);
   }

   public Optional<String> getValueAt(String key) {
      Optional<String> val = dao.getValueAt(regionPrefix + key);
      return val.isPresent()?val:dao.getValueAt(DEFAULT_REGION_PREFIX + key);
   }

   public Map<String, String> getHierarchyAt(String key) {
      Map<String, String> vals = dao.getHierarchyAsMap(regionPrefix + key);
      Map<String, String> defaultVals = dao.getHierarchyAsMap(DEFAULT_REGION_PREFIX + key);
      defaultVals.forEach((k,v) -> { if (!vals.containsKey(k)) vals.put(k, v); } );
      return vals;
   }

   @Override
   public void destroy() {
      dao.deleteHierarchyAt(regionPrefix);
      dao.deleteHierarchyAt(AUDIT_REGION_PREFIX + region);
   }

   private static class Util {

      // The portion of a key after the last "/"
      private static String lastPart(String key) {
         return key.substring(key.lastIndexOf(CONSUL_HIERARCHY_SEPARATOR) + 1);
      }

      // Assumes regionPrefix already has the "/"
      private static String keyFor(String regionPrefix, Object... keyPortions) {
         StringBuilder sb = new StringBuilder(regionPrefix.length() + 10*keyPortions.length);
         sb.append(regionPrefix);
         boolean first = true;
         for (Object portion: keyPortions) {
            if (!first) {
               sb.append(CONSUL_HIERARCHY_SEPARATOR);
            }
            first = false;
            sb.append(portion.toString());
         }
         return sb.toString();
      }

      public static String auditRecord(String region, String key, String oldVal, String newVal, String author) {
         return String.format("region=%s,key=%s,newVal=%s,oldVal=%s,author=%s",
               region, key, newVal, oldVal, author);
      }

      public static void main(String[] args) {
         Map<String, String> m = new HashMap<>();
         m.put("a", "1");
         m.put("b", "2");
         m.put("c", "3");

         Map<String, String> m2 = new HashMap<>();
         m2.put("a", "AA");
         m2.put("c", "CC");

         m2.forEach((k, v) -> m2.putIfAbsent(k, m.get(k)));

         m2.forEach((k, v) -> System.out.println(k + "=" + v));
      }
   }


}

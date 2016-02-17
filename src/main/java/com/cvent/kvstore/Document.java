package com.cvent.kvstore;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * A document is just a set of keys
 * Created by sviswanathan on 2/14/16.
 */
public class Document {
   public static final String KEY_SEPARATOR = "\n";
   private Set<String> keys;

   private Document(Set<String> keys) {
      this.keys = new TreeSet<>(keySortComparator());
      this.keys.addAll(keys);
   }

   public static Comparator<String> keySortComparator() {
      return (k1, k2) -> {
         String[] parts1 = k1.split(KVStore.HIERARCHY_SEPARATOR);
         String[] parts2 = k2.split(KVStore.HIERARCHY_SEPARATOR);
         int n = -1;
         while (++n < parts1.length && n < parts2.length) {
            int c = parts1[n].compareTo(parts2[n]);
            if (c != 0) {
               return c;
            }
         }
         return Integer.compare(parts1.length, parts2.length);
      };
   }

   public Iterator<String> iterateKeys() {
      return keys.iterator();
   }

   public Set<String> keys() {
      return keys;
   }

   public String serialize() {
      StringBuilder sb = new StringBuilder(1024);
      keys.forEach(k -> sb.append(k).append(KEY_SEPARATOR));
      return sb.toString();
   }

   public static Document deserialize(String serialized) {
      Set<String> keys = new HashSet<>();
      for (String key: serialized.split(KEY_SEPARATOR)) {
         keys.add(key);
      }
      return new Document(keys);
   }

   public static Document from(Set<String> keys) {
      return new Document(keys);
   }

   public static Set<String> sort(Set<String> keys) {
      Set<String> sorted = new TreeSet<>(keySortComparator());
      sorted.addAll(keys);
      return sorted;
   }

}

package com.cvent.kvstore;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Created by sviswanathan on 2/14/16.
 */
public class KeySet {
   private Set<String> keys;

   private KeySet(Set<String> keys) {
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

   public static KeySet from(Set<String> keys) {
      return new KeySet(keys);
   }

   public static Set<String> sort(Set<String> keys) {
      Set<String> sorted = new TreeSet<>(keySortComparator());
      sorted.addAll(keys);
      return sorted;
   }
}

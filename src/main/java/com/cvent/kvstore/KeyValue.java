package com.cvent.kvstore;

/**
 * Immutable class representing a key and value combination
 * Created by sviswanathan on 2/10/16.
 */
public class KeyValue {

   public final String key;
   public final String value;

   private KeyValue(String key, String value) {
      this.key = key;
      this.value = value;
   }

   public String key() {
      return key;
   }

   public String value() {
      return value;
   }

   public static KeyValue from (String key, String value) {
      return new KeyValue(key, value);
   }
}

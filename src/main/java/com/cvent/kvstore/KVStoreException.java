package com.cvent.kvstore;

/**
 * Custom exception class for our KVStore
 *
 * Created by sviswanathan on 2/10/16.
 */
public class KVStoreException extends Exception {
   public static enum Reason {
      KEY_ABSENT_IN_DEFAULT,
      WRITE_FAILED,
   }

   public final Reason reason;

   private KVStoreException(Reason reason, String message) {
      super(message);
      this.reason = reason;
   }

   private KVStoreException(Reason reason, String message, Throwable cause) {
      super(message, cause);
      this.reason = reason;
   }

   public static KVStoreException keyAbsentInDefault(String key) {
      return new KVStoreException(Reason.KEY_ABSENT_IN_DEFAULT,
            String.format("key %s not present in %s region", key, KVStore.DEFAULT_REGION));
   }

   public static KVStoreException writeFailed(String key) {
      return new KVStoreException(Reason.WRITE_FAILED,
            String.format("Write failed for key %s", key));
   }

   public Reason getReason() {
      return reason;
   }

}

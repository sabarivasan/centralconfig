package com.cvent.kvstore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Represents an audit log for a change to the KV store
 * Created by sviswanathan on 2/15/16.
 */
public class AuditLog {
   @NotEmpty
   @JsonProperty
   private String document;

   @NotEmpty
   @JsonProperty
   private String author;

   @NotEmpty
   @JsonProperty
   private String region;

   @NotEmpty
   @JsonProperty
   private String key;

   @NotEmpty
   @JsonProperty
   private String oldValue;

   @NotEmpty
   @JsonProperty
   private String newValue;

   @NotEmpty
   @JsonProperty
   private int version;

   @NotEmpty
   @JsonProperty
   private long timestamp;

   public AuditLog() {
   }

   public AuditLog(String document, String author, String region, String key, String oldValue, String newValue, int version) {
      this.document = document;
      this.author = author;
      this.region = region;
      this.key = key;
      this.oldValue = oldValue;
      this.newValue = newValue;
      this.version = version;
      this.timestamp = System.currentTimeMillis();
   }

   public String getDocument() {
      return document;
   }

   public void setDocument(String document) {
      this.document = document;
   }

   public String getAuthor() {
      return author;
   }

   public void setAuthor(String author) {
      this.author = author;
   }

   public String getRegion() {
      return region;
   }

   public void setRegion(String region) {
      this.region = region;
   }

   public String getKey() {
      return key;
   }

   public void setKey(String key) {
      this.key = key;
   }

   public String getOldValue() {
      return oldValue;
   }

   public void setOldValue(String oldValue) {
      this.oldValue = oldValue;
   }

   public String getNewValue() {
      return newValue;
   }

   public void setNewValue(String newValue) {
      this.newValue = newValue;
   }

   public long getTimestamp() {
      return timestamp;
   }

   public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
   }

   public int getVersion() {
      return version;
   }

   public void setVersion(int version) {
      this.version = version;
   }

   @Override
   public String toString() {
      return
            "author=" + author +
                  ", region=" + region +
                  ", key=" + key +
                  ", oldValue=" + oldValue +
                  ", newValue=" + newValue +
                  ", version=" + version +
                  ", timestamp=" + ISODateTimeFormat.basicDateTime().print(timestamp);

   }
}

package com.cvent.kvstore;

/**
 * Created by sviswanathan on 2/14/16.
 */
public enum DocumentType {
   YAML,
   JSON,;

   public boolean isYAML() {
      return YAML == this;
   }

}

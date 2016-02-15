package com.cvent.kvstore.dw;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by sviswanathan on 2/10/16.
 */
public class ConsulKVStoreConfig {

   @JsonProperty
   private String consulEndpoint;

   @JsonProperty
   private String password;

   public ConsulKVStoreConfig() {
   }

   public String getConsulEndpoint() {
      return consulEndpoint;
   }

   public void setConsulEndpoint(String consulEndpoint) {
      this.consulEndpoint = consulEndpoint;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }
}

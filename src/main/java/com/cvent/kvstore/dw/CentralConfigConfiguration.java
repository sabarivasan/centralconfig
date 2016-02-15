package com.cvent.kvstore.dw;

import com.cvent.CventConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Common configuration
 * Created by sviswanathan on 2/15/16.
 */
public class CentralConfigConfiguration extends CventConfiguration {

   @JsonProperty
   private ConsulKVStoreConfig consulKVStoreConfig;

   public CentralConfigConfiguration() {}

   public ConsulKVStoreConfig getConsulKVStoreConfig() {
      return consulKVStoreConfig;
   }

   public void setConsulKVStoreConfig(ConsulKVStoreConfig consulKVStoreConfig) {
      this.consulKVStoreConfig = consulKVStoreConfig;
   }
}

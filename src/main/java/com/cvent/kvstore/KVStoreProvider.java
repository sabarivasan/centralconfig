package com.cvent.kvstore;

import com.cvent.kvstore.dw.ConsulKVStoreConfig;

/**
 *
 * Created by sviswanathan on 2/10/16.
 */
public class KVStoreProvider {

   private ConsulKVStoreConfig config;
   private String region;

   public KVStoreProvider(ConsulKVStoreConfig config, String region) {
      this.config = config;
      this.region = region;
   }


}

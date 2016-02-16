package com.cvent.kvstore.dw;

import com.cvent.CventApplication;
import com.cvent.kvstore.resources.AuditTrailResource;
import com.cvent.kvstore.resources.ConfigGenResource;
import com.cvent.kvstore.resources.DocumentResource;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Created by sviswanathan on 2/15/16.
 */
public class CentralConfigApplication extends CventApplication<CentralConfigConfiguration> {

   @Override
   public String getName() {
      return "config-service";
   }

   @Override
   public void initialize(Bootstrap<CentralConfigConfiguration> bootstrap) {
      super.initialize(bootstrap);
   }

   @Override
   public void run(CentralConfigConfiguration config, Environment environment) throws Exception {
      super.run(config, environment);
      environment.jersey().register(new ConfigGenResource(config.getConsulKVStoreConfig()));
      environment.jersey().register(new AuditTrailResource(config.getConsulKVStoreConfig()));
      environment.jersey().register(new DocumentResource(config.getConsulKVStoreConfig()));
   }

   public static void main(String[] args) throws Exception {
       new CentralConfigApplication().run(args);
   }

}

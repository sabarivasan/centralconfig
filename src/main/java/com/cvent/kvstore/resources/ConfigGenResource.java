package com.cvent.kvstore.resources;


import com.codahale.metrics.annotation.Timed;
import com.cvent.kvstore.ConfigGenerator;
import com.cvent.kvstore.DocumentType;
import com.cvent.kvstore.KeySet;
import com.cvent.kvstore.TemplateToKeyset;
import com.cvent.kvstore.consul.ConsulKVDaoEcwid;
import com.cvent.kvstore.SimpleKVStore;
import com.cvent.kvstore.dw.ConsulKVStoreConfig;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A resource for generating config
 * Created by sviswanathan on 2/15/16.
 */

@Path("/config-gen")
@Consumes(MediaType.APPLICATION_JSON + ",text/yaml")
@Produces(MediaType.APPLICATION_JSON + ",text/yaml")
public class ConfigGenResource {
   private ConsulKVStoreConfig config;

   public ConfigGenResource(ConsulKVStoreConfig config) {
      this.config = config;
   }

   @GET
   @Timed
   public Response generateConfig(@NotEmpty @QueryParam("template") String template,
                                  @NotEmpty @QueryParam("region") String region,
                                         @QueryParam("format") String format,
                                         @HeaderParam("Content-Type") String documentType) throws IOException {
      ConfigGenerator configGenerator = new ConfigGenerator(SimpleKVStore.forRegion(region,
            new ConsulKVDaoEcwid(config)));


//      DocumentType inputDocType = MediaType.APPLICATION_JSON.equals(documentType)?DocumentType.JSON: DocumentType.YAML;
      KeySet keySet = TemplateToKeyset.templateToKeySet(new File(template));
      DocumentType outputDocType = "json".equals(format)?DocumentType.JSON: DocumentType.YAML;
      File tmp = File.createTempFile("out", outputDocType.name());
      try (FileOutputStream os = new FileOutputStream(tmp)) {
         configGenerator.generate(keySet, outputDocType, os);
      } catch (Exception e) {
         return Response.serverError().build();
      }
      return Response.ok(new FileInputStream(tmp)).build();
   }
}

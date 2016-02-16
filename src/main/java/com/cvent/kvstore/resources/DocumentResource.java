package com.cvent.kvstore.resources;

import com.cvent.kvstore.DocumentType;
import com.cvent.kvstore.KVStore;
import com.cvent.kvstore.KVStoreException;
import com.cvent.kvstore.KeySet;
import com.cvent.kvstore.SimpleKVStore;
import com.cvent.kvstore.TemplateToKeyset;
import com.cvent.kvstore.consul.ConsulKVDaoEcwid;
import com.cvent.kvstore.dw.ConsulKVStoreConfig;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit.http.Body;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;

/**
 * CRUD operations for a document.
 * A document is just a KeySet.
 *
 * Created by sviswanathan on 2/15/16.
 */

@Consumes({MediaType.APPLICATION_JSON, "text/yaml"})
@Produces({MediaType.APPLICATION_JSON, "text/yaml"})
public class DocumentResource {

   private KVStore docKVStore;

   public DocumentResource(ConsulKVStoreConfig config) {
      docKVStore = SimpleKVStore.forRegion(KVStore.DOCUMENT_REGION, new ConsulKVDaoEcwid(config));
   }

   @POST
   @Path("/{name}")
   /**
    * Can create a document by passing in the body a yaml or json file
    */
   public void createDoc(@NotNull @PathParam("name") String name,
                           @NotNull @Body InputStream file,
                           @NotNull @NotEmpty @QueryParam("author") String author,
                           @NotNull @HeaderParam("Content-Type") String contentType) throws IOException, KVStoreException {

      String key = KVStore.DEFAULT_REGION + KVStore.HIERARCHY_SEPARATOR + name;
      if (docKVStore.getValueAt(key).isPresent()) {
         throw new IllegalArgumentException(String.format("Document named %s already exists. Did you mean to use PUT to update it?", name));
      }

      DocumentType docType;
      if (MediaType.APPLICATION_JSON.equals(contentType)) {
         docType = DocumentType.JSON;
      } else {
         docType = DocumentType.YAML;
      }
      KeySet keySet = TemplateToKeyset.from(file, docType);
      StringBuilder sb = new StringBuilder(1024);
      keySet.keys().forEach(k -> sb.append(k).append("\n"));

      docKVStore.put(key, sb.toString(), author, true);
   }


}

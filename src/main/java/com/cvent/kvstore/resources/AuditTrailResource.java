package com.cvent.kvstore.resources;

import com.codahale.metrics.annotation.Timed;
import com.cvent.JsonSerializer;
import com.cvent.kvstore.KVStore;
import com.cvent.kvstore.SimpleKVStore;
import com.cvent.kvstore.consul.ConsulKVDaoEcwid;
import com.cvent.kvstore.dw.ConsulKVStoreConfig;
import com.cvent.kvstore.model.AuditLog;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A resource that extracts the audit log entries
 * Created by sviswanathan on 2/15/16.
 */
@Path("/audit-trail")
@Produces(MediaType.APPLICATION_JSON + ",text/yaml")
public class AuditTrailResource {
   private final ConsulKVStoreConfig config;

   public AuditTrailResource(ConsulKVStoreConfig config) {
      this.config = config;
   }

   @GET
   @Timed
   public List<AuditLog> getAuditLogs(@NotNull @NotEmpty @QueryParam("document") String documentName,
                                      @NotNull @NotEmpty @QueryParam("region") String region,
                                      @NotNull @NotEmpty @QueryParam("key") String key,
                                      @QueryParam("author") String author) throws IOException {

      KVStore auditKVStore = SimpleKVStore.forAuditRegion(documentName,
            new ConsulKVDaoEcwid(config));

      Map<String, String> logs = auditKVStore.getHierarchyAt(region + KVStore.HIERARCHY_SEPARATOR + key);
      List<AuditLog> ret = new LinkedList<>();
      for (Map.Entry<String, String> log: logs.entrySet()) {
         AuditLog l = JsonSerializer.fromJson(log.getValue(), AuditLog.class);
         if (author != null && !author.equalsIgnoreCase(l.getAuthor())) {
            continue;
         }
         ret.add(l);
      }
      return ret;
   }

}

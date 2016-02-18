package com.cvent.kvstore.resources;


import com.codahale.metrics.annotation.Timed;
import com.cvent.kvstore.ConfigGenerator;
import com.cvent.kvstore.Document;
import com.cvent.kvstore.DocumentType;
import com.cvent.kvstore.KVStore;
import com.cvent.kvstore.SimpleKVStore;
import com.cvent.kvstore.consul.ConsulKVDaoEcwid;
import com.cvent.kvstore.dw.ConsulKVStoreConfig;
import com.google.common.base.Optional;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
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
    private KVStore docKVStore;

    public ConfigGenResource(ConsulKVStoreConfig config) {
        this.config = config;
        docKVStore = SimpleKVStore.documentStoreFor(new ConsulKVDaoEcwid(config));
    }

//    @GET
//    @Timed
//    public Response generateConfigFromTemplate(@NotEmpty @QueryParam("template") String template,
//                                               @NotEmpty @QueryParam("region") String region,
//                                               @QueryParam("format") String format,
//                                               @HeaderParam("Content-Type") String documentType) throws IOException {
//        ConfigGenerator configGenerator = new ConfigGenerator(SimpleKVStore.forRegion(region,
//              new ConsulKVDaoEcwid(config)));
//
//        Document document = TemplateToDocument.from(new File(template));
//        return Response.ok(new FileInputStream(writeDocumentToFile(format, configGenerator, document))).build();
//    }


    @GET
    @Timed
    @Path("/{document}/{region}")
    public Response generateConfigFromDocument(@NotEmpty @PathParam("document") String documentName,
                                               @NotEmpty @PathParam("region") String region,
                                               @QueryParam("format") String format) throws IOException {
        ConfigGenerator configGenerator = new ConfigGenerator(SimpleKVStore.forRegion(documentName, region,
              new ConsulKVDaoEcwid(config)));
        Optional<String> serializedDoc = docKVStore.getValueAt(documentName);
        if (!serializedDoc.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Document document = Document.deserialize(serializedDoc.get());
        return Response.ok(new FileInputStream(writeDocumentToFile(format, configGenerator, document))).build();
    }

    private File writeDocumentToFile(String format, ConfigGenerator configGenerator, Document document) throws IOException {
        DocumentType outputDocType = "json".equals(format) ? DocumentType.JSON : DocumentType.YAML;
        return configGenerator.generateToTmpFile(document, outputDocType);
    }

}

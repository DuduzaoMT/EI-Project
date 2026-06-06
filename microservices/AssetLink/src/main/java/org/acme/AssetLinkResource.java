package org.acme;

import java.net.URI;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.MediaType;

@Path("AssetLink")
public class AssetLinkResource {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true")
    boolean schemaCreate;

    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:9092")
    String kafkaBootstrapServers;

    void config(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
        }
    }

    private void initdb() {
        client.query("DROP TABLE IF EXISTS AssetLink").execute()
        .flatMap(r -> client.query("CREATE TABLE AssetLink (id SERIAL PRIMARY KEY, idProsumer BIGINT UNSIGNED, idUtilityOperator BIGINT UNSIGNED, CONSTRAINT UC_Loyal UNIQUE (idProsumer,idUtilityOperator))").execute())
        .flatMap(r -> client.query(" INSERT INTO AssetLink (idProsumer,idUtilityOperator) VALUES (1,1)").execute())
        .flatMap(r -> client.query(" INSERT INTO AssetLink (idProsumer,idUtilityOperator) VALUES (2,1)").execute())
        .flatMap(r -> client.query(" INSERT INTO AssetLink (idProsumer,idUtilityOperator) VALUES (1,3)").execute())
        .flatMap(r -> client.query(" INSERT INTO AssetLink (idProsumer,idUtilityOperator) VALUES (4,2)").execute())
        .await().indefinitely();
    }

    private void createKafkaTopic(String topicName) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        try (AdminClient adminClient = AdminClient.create(props)) {
            NewTopic newTopic = new NewTopic(topicName, 1, (short) 1);
            adminClient.createTopics(Collections.singletonList(newTopic)).all().get(10, TimeUnit.SECONDS);
            System.out.println("Kafka topic created: " + topicName);
        } catch (Exception e) {
            System.err.println("Failed to create Kafka topic " + topicName + ": " + e.getMessage());
        }
    }

    private void deleteKafkaTopic(String topicName) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        try (AdminClient adminClient = AdminClient.create(props)) {
            adminClient.deleteTopics(Collections.singletonList(topicName)).all().get(10, TimeUnit.SECONDS);
            System.out.println("Kafka topic deleted: " + topicName);
        } catch (Exception e) {
            System.err.println("Failed to delete Kafka topic " + topicName + ": " + e.getMessage());
        }
    }

    @GET
    public Multi<AssetLink> get() {
        return AssetLink.findAll(client);
    }

    @GET
    @Path("active")
    public Multi<AssetLink> getActive(@QueryParam("gridCellId") String gridCellId) {
        return AssetLink.findByGridCell(client, gridCellId);
    }

    @GET
    @Path("{id}")
    public Uni<Response> getSingle(Long id) {
        return AssetLink.findById(client, id)
                .onItem().transform(assetlink -> assetlink != null ? Response.ok(assetlink) : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(ResponseBuilder::build);
    }

    @GET
    @Path("{idProsumer}/{idUtilityOperator}")
    public Uni<Response> getDual(Long idProsumer, Long idUtilityOperator) {
        return AssetLink.findById2(client, idProsumer, idUtilityOperator)
                .onItem().transform(assetlink -> assetlink != null ? Response.ok(assetlink) : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(ResponseBuilder::build);
    }

    @POST
    public Uni<Response> create(AssetLink assetlink) {
        return assetlink.save(client, assetlink.idProsumer, assetlink.idUtilityOperator)
                .onItem().transform(id -> {
                    createKafkaTopic(id + "-" + assetlink.idUtilityOperator);
                    return id;
                })
                .onItem().transform(id -> URI.create("/AssetLink/" + id))
                .onItem().transform(uri -> Response.created(uri).build());
    }

    @DELETE
    @Path("{id}")
    public Uni<Response> delete(Long id) {
        return AssetLink.findById(client, id)
                .onItem().transformToUni(assetlink -> {
                    if (assetlink == null) {
                        return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
                    }
                    String topicName = id + "-" + assetlink.idUtilityOperator;
                    return AssetLink.delete(client, id)
                            .onItem().transform(deleted -> {
                                if (deleted) deleteKafkaTopic(topicName);
                                return deleted ?
                                    Response.status(Response.Status.NO_CONTENT).build() :
                                    Response.status(Response.Status.NOT_FOUND).build();
                            });
                });
    }

    @PUT
    @Path("/{id}/{idProsumer}/{idUtilityOperator}")
    public Uni<Response> update(Long id, Long idProsumer, Long idUtilityOperator) {
        return AssetLink.update(client, id, idProsumer, idUtilityOperator)
                .onItem().transform(updated -> updated ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

}

package org.acme;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.time.LocalDateTime;
import org.json.JSONObject;

@Path("GridBalancing")
public class GridBalancingResource {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true")
    boolean schemaCreate;

    @Channel("balancing-recommendation")
    Emitter<String> eventEmitter;

    void config(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
        }
    }

    private void initdb() {
        client.query("DROP TABLE IF EXISTS GridBalancing").execute()
        .flatMap(r -> client.query("CREATE TABLE GridBalancing ("
                + "id SERIAL PRIMARY KEY, "
                + "timestamp DATETIME NOT NULL, "
                + "source_grid_cell TEXT NOT NULL, "
                + "target_grid_cell TEXT NOT NULL, "
                + "recommended_action TEXT NOT NULL, "
                + "power_kw FLOAT, "
                + "status TEXT NOT NULL)").execute())
        .await().indefinitely();
    }

    @POST
    @Path("Emit")
    public Uni<Response> emitEvent(GridBalancing event) {
        event.timestamp = LocalDateTime.now();
        event.status = "PENDING";

        String insertQuery = String.format(
            "INSERT INTO GridBalancing (timestamp, source_grid_cell, target_grid_cell, recommended_action, power_kw, status) " +
            "VALUES ('%s', '%s', '%s', '%s', %f, '%s')",
            event.timestamp, event.source_grid_cell, event.target_grid_cell, event.recommended_action, event.power_kw, event.status
        );

        return client.query(insertQuery).execute()
            .onItem().transformToUni(pgRowSet -> {
                long insertedId = pgRowSet.property(io.vertx.mutiny.mysqlclient.MySQLClient.LAST_INSERTED_ID);

                JSONObject kafkaMsg = new JSONObject();
                kafkaMsg.put("event_id", insertedId);
                kafkaMsg.put("recommended_action", event.recommended_action);
                kafkaMsg.put("source_grid_cell", event.source_grid_cell);
                kafkaMsg.put("target_grid_cell", event.target_grid_cell);
                kafkaMsg.put("power_kw", event.power_kw);

                eventEmitter.send(kafkaMsg.toString());

                String updateQuery = "UPDATE GridBalancing SET status = 'PUBLISHED' WHERE id = " + insertedId;

                return client.query(updateQuery).execute()
                    .onItem().transform(updateSet -> {
                        event.id = insertedId;
                        event.status = "PUBLISHED";
                        return Response.ok(event).status(Response.Status.CREATED).build();
                    });
            });
    }

    @GET
    public Multi<GridBalancing> get() {
        return GridBalancing.findAll(client);
    }

    @GET
    @Path("{id}")
    public Uni<Response> getSingle(Long id) {
        return GridBalancing.findById(client, id)
                .onItem().transform(gridEvent -> gridEvent != null ? Response.ok(gridEvent) : Response.status(Response.Status.NOT_FOUND)) 
                .onItem().transform(ResponseBuilder::build); 
    }

    @GET
    @Path("source/{sourceGridCell}")
    public Multi<GridBalancing> getBySourceGridCell(@PathParam("sourceGridCell") String sourceGridCell) {
        return GridBalancing.findBySourceGridCell(client, sourceGridCell);
    }

    @GET
    @Path("target/{targetGridCell}")
    public Multi<GridBalancing> getByTargetGridCell(@PathParam("targetGridCell") String targetGridCell) {
        return GridBalancing.findByTargetGridCell(client, targetGridCell);
    }

    @GET
    @Path("status/{status}")
    public Multi<GridBalancing> getByStatus(@PathParam("status") String status) {
        return GridBalancing.findByStatus(client, status);
    }
}
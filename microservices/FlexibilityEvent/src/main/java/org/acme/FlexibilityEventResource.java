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
import java.util.Locale;
import org.json.JSONObject;

@Path("Flexibility")
public class FlexibilityEventResource {

    private static final float HIGH_SOC_THRESHOLD = 80.0f;
    private static final float LOW_SOC_THRESHOLD = 20.0f;

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true")
    boolean schemaCreate;

    @Channel("flexibility-offers")
    Emitter<String> eventEmitter;

    void config(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
        }
    }

    private void initdb() {
        client.query("DROP TABLE IF EXISTS FlexibilityEvent").execute()
        .flatMap(r -> client.query("CREATE TABLE FlexibilityEvent ("
                + "id SERIAL PRIMARY KEY, "
                + "timestamp DATETIME NOT NULL, "
                + "asset_id TEXT NOT NULL, "
                + "prosumer_id BIGINT, "
                + "grid_cell_id TEXT NOT NULL, "
                + "logic_type TEXT NOT NULL, "
                + "proposed_action TEXT NOT NULL, "
                + "incentive_value FLOAT, "
                + "target_value_kw FLOAT, "
                + "telemetry_reference_id BIGINT, "
                + "status TEXT NOT NULL)").execute())
        .await().indefinitely();
    }

    @POST
    @Path("Emit")
    public Uni<Response> emitEvent(FlexibilityEvent event) {
        event.timestamp = LocalDateTime.now();
        event.status = "PENDING";

        String insertQuery = String.format(Locale.US,
            "INSERT INTO FlexibilityEvent (timestamp, asset_id, prosumer_id, grid_cell_id, logic_type, proposed_action, incentive_value, target_value_kw, telemetry_reference_id, status) " +
            "VALUES ('%s', '%s', %s, '%s', '%s', '%s', %f, %f, %s, '%s')",
            event.timestamp, event.asset_id, event.prosumer_id != null ? event.prosumer_id : "NULL", event.grid_cell_id, event.logic_type, event.proposed_action, 
            event.incentive_value, event.target_value_kw, event.telemetry_reference_id != null ? event.telemetry_reference_id : "NULL", event.status
        );

        return client.query(insertQuery).execute()
            .onItem().transformToUni(pgRowSet -> {
                
                long insertedId = pgRowSet.property(io.vertx.mutiny.mysqlclient.MySQLClient.LAST_INSERTED_ID);
                
                JSONObject kafkaMsg = new JSONObject();
                kafkaMsg.put("event_id", insertedId);
                kafkaMsg.put("action", event.proposed_action);
                kafkaMsg.put("asset_id", event.asset_id);
                kafkaMsg.put("prosumer_id", event.prosumer_id);
                kafkaMsg.put("value_kw", event.target_value_kw);
                
                eventEmitter.send(kafkaMsg.toString());
                
                String updateQuery = "UPDATE FlexibilityEvent SET status = 'PUBLISHED' WHERE id = " + insertedId;
                
                return client.query(updateQuery).execute()
                    .onItem().transform(updateSet -> {
                        event.id = insertedId;
                        event.status = "PUBLISHED"; 
                        return Response.ok(event).status(Response.Status.CREATED).build();
                    });
            });
    }

    @POST
    @Path("Evaluate")
    public Response evaluateFlexibility(FlexibilityEvaluationRequest request) {
        String evaluation;
        String recommendedAction;
        String reason;

        if (request.socPercent != null && request.socPercent <= LOW_SOC_THRESHOLD) {
            evaluation = "BALANCING_UNAVAILABLE";
            recommendedAction = "UNAVAILABLE";
            reason = "Asset state of charge is too low to participate in balancing actions.";
        } else if (request.socPercent != null && request.socPercent >= HIGH_SOC_THRESHOLD
                && "HIGH".equalsIgnoreCase(request.marketPrice)) {
            evaluation = "ARBITRAGE_OPPORTUNITY";
            recommendedAction = "SELL";
            reason = "High state of charge combined with a high market price presents an arbitrage opportunity.";
        } else {
            evaluation = "EVALUATED_NO_ACTION";
            recommendedAction = "NONE";
            reason = "No actionable flexibility opportunity was identified for the current conditions.";
        }

        JSONObject result = new JSONObject();
        result.put("assetId", request.assetId);
        result.put("prosumerId", request.prosumerId);
        result.put("gridCellId", request.gridCellId);
        result.put("marketPrice", request.marketPrice);
        result.put("socPercent", request.socPercent);
        result.put("evaluation", evaluation);
        result.put("recommendedAction", recommendedAction);
        result.put("reason", reason);

        return Response.ok(result.toString(), MediaType.APPLICATION_JSON).build();
    }

    @GET
    public Multi<FlexibilityEvent> get() {
        return FlexibilityEvent.findAll(client);
    }

    @GET
    @Path("{id}")
    public Uni<Response> getSingle(Long id) {
        return FlexibilityEvent.findById(client, id)
                .onItem().transform(flexEvent -> flexEvent != null ? Response.ok(flexEvent) : Response.status(Response.Status.NOT_FOUND)) 
                .onItem().transform(ResponseBuilder::build); 
    }

    @GET
    @Path("grid/{gridCellId}")
    public Multi<FlexibilityEvent> getByGridCell(@PathParam("gridCellId") String gridCellId) {
        return FlexibilityEvent.findByGridCellId(client, gridCellId);
    }

    @GET
    @Path("status/{status}")
    public Multi<FlexibilityEvent> getByStatus(@PathParam("status") String status) {
        return FlexibilityEvent.findByStatus(client, status);
    }
}
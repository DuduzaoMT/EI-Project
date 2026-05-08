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

@Path("EnergyAnalytics")
public class EnergyAnalyticsResource {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true")
    boolean schemaCreate;

    @Channel("energy-discharged-zone")
    Emitter<String> dischargedEmitter;

    @Channel("generated-energy-prosumer")
    Emitter<String> generatedEmitter;

    @Channel("consumed-energy-prosumer")
    Emitter<String> consumedEmitter;

    @Channel("average-soc")
    Emitter<String> socEmitter;

    void config(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
        }
    }

    private void initdb() {
        client.query("DROP TABLE IF EXISTS EnergyAnalytics").execute()
        .flatMap(r -> client.query("CREATE TABLE EnergyAnalytics ("
                + "id SERIAL PRIMARY KEY, "
                + "timestamp DATETIME NOT NULL, "
                + "metric_type TEXT NOT NULL, "
                + "reference_id TEXT NOT NULL, "
                + "metric_value FLOAT NOT NULL, "
                + "unit TEXT NOT NULL, "
                + "status TEXT NOT NULL)").execute())
        .await().indefinitely();
    }

    @POST
    @Path("Publish")
    public Uni<Response> recordMetric(EnergyAnalytics event) {
        event.timestamp = LocalDateTime.now();
        event.status = "PENDING";

        String insertQuery = String.format(
            "INSERT INTO EnergyAnalytics (timestamp, metric_type, reference_id, metric_value, unit, status) " +
            "VALUES ('%s', '%s', '%s', %f, '%s', '%s')",
            event.timestamp, event.metric_type, event.reference_id, event.metric_value, event.unit, event.status
        );

        return client.query(insertQuery).execute()
            .onItem().transformToUni(pgRowSet -> {
                
                long insertedId = pgRowSet.property(io.vertx.mutiny.mysqlclient.MySQLClient.LAST_INSERTED_ID);
                
                JSONObject kafkaMsg = new JSONObject();
                kafkaMsg.put("analytics_id", insertedId);
                kafkaMsg.put("metric_type", event.metric_type);
                kafkaMsg.put("reference_id", event.reference_id);
                kafkaMsg.put("metric_value", event.metric_value);
                kafkaMsg.put("unit", event.unit);
                
                String messageStr = kafkaMsg.toString();
                
                Response.Status tempStatus = Response.Status.CREATED;
                switch (event.metric_type) {
                    case "ENERGY_DISCHARGED_BY_ZONE":
                        dischargedEmitter.send(messageStr);
                        event.status = "PUBLISHED";
                        break;
                    case "GENERATED_ENERGY_BY_PROSUMER":
                        generatedEmitter.send(messageStr);
                        event.status = "PUBLISHED";
                        break;
                    case "CONSUMED_ENERGY_BY_PROSUMER":
                        consumedEmitter.send(messageStr);
                        event.status = "PUBLISHED";
                        break;
                    case "AVERAGE_SOC":
                        socEmitter.send(messageStr);
                        event.status = "PUBLISHED";
                        break;
                    default:
                        System.err.println("Unrecognized metric_type: " + event.metric_type);
                        event.status = "ERROR";
                        tempStatus = Response.Status.BAD_REQUEST;
                        break;
                }
                
                String updateQuery = "UPDATE EnergyAnalytics SET status = '" + event.status + "' WHERE id = " + insertedId;
                
                final Response.Status finalHttpResponseStatus = tempStatus;
                
                return client.query(updateQuery).execute()
                    .onItem().transform(updateSet -> {
                        event.id = insertedId;
                        return Response.ok(event).status(finalHttpResponseStatus).build();
                    });
            });
    }

    @GET
    public Multi<EnergyAnalytics> get() {
        return EnergyAnalytics.findAll(client);
    }

    @GET
    @Path("{id}")
    public Uni<Response> getSingle(Long id) {
        return EnergyAnalytics.findById(client, id)
                .onItem().transform(energyEvent -> energyEvent != null ? Response.ok(energyEvent) : Response.status(Response.Status.NOT_FOUND)) 
                .onItem().transform(ResponseBuilder::build); 
    }

    @GET
    @Path("metric_type/{metricType}")
    public Multi<EnergyAnalytics> getByMetricType(@PathParam("metricType") String metricType) {
        return EnergyAnalytics.findByMetricType(client, metricType);
    }
}
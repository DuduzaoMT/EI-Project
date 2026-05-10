package org.acme;

import java.net.URI;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.MediaType;

@Path("UtilityOperator")
public class UtilityOperatorResource {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;
    
    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true") 
    boolean schemaCreate ;

    void config(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
        }
    }
    
    private void initdb() {
        // In a production environment this configuration SHOULD NOT be used
        client.query("DROP TABLE IF EXISTS GridCell").execute()
        .flatMap(r -> client.query("DROP TABLE IF EXISTS UtilityOperator").execute())
        .flatMap(r -> client.query("CREATE TABLE UtilityOperator (id SERIAL PRIMARY KEY, name TEXT NOT NULL, location TEXT NOT NULL)").execute())
        .flatMap(r -> client.query("CREATE TABLE GridCell (id SERIAL PRIMARY KEY, operator_id BIGINT UNSIGNED, grid_cell_id TEXT NOT NULL, location TEXT NOT NULL, max_load_mw FLOAT)").execute())
        .flatMap(r -> client.query("INSERT INTO UtilityOperator (name,location) VALUES ('ArcoCegoLisbon','Lisboa')").execute())
        .flatMap(r -> client.query("INSERT INTO GridCell (operator_id, grid_cell_id, location, max_load_mw) VALUES (1, 'LISBON-DT', 'Lisbon Downtown', 50.0)").execute())
        .await().indefinitely();
    }
    
    @GET
    public Multi<UtilityOperator> get() {
        return UtilityOperator.findAll(client);
    }
    
    @GET
    @Path("{id}")
    public Uni<Response> getSingle(Long id) {
        return UtilityOperator.findById(client, id)
                .onItem().transform(operator -> operator != null ? Response.ok(operator) : Response.status(Response.Status.NOT_FOUND)) 
                .onItem().transform(ResponseBuilder::build); 
    }
     
    @POST
    public Uni<Response> create(UtilityOperator operator) {
        return operator.save(client , operator.name , operator.location)
                .onItem().transform(id -> URI.create("/UtilityOperator/" + id))
                .onItem().transform(uri -> Response.created(uri).build());
    }
    
    @DELETE
    @Path("{id}")
    public Uni<Response> delete(Long id) {
        return UtilityOperator.delete(client, id)
                .onItem().transform(deleted -> deleted ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

    @PUT
    @Path("/{id}/{name}/{location}")
    public Uni<Response> update(Long id , String name , String location) {
        return UtilityOperator.update(client, id , name , location)
                .onItem().transform(updated -> updated ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

    // Criar uma nova GridCell para um Operator
@POST
@Path("{operatorId}/grid-cells")
public Uni<Response> createGridCell(@PathParam("operatorId") Long operatorId, GridCell gridCell) {
    gridCell.operator_id = operatorId;
    return gridCell.save(client, gridCell.operator_id, gridCell.grid_cell_id, gridCell.location, gridCell.max_load_mw)
            .onItem().transform(id -> URI.create("/UtilityOperator/" + operatorId + "/grid-cells/" + id))
            .onItem().transform(uri -> Response.created(uri).build());
}


@GET
@Path("{operatorId}/grid-cells")
public Multi<GridCell> getOperatorGridCells(@PathParam("operatorId") Long operatorId) {
    return GridCell.findByOperatorId(client, operatorId);
}


@PUT
@Path("grid-cells/{id}/{maxLoadMw}")
public Uni<Response> updateGridCellCapacity(@PathParam("id") Long id, @PathParam("maxLoadMw") Float maxLoadMw) {
    return GridCell.updateCapacity(client, id, maxLoadMw)
            .onItem().transform(updated -> updated ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
            .onItem().transform(status -> Response.status(status).build());
}


@DELETE
@Path("grid-cells/{id}")
public Uni<Response> deleteGridCell(@PathParam("id") Long id) {
    return GridCell.delete(client, id)
            .onItem().transform(deleted -> deleted ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
            .onItem().transform(status -> Response.status(status).build());
}
    
}

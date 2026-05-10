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

@Path("Prosumer")
public class ProsumerResource {

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
        client.query("DROP TABLE IF EXISTS Asset").execute()
        .flatMap(r -> client.query("DROP TABLE IF EXISTS Prosumer").execute())
        .flatMap(r -> client.query("CREATE TABLE Prosumer (id SERIAL PRIMARY KEY, name TEXT NOT NULL, FiscalNumber BIGINT UNSIGNED, location TEXT NOT NULL)").execute())
        .flatMap(r -> client.query("CREATE TABLE Asset (id SERIAL PRIMARY KEY, prosumer_id BIGINT UNSIGNED, asset_id TEXT NOT NULL, asset_type TEXT NOT NULL, max_capacity_kw FLOAT)").execute())
        .flatMap(r -> client.query("INSERT INTO Prosumer (name,FiscalNumber,location) VALUES ('client1','123456','Lisbon')").execute())
        .flatMap(r -> client.query("INSERT INTO Asset (prosumer_id, asset_id, asset_type, max_capacity_kw) VALUES (1, 'BATT-001', 'BATTERY', 10.5)").execute())
        .flatMap(r -> client.query("INSERT INTO Asset (prosumer_id, asset_id, asset_type, max_capacity_kw) VALUES (1, 'SOLAR-001', 'SOLAR', NULL)").execute())
        .await().indefinitely();
    }
    
    @GET
    public Multi<Prosumer> get() {
        return Prosumer.findAll(client);
    }
    
    @GET
    @Path("{id}")
    public Uni<Response> getSingle(Long id) {
        return Prosumer.findById(client, id)
                .onItem().transform(prosumer -> prosumer != null ? Response.ok(prosumer) : Response.status(Response.Status.NOT_FOUND)) 
                .onItem().transform(ResponseBuilder::build); 
    }
     
    @POST
    public Uni<Response> create(Prosumer prosumer) {
        return prosumer.save(client , prosumer.name , prosumer.FiscalNumber , prosumer.location)
                .onItem().transform(id -> URI.create("/Prosumer/" + id))
                .onItem().transform(uri -> Response.created(uri).build());
    }
    
    @DELETE
    @Path("{id}")
    public Uni<Response> delete(Long id) {
        return Prosumer.delete(client, id)
                .onItem().transform(deleted -> deleted ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

    @PUT
    @Path("/{id}/{name}/{FiscalNumber}/{location}")
    public Uni<Response> update(Long id , String name , Long FiscalNumber , String location) {
        return Prosumer.update(client, id , name , FiscalNumber , location)
                .onItem().transform(updated -> updated ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

    @POST
    @Path("{prosumerId}/assets")
    public Uni<Response> createAsset(@PathParam("prosumerId") Long prosumerId, Asset asset) {
        asset.prosumer_id = prosumerId;
        return asset.save(client, asset.prosumer_id, asset.asset_id, asset.asset_type, asset.max_capacity_kw)
                .onItem().transform(id -> URI.create("/Prosumer/" + prosumerId + "/assets/" + id))
                .onItem().transform(uri -> Response.created(uri).build());
    }

    @GET
    @Path("{prosumerId}/assets")
    public Multi<Asset> getProsumerAssets(@PathParam("prosumerId") Long prosumerId) {
        return Asset.findByProsumerId(client, prosumerId);
    }

    @DELETE
    @Path("{prosumerId}/assets/{assetId}")
    public Uni<Response> deleteAsset(@PathParam("prosumerId") Long prosumerId, @PathParam("assetId") Long assetId) {
        return Asset.delete(client, assetId)
                .onItem().transform(deleted -> deleted ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }  
}

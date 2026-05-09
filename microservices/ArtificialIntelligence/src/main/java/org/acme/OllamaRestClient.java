package org.acme;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.smallrye.mutiny.Uni;

@RegisterRestClient(configKey = "ollama-api")
@Path("/api")
public interface OllamaRestClient {

    @POST
    @Path("/generate")
    Uni<String> generate(String requestPayload);
}
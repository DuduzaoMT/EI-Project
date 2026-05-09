package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import io.smallrye.mutiny.Uni;
import org.json.JSONObject;

@Path("Forecasting")
public class ForecastingResource {

    @Inject
    @RestClient
    OllamaRestClient ollamaClient;

    @POST
    @Path("Analyze")
    public Uni<Response> analyzeLog(ForecastRequest request) {
        
        String prompt = "Analyze the following log text and determine if the 'Discharge' command resulted in a successful 'Grid Stable' state. " +
                        "Reply EXACTLY with 'GRID_STABLE' if successful, or 'FAILED' if it was not. " +
                        "Do not include any other text. Log: " + request.log_text;

        JSONObject ollamaReq = new JSONObject();
        ollamaReq.put("model", "llama3.2");
        ollamaReq.put("prompt", prompt);
        ollamaReq.put("stream", false);

        return ollamaClient.generate(ollamaReq.toString())
            .onItem().transform(ollamaResponseStr -> {
                
                JSONObject ollamaResponseJson = new JSONObject(ollamaResponseStr);
                String aiResponse = ollamaResponseJson.getString("response").trim();
                
                JSONObject finalResult = new JSONObject();
                finalResult.put("status", aiResponse);
                
                return Response.ok(finalResult.toString()).build();
            });
    }
}
package org.acme.e2e;

import io.camunda.zeebe.client.ZeebeClient;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseE2ETest {

    protected ZeebeClient zeebeClient;
    protected String apiGatewayUrl;

    @BeforeEach
    public void setupConfig() {
        String zeebeAddress = System.getProperty("zeebe.address", "localhost:26500");
        apiGatewayUrl = System.getProperty("api.gateway.url", "http://localhost:8000");

        zeebeClient = ZeebeClient.newClientBuilder()
                .gatewayAddress(zeebeAddress)
                .usePlaintext() 
                .build();

        RestAssured.baseURI = apiGatewayUrl;
    }

    @AfterEach
    public void tearDown() {
        if (zeebeClient != null) {
            zeebeClient.close();
        }
    }
}
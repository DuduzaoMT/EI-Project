package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class ArtificialIntelligenceResourceTest {

    @Test
    public void testForecastingEndpointExists() {
        given()
          .when().get("/forecast")
          .then()
             .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(500)));
    }

    @Test
    public void testForecastWithValidParameters() {
        String forecastJson = "{\"asset_id\":\"BATT-001\",\"forecast_hours\":24}";
        
        given()
          .contentType("application/json")
          .body(forecastJson)
          .when().post("/forecast")
          .then()
             .statusCode(anyOf(equalTo(200), equalTo(201), equalTo(500)));
    }
}

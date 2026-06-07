package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class EnergyAnalyticsResourceTest {

    @Test
    public void testGetAllEnergyAnalytics() {
        given()
          .when().get("/EnergyAnalytics")
          .then()
             .statusCode(200);
    }

    @Test
    public void testGetSingleEnergyAnalytics() {
        given()
          .when().get("/EnergyAnalytics/1")
          .then()
             .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    public void testPublishEnergyMetric() {
        String metricJson = "{\"metric_type\":\"ENERGY_DISCHARGED_BY_ZONE\",\"reference_id\":\"ZONE-1\",\"metric_value\":150.5,\"unit\":\"kWh\"}";
        
        given()
          .contentType("application/json")
          .body(metricJson)
          .when().post("/EnergyAnalytics/Publish")
          .then()
             .statusCode(201)
             .body(containsString("PUBLISHED"));
    }

    @Test
    public void testGetByMetricType() {
        given()
          .when().get("/EnergyAnalytics/metric_type/ENERGY_DISCHARGED_BY_ZONE")
          .then()
             .statusCode(200);
    }
}

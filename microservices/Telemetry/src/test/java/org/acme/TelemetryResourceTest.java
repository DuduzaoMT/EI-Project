package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class TelemetryResourceTest {

    @Test
    public void testGetAllTelemetry() {
        given()
          .when().get("/Telemetry")
          .then()
             .statusCode(200);
    }

    @Test
    public void testGetSingleTelemetry() {
        given()
          .when().get("/Telemetry/1")
          .then()
             .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    public void testGetTelemetryByAssetId() {
        given()
          .when().get("/Telemetry/asset/1")
          .then()
             .statusCode(200);
    }

    @Test
    public void testGetTelemetryByGridCellId() {
        given()
          .when().get("/Telemetry/grid/LISBON-DT")
          .then()
             .statusCode(200);
    }

    @Test
    public void testTelemetryEndpointExists() {
        given()
          .when().get("/Telemetry")
          .then()
             .statusCode(200);
    }
}

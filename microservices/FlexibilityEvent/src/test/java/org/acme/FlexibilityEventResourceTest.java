package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class FlexibilityEventResourceTest {

    @Test
    public void testGetAllFlexibilityEvents() {
        given()
          .when().get("/Flexibility")
          .then()
             .statusCode(200);
    }

    @Test
    public void testGetSingleFlexibilityEvent() {
        given()
          .when().get("/Flexibility/1")
          .then()
             .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    public void testGetByGridCell() {
        given()
          .when().get("/Flexibility/grid/LISBON-DT")
          .then()
             .statusCode(200);
    }

    @Test
    public void testGetByStatus() {
        given()
          .when().get("/Flexibility/status/PUBLISHED")
          .then()
             .statusCode(200);
    }

    @Test
    public void testEmitEvent() {

        String requestJson = """
        {
            "asset_id":"BATT-001",
            "prosumer_id":1,
            "grid_cell_id":"LISBON-DT",
            "logic_type":"ARBITRAGE",
            "proposed_action":"SELL",
            "incentive_value":10.5,
            "target_value_kw":5.0,
            "telemetry_reference_id":100
        }
        """;

        given()
            .contentType("application/json")
            .body(requestJson)
        .when()
            .post("/Flexibility/Emit")
        .then()
            .statusCode(201)
            .body("asset_id", equalTo("BATT-001"))
            .body("prosumer_id", equalTo(1))
            .body("logic_type", equalTo("ARBITRAGE"))
            .body("proposed_action", equalTo("SELL"))
            .body("status", equalTo("PUBLISHED"));
    }
}
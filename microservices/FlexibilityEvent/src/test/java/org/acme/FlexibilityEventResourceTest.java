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
    public void testEvaluateFlexibilityHighSOC() {
        String requestJson = "{\"assetId\":\"BATT-001\",\"prosumerId\":1,\"gridCellId\":\"LISBON-DT\",\"marketPrice\":\"HIGH\",\"socPercent\":92.5}";
        
        given()
          .contentType("application/json")
          .body(requestJson)
          .when().post("/Flexibility/Evaluate")
          .then()
             .statusCode(200)
             .body(containsString("ARBITRAGE"))
             .body(containsString("SELL"));
    }

    @Test
    public void testEvaluateFlexibilityLowSOC() {
        String requestJson = "{\"assetId\":\"BATT-002\",\"prosumerId\":1,\"gridCellId\":\"LISBON-DT\",\"marketPrice\":\"HIGH\",\"socPercent\":15.0}";
        
        given()
          .contentType("application/json")
          .body(requestJson)
          .when().post("/Flexibility/Evaluate")
          .then()
             .statusCode(200)
             .body(containsString("BALANCING"))
             .body(containsString("UNAVAILABLE"));
    }

    @Test
    public void testEvaluateFlexibilityOther() {
        String requestJson = "{\"assetId\":\"SOLAR-001\",\"prosumerId\":2,\"gridCellId\":\"PORTO-NW\",\"marketPrice\":\"NORMAL\",\"socPercent\":30.0}";
        
        given()
          .contentType("application/json")
          .body(requestJson)
          .when().post("/Flexibility/Evaluate")
          .then()
             .statusCode(200)
             .body(containsString("EVALUATED_NO_ACTION"));
    }
}

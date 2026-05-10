package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class UtilityOperatorResourceTest {

    @Test
    public void testGetAllUtilityOperators() {
        given()
          .when().get("/UtilityOperator")
          .then()
             .statusCode(200)
             .body(containsString("ArcoCegoLisbon"));
    }

    @Test
    public void testGetSingleUtilityOperator() {
        given()
          .when().get("/UtilityOperator/1")
          .then()
             .statusCode(200)
             .body(containsString("ArcoCegoLisbon"));
    }

    @Test
    public void testCreateUtilityOperator() {
        String operatorJson = "{\"name\":\"NewOperator\",\"location\":\"Covilhã\"}";
        
        given()
          .contentType("application/json")
          .body(operatorJson)
          .when().post("/UtilityOperator")
          .then()
             .statusCode(201)
             .header("Location", notNullValue());
    }

    @Test
    public void testGetOperatorGridCells() {
        given()
          .when().get("/UtilityOperator/1/grid-cells")
          .then()
             .statusCode(200)
             .body(containsString("LISBON-DT"));
    }

    @Test
    public void testUpdateGridCellCapacity() {
        given()
          .when().put("/UtilityOperator/grid-cells/1/75.0")
          .then()
             .statusCode(204);
    }
}

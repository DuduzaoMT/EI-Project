package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class GridBalancingResourceTest {

    @Test
    public void testGetAllGridBalancingEvents() {
        given()
          .when().get("/GridBalancing")
          .then()
             .statusCode(200);
    }

    @Test
    public void testGetSingleGridBalancingEvent() {
        given()
          .when().get("/GridBalancing/1")
          .then()
             .statusCode(anyOf(equalTo(200), equalTo(404)));
    }

    @Test
    public void testGetBySourceGridCell() {
        given()
          .when().get("/GridBalancing/source/LISBON-DT")
          .then()
             .statusCode(200);
    }

    @Test
    public void testGetByTargetGridCell() {
        given()
          .when().get("/GridBalancing/target/PORTO-NW")
          .then()
             .statusCode(200);
    }

    @Test
    public void testGetByStatus() {
        given()
          .when().get("/GridBalancing/status/PUBLISHED")
          .then()
             .statusCode(200);
    }

    @Test
    public void testEmitGridBalancingEvent() {
        String eventJson = "{\"source_grid_cell\":\"LISBON-DT\",\"target_grid_cell\":\"PORTO-NW\",\"recommended_action\":\"INCREASE_LOAD\",\"power_kw\":25.5}";
        
        given()
          .contentType("application/json")
          .body(eventJson)
          .when().post("/GridBalancing/Emit")
          .then()
             .statusCode(201)
             .body(containsString("PUBLISHED"));
    }
}

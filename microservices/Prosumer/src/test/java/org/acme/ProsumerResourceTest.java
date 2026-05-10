package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class ProsumerResourceTest {

    @Test
    public void testGetAllProsumers() {
        given()
          .when().get("/Prosumer")
          .then()
             .statusCode(200)
             .body(containsString("client1"));
    }

    @Test
    public void testGetSingleProsumer() {
        given()
          .when().get("/Prosumer/1")
          .then()
             .statusCode(200)
             .body(containsString("client1"));
    }

    @Test
    public void testCreateProsumer() {
        String prosumerJson = "{\"name\":\"TestProsumer\",\"FiscalNumber\":999999,\"location\":\"TestCity\"}";
        
        given()
          .contentType("application/json")
          .body(prosumerJson)
          .when().post("/Prosumer")
          .then()
             .statusCode(201)
             .header("Location", notNullValue());
    }

    @Test
    public void testGetProsumerAssets() {
        given()
          .when().get("/Prosumer/1/assets")
          .then()
             .statusCode(200)
             .body(containsString("BATTERY"));
    }
}

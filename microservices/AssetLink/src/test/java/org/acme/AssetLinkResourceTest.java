package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class AssetLinkResourceTest {

    @Test
    public void testGetAllAssetLinks() {
        given()
          .when().get("/AssetLink")
          .then()
             .statusCode(200)
             .body(containsString("idProsumer"));
    }

    @Test
    public void testGetSingleAssetLink() {
        given()
          .when().get("/AssetLink/1")
          .then()
             .statusCode(200);
    }

    @Test
    public void testGetAssetLinkByProsumerAndOperator() {
        given()
          .when().get("/AssetLink/1/1")
          .then()
             .statusCode(200)
             .body(containsString("idProsumer"));
    }

    @Test
    public void testCreateAssetLink() {
        String assetLinkJson = "{\"idProsumer\":5,\"idUtilityOperator\":2}";
        
        given()
          .contentType("application/json")
          .body(assetLinkJson)
          .when().post("/AssetLink")
          .then()
             .statusCode(201)
             .header("Location", notNullValue());
    }

    @Test
    public void testDeleteAssetLink() {
        given()
          .when().delete("/AssetLink/4")
          .then()
             .statusCode(204);
    }
}

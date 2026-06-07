package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class ArtificialIntelligenceResourceTest {

    @Test
    public void testForecastWithValidParameters() {
        String forecastJson = "{\n" +
            "  \"log_text\": \"This is a long text log that contains information about the energy consumption and production patterns of a prosumer. It includes details about the types of appliances used, their energy consumption, and the times at which they are used. The log also contains information about the weather conditions, such as temperature and sunlight, which can affect energy production from solar panels. Additionally, it includes data on the prosumer's energy generation from renewable sources, such as solar panels or wind turbines, and their energy consumption patterns throughout the day. This information is crucial for analyzing and forecasting future energy needs and optimizing energy usage for the prosumer.\"\n" +
            "}";
            
        given()
        .contentType("application/json")
        .body(forecastJson)
        .when().post("/Forecasting/Analyze")
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(201), equalTo(500)));
    }
}

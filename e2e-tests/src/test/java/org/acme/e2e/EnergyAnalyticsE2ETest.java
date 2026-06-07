package org.acme.e2e;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class EnergyAnalyticsE2ETest extends BaseE2ETest {

    @Test
    public void testEnergyAnalyticsFlowAutomated() {
        zeebeClient.newWorker()
                .jobType("io.camunda.zeebe:userTask")
                .handler((client, job) -> {
                    // "Select Zone for Energy Analytics"
                    if (job.getElementId().equals("UserTask_SelectZone_EA")) {
                        client.newCompleteCommand(job.getKey())
                                .variables(Map.of("gridCellId", "LISBON-DT"))
                                .send().join();
                    }
                })
                .name("EnergyAnalyticsAutomationWorker")
                .open();

        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId("EnergyAnalyticsProcess")
                .latestVersion()
                .send().join();

        // (automatic) fetches zone telemetry/prosumers/operator data and publishes the analysis
        given().baseUri(apiGatewayUrl)
               .when().get("/energy-analytics/EnergyAnalytics")
               .then().statusCode(200);
    }
}

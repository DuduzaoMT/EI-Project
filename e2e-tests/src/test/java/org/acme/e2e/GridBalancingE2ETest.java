package org.acme.e2e;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class GridBalancingE2ETest extends BaseE2ETest {

    @Test
    public void testGridBalancingFlowAutomated() {
        zeebeClient.newWorker()
                .jobType("io.camunda.zeebe:userTask")
                .handler((client, job) -> {
                    // "Select Grid Zone for Balancing"
                    if (job.getElementId().equals("UserTask_SelectZone_GB")) {
                        client.newCompleteCommand(job.getKey())
                                .variables(Map.of("gridCellId", "LISBON-DT"))
                                .send().join();
                    }
                })
                .name("GridBalancingAutomationWorker")
                .open();

        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId("GridBalancingProcess")
                .latestVersion()
                .send().join();

        // (automatic) fetches operator data, zone telemetry and prosumers, then emits a balancing recommendation
        given().baseUri(apiGatewayUrl)
               .when().get("/grid-balancing/GridBalancing/source/LISBON-DT")
               .then().statusCode(200);
    }
}

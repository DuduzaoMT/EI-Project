package org.acme.e2e;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class FlexibilityEmissionE2ETest extends BaseE2ETest {

    @Test
    public void testFlexibilityEmissionFlowAutomated() {
        zeebeClient.newWorker()
                .jobType("io.camunda.zeebe:userTask")
                .handler((client, job) -> {
                    // "Select Asset for Flexibility Emission"
                    if (job.getElementId().equals("UserTask_SelectAsset_FE")) {
                        client.newCompleteCommand(job.getKey())
                                .variables(Map.of(
                                        "assetId", "BATT-001",
                                        "gridCellId", "LISBON-DT"
                                ))
                                .send().join();
                    }
                })
                .name("FlexibilityEmissionAutomationWorker")
                .open();

        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId("FlexibilityEmissionProcess")
                .latestVersion()
                .send().join();

        // (automatic) fetches the asset's telemetry and emits the flexibility event
        given().baseUri(apiGatewayUrl)
               .when().get("/flexibility-event/Flexibility/grid/LISBON-DT")
               .then().statusCode(200);
    }
}

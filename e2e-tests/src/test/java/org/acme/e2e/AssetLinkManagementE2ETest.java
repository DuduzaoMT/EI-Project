package org.acme.e2e;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class AssetLinkManagementE2ETest extends BaseE2ETest {

    @Test
    public void testAssetLinkAssociationFlowAutomated() {
        // Reuse a Prosumer that already exists (e.g. the "João Silva" created by
        // ProsumerManagementE2ETest) — the AssetLink flow links an existing Prosumer
        // to an existing UtilityOperator, it does not create either of them.
        int prosumerId = given().baseUri(apiGatewayUrl)
                .when().get("/prosumer/Prosumer")
                .then().statusCode(200)
                .extract().jsonPath().getInt("[0].id");

        // Single worker that distinguishes the user tasks of the 3 collaborating
        // processes (ProsumerForAssetLink, AssetLinkManagement, TelemetryManagement) by element ID
        zeebeClient.newWorker()
                .jobType("io.camunda.zeebe:userTask")
                .handler((client, job) -> {
                    String elementId = job.getElementId();

                    // ProsumerForAssetLink: "Decide the data to AssetLink association order"
                    if (elementId.equals("Activity_9841ec7e-705e-4a46-9924-61aed9dc6c00")) {
                        client.newCompleteCommand(job.getKey())
                                .variables(Map.of(
                                        "UtilityOperatorID", "1",
                                        "prosumerID", String.valueOf(prosumerId)
                                ))
                                .send().join();
                    }
                    // AssetLinkManagement / TelemetryManagement: "Verify if execute product is possible"
                    else if (elementId.equals("Activity_9a1f746e-fa58-4118-9e43-d8ae475999d1")
                            || elementId.equals("Activity_8d2f041c-b6af-48f0-b2e1-e9783223c1e2")) {
                        client.newCompleteCommand(job.getKey())
                                .variables(Map.of("promise", true))
                                .send().join();
                    }
                    // ProsumerForAssetLink / AssetLinkManagement: "Check AssetLink association order" / "Check Telemetry Consumer Order"
                    else if (elementId.equals("Activity_fc1e6910-1008-4a65-96d6-d47bc7a5c702")
                            || elementId.equals("Activity_fdcd84ce-514c-4a3a-a44c-da80d1c96a60")) {
                        client.newCompleteCommand(job.getKey())
                                .variables(Map.of("accept", true))
                                .send().join();
                    }
                })
                .name("AssetLinkAutomationWorker")
                .open();

        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId("ProsumerForAssetLink")
                .latestVersion()
                .variables(Map.of("camundaBaseUrl", apiGatewayUrl))
                .send().join();

        // Final verification — the association should now exist
        given().baseUri(apiGatewayUrl)
               .when().get("/assetlink/AssetLink")
               .then().statusCode(200);
    }
}

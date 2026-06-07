package org.acme.e2e;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

public class AIForecastingE2ETest extends BaseE2ETest {

    @Test
    public void testAIForecastingFlowAutomated() {
        zeebeClient.newWorker()
                .jobType("io.camunda.zeebe:userTask")
                .handler((client, job) -> {
                    // "Select Zone for AI Flexibility Forecast"
                    if (job.getElementId().equals("UserTask_SelectZone_AI")) {
                        client.newCompleteCommand(job.getKey())
                                .variables(Map.of("gridCellId", "LISBON-DT"))
                                .send().join();
                    }
                })
                .name("AIForecastingAutomationWorker")
                .open();

        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId("AIForecastingProcess")
                .latestVersion()
                .send().join();

        // The flow calls Ollama for the AI analysis, so the emitted event may take a while to appear
        await().atMost(Duration.ofMinutes(3))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        given().baseUri(apiGatewayUrl)
                               .when().get("/flexibility-event/Flexibility/grid/LISBON-DT")
                               .then().statusCode(200));
    }
}

package org.acme.e2e;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;

public class UtilityOperatorManagementE2ETest extends BaseE2ETest {

    @Test
    public void testUtilityOperatorCreationFlowAutomated() {
        // Worker that distinguishes the user tasks of the linear sequence by their element ID
        zeebeClient.newWorker()
                .jobType("io.camunda.zeebe:userTask")
                .handler((client, job) -> {
                    String elementId = job.getElementId();

                    // "Request Utility Operator Creation" — fill in Name and Location
                    if (elementId.equals("Activity_8e3f9470-589c-4cc3-90b5-1fd584836c9b")) {
                        client.newCompleteCommand(job.getKey())
                                .variables(Map.of(
                                        "name", "OperadorTeste",
                                        "location", "Porto"
                                ))
                                .send().join();
                    }
                    // "Verify if Utility Operator Creation is possible" — promise checkbox
                    else if (elementId.equals("Activity_67317895-66a5-4c33-a4bc-0bebc6a11de6")) {
                        client.newCompleteCommand(job.getKey())
                                .variables(Map.of("promise", true))
                                .send().join();
                    }
                    // "Check Utility Operator Creation" — accept checkbox
                    else if (elementId.equals("Activity_11b6250a-0692-4e73-ba03-2cf01710e134")) {
                        client.newCompleteCommand(job.getKey())
                                .variables(Map.of("accept", true))
                                .send().join();
                    }
                    // "Promise/Declare/Accept Utility Operator Creation" are bare BPMN tasks
                    // (no implementation defined), so Zeebe completes them automatically.
                })
                .name("UtilityOperatorAutomationWorker")
                .open();

        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId("UtilityOperatorManagement")
                .latestVersion()
                .send().join();

        // Final verification — the new operator should now exist in the microservice
        given().baseUri(apiGatewayUrl)
               .when().get("/utility-operator/UtilityOperator")
               .then().statusCode(200)
               .body("name", hasItem("OperadorTeste"));
    }
}

package org.acme.e2e;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class ProsumerManagementE2ETest extends BaseE2ETest {

    @Test
    public void testProsumerCreationFlowAutomated() {
        // Worker especializado para distinguir os formulários pelo ID da Task
        zeebeClient.newWorker()
                .jobType("io.camunda.zeebe:userTask")
                .handler((client, job) -> {
                    String elementId = job.getElementId();
                    
                    // Identifica o ID da tarefa correspondente ao primeiro formulário (Request)
                    if (elementId.contains("Task_RequestProsumer")) {
                        client.newCompleteCommand(job.getKey())
                                .variables(Map.of(
                                        "name", "Empresa Exemplo Lda",
                                        "fiscalNumber", "500123456",
                                        "location", "Lisboa"
                                ))
                                .send().join();
                    } 
                    // Identifica o ID da tarefa correspondente ao segundo formulário (Accept/Decision)
                    else if (elementId.contains("Task_AcceptProsumer")) {
                        client.newCompleteCommand(job.getKey())
                                .variables(Map.of("isPossible", true))
                                .send().join();
                    }
                })
                .name("ProsumerAutomationWorker")
                .open();

        zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId("ProsumerMngInitiator")
                .latestVersion()
                .variables(Map.of("camundaBaseUrl", apiGatewayUrl))
                .send().join();

        // Verificação final
        given().baseUri(apiGatewayUrl)
               .when().get("/prosumer/Prosumer/500123456")
               .then().statusCode(200);
    }
}
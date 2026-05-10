# Test Documentation - Energy Integration

## Overview

**Framework:** JUnit 5 + REST Assured  
**Total Tests:** 38 test cases across 8 microservices  
**Test Command:** `./mvnw test`

---

## Test Summary by Microservice

| Microservice | Tests | Coverage |
|---|---|---|
| Prosumer | 4 | CRUD + asset management |
| AssetLink | 5 | Relationships |
| UtilityOperator | 5 | Operations + grid cells |
| GridBalancing | 6 | Events + Kafka |
| FlexibilityEvent | 7 | Evaluation logic |
| Telemetry | 5 | Data retrieval |
| EnergyAnalytics | 4 | Metrics + Kafka |
| ArtificialIntelligence | 2 | Service integration |

---

## How to Run Tests

**Single Microservice:**
```bash
cd microservices/Prosumer
./mvnw test
```

**All Microservices:**
```bash
cd microservices
for dir in Prosumer AssetLink UtilityOperator GridBalancing FlexibilityEvent Telemetry EnergyAnalytics ArtificialIntelligence; do
  cd "$dir" && ./mvnw test && cd ..
done
```

**Specific Test:**
```bash
./mvnw test -Dtest=ProsumerResourceTest#testGetAllProsumers
```

---

## Test Cases by Service

### Prosumer (4 tests)
- `testGetAllProsumers` - GET /Prosumer → 200
- `testGetSingleProsumer` - GET /Prosumer/1 → 200
- `testCreateProsumer` - POST /Prosumer → 201
- `testGetProsumerAssets` - GET /Prosumer/1/assets → 200

### AssetLink (5 tests)
- `testGetAllAssetLinks` - GET /AssetLink → 200
- `testGetSingleAssetLink` - GET /AssetLink/1 → 200
- `testGetAssetLinkByProsumerAndOperator` - GET /AssetLink/1/1 → 200
- `testCreateAssetLink` - POST /AssetLink → 201
- `testDeleteAssetLink` - DELETE /AssetLink/4 → 204

### UtilityOperator (5 tests)
- `testGetAllUtilityOperators` - GET /UtilityOperator → 200
- `testGetSingleUtilityOperator` - GET /UtilityOperator/1 → 200
- `testCreateUtilityOperator` - POST /UtilityOperator → 201
- `testGetOperatorGridCells` - GET /UtilityOperator/1/grid-cells → 200
- `testUpdateGridCellCapacity` - PUT /UtilityOperator/grid-cells/1/75.0 → 204

### GridBalancing (6 tests)
- `testGetAllGridBalancingEvents` - GET /GridBalancing → 200
- `testGetSingleGridBalancingEvent` - GET /GridBalancing/1 → 200/404
- `testGetBySourceGridCell` - GET /GridBalancing/source/LISBON-DT → 200
- `testGetByTargetGridCell` - GET /GridBalancing/target/PORTO-NW → 200
- `testGetByStatus` - GET /GridBalancing/status/PUBLISHED → 200
- `testEmitGridBalancingEvent` - POST /GridBalancing/Emit → 201 + Kafka

### FlexibilityEvent (7 tests)
- `testGetAllFlexibilityEvents` - GET /Flexibility → 200
- `testGetSingleFlexibilityEvent` - GET /Flexibility/1 → 200/404
- `testGetByGridCell` - GET /Flexibility/grid/LISBON-DT → 200
- `testGetByStatus` - GET /Flexibility/status/PUBLISHED → 200
- `testEvaluateFlexibilityHighSOC` - POST /Flexibility/Evaluate (SOC 92.5%) → 200 + Kafka
- `testEvaluateFlexibilityLowSOC` - POST /Flexibility/Evaluate (SOC 15%) → 200 + Kafka
- `testEvaluateFlexibilityOther` - POST /Flexibility/Evaluate (no emit) → 200

### Telemetry (5 tests)
- `testGetAllTelemetry` - GET /Telemetry → 200
- `testGetSingleTelemetry` - GET /Telemetry/1 → 200/404
- `testGetTelemetryByAssetId` - GET /Telemetry/asset/1 → 200
- `testGetTelemetryByGridCellId` - GET /Telemetry/grid/LISBON-DT → 200
- `testTelemetryEndpointExists` - GET /Telemetry → 200

### EnergyAnalytics (4 tests)
- `testGetAllEnergyAnalytics` - GET /EnergyAnalytics → 200
- `testGetSingleEnergyAnalytics` - GET /EnergyAnalytics/1 → 200/404
- `testPublishEnergyMetric` - POST /EnergyAnalytics/Publish → 201 + Kafka
- `testGetByMetricType` - GET /EnergyAnalytics/metric/... → 200

### ArtificialIntelligence (2 tests)
- `testForecastingEndpointExists` - GET /forecast → 200/400/500
- `testForecastWithValidParameters` - POST /forecast → 200/201/500

---

## Prerequisites

- **MySQL:** Running on localhost:3306
- **Kafka:** Bootstrap servers configured (topics auto-created)
- **Ollama:** Optional for AI tests

---

## Test Framework

```java
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
}
```

---

## Troubleshooting

| Issue | Solution |
|---|---|
| 404 Not Found | Start microservice: `./mvnw quarkus:dev` |
| Database errors | Check MySQL running, verify credentials |
| Kafka tests fail | Start Kafka broker |
| AI tests fail | Start Ollama service |

---

**Updated:** May 10, 2026  
**Status:** Ready for testing

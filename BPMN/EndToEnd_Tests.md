# End-to-End Tests — BPMN Flows 

> Name aligned with the assignment's terminology: *"documentation of the end-to-end tests of the complete execution of the business processes"*.

## Infrastructure addresses — placeholders

The public addresses change every time the infrastructure is redeployed (each
`terraform apply` creates new EC2 instances with new DNS names). Because of
this, the commands below use **placeholders** — replace them with the current
values before running the tests:

| Placeholder | What it is | Where to get the value |
|---|---|---|
| `<CAMUNDA_DNS>` | Public DNS of the Camunda EC2 instance (Operate/Tasklist/REST API on port 8080) | `terraform output` inside `Camunda-Terraform/` (output `address`); or AWS EC2 console → instance `terraform-example-Camunda` → "Public IPv4 DNS" field; also printed at the end of `DeploymentAutomation-ubuntu.sh` / `DeploymentAutomation-macOS.sh` |
| `<KONG_DNS>` | Public DNS of the Kong EC2 instance (proxy on port 8000, admin API on port 8001) | `terraform output` inside `KongTerraform/` (output `address`); or AWS EC2 console → instance `terraform-example-Kong` → "Public IPv4 DNS"; also printed at the end of the deployment script |

With the current values, the endpoints become:
- **Camunda**: `http://<CAMUNDA_DNS>:8080`
  - Operate: `http://<CAMUNDA_DNS>:8080/operate`
  - Login: `demo` / `demo`
- **Kong Gateway**: `http://<KONG_DNS>:8000`

> Note: if you redeploy, update the table above with the new DNS values first
> — every command in this guide refers to `<CAMUNDA_DNS>` and `<KONG_DNS>` so
> that they remain valid after a redeploy.

## How to start any process
Every process with a "none start event" can be started with:
```bash
curl -X POST "http://<CAMUNDA_DNS>:8080/v2/process-instances" \
  -H "Content-Type: application/json" \
  -d '{"processDefinitionId": "<PROCESS_ID>", "variables": { ... }}'
```
Then everything can be followed in **Operate** (*Processes* tab → pick the instance → complete the highlighted *User Tasks* as they appear).

---

# 1. ProsumerManagement ✅ (already tested successfully)

**Process to start**: `ProsumerMngInitiator`

```bash
curl -X POST "http://<CAMUNDA_DNS>:8080/v2/process-instances" \
  -H "Content-Type: application/json" \
  -d '{"processDefinitionId": "ProsumerMngInitiator", "variables": {"camundaBaseUrl": "http://<CAMUNDA_DNS>:8080"}}'
```

### Steps in Operate
1. **"Decide the Data for Prosumer Creation order"** — fill in `Name`, `Fiscal Number`, `Location` (e.g. `João Silva`, `123456789`, `Lisboa`)
2. *(automatic)* "Request Prosumer Creation order" → starts the Executor process
3. **"Verify if execute is possible"** (Executor side) — tick the "Is it possible to promise the request?" checkbox as **yes** and complete
4. *(automatic)* "Promise Prosumer Creation order" → "Create Prosumer" → "Declare Prosumer Creation order" → "Accept Prosumer Creation order"
5. Process ends (end event)

### Final verification
```bash
curl http://<KONG_DNS>:8000/prosumer/Prosumer
```
You should see a new record with the name/location you entered.

### Bugs fixed in the file (already applied)
- `camundaBaseUrl` was not being passed from the Initiator to the Executor → now included in the body of "Request Prosumer Creation order"
- The "Create Prosumer" URL was broken (`http://:8000/...`, missing the Kong address) → fixed
- "Create Prosumer" was sending `fiscalNumber` (camelCase) instead of `fiscalnumber` (the actual variable) → fixed, the fiscal number now reaches the DB

---

# Group 1 — Simple flows (single process, no messages between participants)

These don't use `camundaBaseUrl` or collaboration between processes — they only had the missing-Kong-address bug in the service tasks, **already fixed in all of them**.

## 2. UtilityOperatorManagement

**Process**: `UtilityOperatorManagement` (no variables required to start)

```bash
curl -X POST "http://<CAMUNDA_DNS>:8080/v2/process-instances" \
  -H "Content-Type: application/json" \
  -d '{"processDefinitionId": "UtilityOperatorManagement"}'
```

### Steps in Operate (linear sequence, all `assignee=demo`)
1. **"Request Utility Operator Creation"** — fill in `Name` and `Location` (e.g. `OperadorTeste`, `Porto`)
2. **"Verify if Utility Operator Creation is possible"** — "Is it possible to promise the request?" checkbox → tick **yes**
3. **"Promise Utility Operator Creation"** — same checkbox → tick **yes**
4. *(automatic)* "Create Utility Operator" (real REST call to the microservice)
5. **"Declare Utility Operator Creation"** — checkbox → tick **yes**
6. **"Check Utility Operator Creation"** — "Is the creation of Utility Operator acceptable?" checkbox → tick **yes**
7. **"Accept Utility Operator Creation"** — same checkbox → tick **yes**
8. End

### Verification
```bash
curl http://<KONG_DNS>:8000/utility-operator/UtilityOperator
```

---

## 3. AIForecasting

**Process**: `AIForecastingProcess`

```bash
curl -X POST "http://<CAMUNDA_DNS>:8080/v2/process-instances" \
  -H "Content-Type: application/json" \
  -d '{"processDefinitionId": "AIForecastingProcess"}'
```

### Steps in Operate
1. **"Select Zone for AI Flexibility Forecast"** — fill `Grid Cell ID` with **`LISBON-DT`** (the grid cell that exists in the seed data)
2. *(automatic)* "Get Flexibility Event History" → "AI Analysis via Ollama (ArtificialIntelligence MS)" → "Emit AI Flexibility Event (AI_DISCHARGE)"
3. End

> This is a real call to an AI model (Ollama) — it may take longer than the other flows.

---

## 4. EnergyAnalytics

**Process**: `EnergyAnalyticsProcess`

```bash
curl -X POST "http://<CAMUNDA_DNS>:8080/v2/process-instances" \
  -H "Content-Type: application/json" \
  -d '{"processDefinitionId": "EnergyAnalyticsProcess"}'
```

### Steps in Operate
1. **"Select Zone for Energy Analytics"** — `Grid Cell ID` = **`LISBON-DT`**
2. *(automatic)* fetches zone telemetry, zone prosumers, operator data, and publishes the energy analysis
3. End

---

## 5. FlexibilityEmission

**Process**: `FlexibilityEmissionProcess`

```bash
curl -X POST "http://<CAMUNDA_DNS>:8080/v2/process-instances" \
  -H "Content-Type: application/json" \
  -d '{"processDefinitionId": "FlexibilityEmissionProcess"}'
```

### Steps in Operate
1. **"Select Asset for Flexibility Emission"** — `Asset ID` = **`BATT-001`** (or `SOLAR-001`), `Grid Cell ID` = **`LISBON-DT`**
2. *(automatic)* fetches the asset's telemetry → emits the flexibility event
3. End

---

## 6. GridBalancing

**Process**: `GridBalancingProcess`

```bash
curl -X POST "http://<CAMUNDA_DNS>:8080/v2/process-instances" \
  -H "Content-Type: application/json" \
  -d '{"processDefinitionId": "GridBalancingProcess"}'
```

### Steps in Operate
1. **"Select Grid Zone for Balancing"** — `Grid Cell ID` = **`LISBON-DT`**
2. *(automatic)* fetches operator data, zone telemetry, zone prosumers → emits a balancing recommendation
3. End

---

# Group 2 — AssetLinkManagement (multi-participant, same complexity as Prosumer)

It has **3 collaborating processes**: `ProsumerForAssetLink` (Initiator) → `AssetLinkManagement` (Executor) → `TelemetryManagement`, linked through message exchange — exactly like Prosumer. The same kind of fix was applied (`camundaBaseUrl` included in the messages that start the other processes + Kong addresses fixed in the 6 service tasks that were broken).

**Process to start**: `ProsumerForAssetLink`

```bash
curl -X POST "http://<CAMUNDA_DNS>:8080/v2/process-instances" \
  -H "Content-Type: application/json" \
  -d '{"processDefinitionId": "ProsumerForAssetLink", "variables": {"camundaBaseUrl": "http://<CAMUNDA_DNS>:8080"}}'
```

### Steps in Operate
> Heads up: **3 different process instances** will appear in Operate (one per participant) — you'll need to switch between them as the tasks "wake up".

1. **"Decide the data to AssetLink association order"** (in `ProsumerForAssetLink`) — fill in:
   - `UtilityOperatorID to choose?` → **`1`** (operator seed "ArcoCegoLisbon")
   - `ProsumerID to choose?` → **`1`** or **`2`** (use the "João Silva" we created in the previous test!)
2. *(automatic)* "Request AssetLink association order" → publishes a message and starts the `AssetLinkManagement` instance
3. Switch to the **`AssetLinkManagement`** instance: complete **"Verify if execute product is possible"** — "Is it possible to promise the request?" checkbox → tick **yes**
4. The flow continues and eventually starts the **`TelemetryManagement`** instance
5. Switch to that instance: complete **"Verify if execute product is possible"** (same checkbox) → tick **yes**
6. As the confirmation messages come back, acceptance tasks will appear in `ProsumerForAssetLink`:
   - **"Check Telemetry Consumer Order"** and/or **"Check AssetLink association order"** — "Is the creation acceptable?" checkbox → tick **yes**
7. All 3 instances end (end events)

### Verification
```bash
# Confirm the association was created
curl http://<KONG_DNS>:8000/assetlink/AssetLink
```

---

# Summary of systemic bugs fixed in this session

1. **Missing Kong address** (`http://:8000/...` instead of `http://<KONG_DNS>:8000/...`) — affected **20 service tasks across 6 files** (`AIForecasting`, `AssetLinkManagement`, `EnergyAnalytics`, `UtilityOperatorManagement`, `GridBalancing`, `FlexibilityEmission`, plus `ProsumerManagement` which had already been fixed). Fixed via a global replacement with the current Kong address.
2. **`camundaBaseUrl` not propagated between collaborating processes** — fixed in `ProsumerManagement` (Initiator→Executor) and in `AssetLinkManagement` (`ProsumerForAssetLink`→`AssetLinkManagement`→`TelemetryManagement`), by adding the variable to the bodies of the calls that start the other processes.
3. **Wrong variable name** (`fiscalNumber` vs `fiscalnumber`) when creating the Prosumer — fixed.

> **IMPORTANT**: after any change to the `.bpmn` files, you need to **redeploy** before testing (replace `<CAMUNDA_DNS>` and `<FILE_NAME>` with the current values):
> ```bash
> curl -L -X POST "http://<CAMUNDA_DNS>:8080/v2/deployments" \
>   -H "Content-Type: multipart/form-data" -H "Accept: application/json" \
>   -F "resources=@./BPMN/<FILE_NAME>.bpmn"
> ```

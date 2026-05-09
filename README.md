# EI-Project

## Getting Started

1. Update `access.sh` with your Java installation path, AWS credentials, and Docker configuration.

2. **Before Deployment:**

   In the Terraform files for the microservices, update the AMI (Amazon Machine Image) according to the processor architecture (x86 or ARM) of the host that builds the Docker images. This ensures compatibility between the Docker images and the EC2 instances.

   **Example:**

   ```hcl
   # Use the correct AMI for your architecture
   ami = "ami-0eb38b817b93460ac" # x86: ami-0eb38b817b93460ac, arm: ami-0bb7267a511c0a8e8
   instance_type = "t3.small"     # x86: t3.small, arm: t4g.small
   ```

   **Files to update:**
   - Quarkus-Terraform/assetlink/EC2InstallQuarkus.tf
   - Quarkus-Terraform/prosumer/EC2InstallQuarkus.tf
   - Quarkus-Terraform/telemetry/EC2InstallQuarkus.tf
   - Quarkus-Terraform/utilityoperator/EC2InstallQuarkus.tf
   - Quarkus-Terraform/flexibilityEvent/EC2InstallQuarkus.tf
   - Quarkus-Terraform/gridBalancing/EC2InstallQuarkus.tf
   - Quarkus-Terraform/energyAnalytics/EC2InstallQuarkus.tf
   - Quarkus-Terraform/artificialIntelligence/EC2OllamaConfiguration.tf

3. **Kafka Setup:**
   - Wait for the deployment to finish.
   - SSH into the Kafka machine.
   - Navigate to the Kafka directory:
     ```sh
     cd /path/to/kafka
     ```
   - Create the topic (Only if you want to test telemetry):

     ```sh
     bin/kafka-topics.sh --create --topic 560987123-EDP --bootstrap-server localhost:9092
     ```

   - On your local machine, go to the producer directory and run:
     ```sh
     java -jar VPPaaSSimulator.jar --broker-list <broker-dns>:9092
     ```

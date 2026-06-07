#!/bin/bash

# Public DNS - Camunda
cd Camunda-Terraform
esc=$'\e'
addressCamunda="$(terraform state show aws_instance.exampleInstallCamundaEngine |grep public_dns| sed "s/public_dns//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"
cd ..

# Public DNS - Kong Gateway
cd KongTerraform
addressKong="$(terraform state show aws_instance.exampleInstallKong |grep public_dns | sed "s/public_dns//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"
cd ..

echo "Camunda IP: $addressCamunda"
echo "Kong IP: $addressKong"
echo "Starting End-to-End Tests..."

cd e2e-tests
mvn test -Dzeebe.address="$addressCamunda:26500" -Dapi.gateway.url="http://$addressKong:8000"
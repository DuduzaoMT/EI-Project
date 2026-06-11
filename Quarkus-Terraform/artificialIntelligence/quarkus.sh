#!/bin/bash
echo "Starting..."

cd
sudo yum update -y

sudo curl -fsSL https://ollama.com/install.sh | sh

export HOME=$HOME:/usr/local/bin

sudo sed -i "s/\[Install\]/Environment=\"OLLAMA_HOST=0.0.0.0:11434\"\n\[Install\]/g" /etc/systemd/system/ollama.service

sudo systemctl enable ollama
sudo systemctl start ollama

ollama pull llama3.2:latest

echo "Ollama Finished."

sudo yum install -y docker
sudo service docker start


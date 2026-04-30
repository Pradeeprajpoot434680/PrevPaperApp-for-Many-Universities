#!/bin/bash

echo "=============================="
echo "Starting Kafka (KRaft mode)..."
echo "=============================="

cd kafka_2.13-4.1.1 || exit

# Generate Cluster ID (only needed first time, but safe to keep)
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"

bin/kafka-storage.sh format --standalone -t $KAFKA_CLUSTER_ID -c config/server.properties

gnome-terminal -- bash -c "bin/kafka-server-start.sh config/server.properties; exec bash"

sleep 10

echo "Kafka started ✅"


echo "=============================="
echo "Starting Service Registry..."
echo "=============================="

gnome-terminal -- bash -c "cd ~/Desktop/coding/web_2_projects/Software-Engineering/PrevPaperApp/service-registry && mvn spring-boot:run; exec bash"

sleep 15

echo "Service Registry started ✅"


echo "=============================="
echo "Starting Microservices..."
echo "=============================="

BASE_DIR=~/Desktop/coding/web_2_projects/Software-Engineering/PrevPaperApp

services=(
"auth-service"
"user-service"
"content-service"
"university-service"
"upload-service"
"notificationService"
)

for service in "${services[@]}"
do
    echo "Starting $service..."
    gnome-terminal -- bash -c "cd $BASE_DIR/$service && mvn spring-boot:run; exec bash"
    sleep 5
done

echo "Core services started ✅"


echo "=============================="
echo "Starting API Gateway..."
echo "=============================="

gnome-terminal -- bash -c "cd $BASE_DIR/api-gateway && mvn spring-boot:run; exec bash"

echo "🎉 ALL SERVICES STARTED SUCCESSFULLY"

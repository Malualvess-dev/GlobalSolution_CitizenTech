#!/bin/bash
set -e

RG="rg-paradacerta-gs"
LOCATION="canadacentral"
SUFFIX=$RANDOM
SQL_SERVER="sqlserver-paradacerta-$SUFFIX"
SQL_DB="sqldb-paradacerta"
SQL_ADMIN="adminparadacerta"
SQL_PASS="ParadaCerta@2026!"
ACR_NAME="acrparadacerta$SUFFIX"
PLAN_NAME="plan-paradacerta"
WEBAPP_NAME="api-paradacerta-$SUFFIX"
ACI_RABBIT="aci-rabbitmq-paradacerta"
RABBIT_DNS="rabbitmq-paradacerta-$SUFFIX"
RABBIT_USER="paradacerta"
RABBIT_PASS="Rabbit@2026!"

az group create --name $RG --location $LOCATION

az sql server create \
  --name $SQL_SERVER \
  --resource-group $RG \
  --location $LOCATION \
  --admin-user $SQL_ADMIN \
  --admin-password $SQL_PASS

az sql server firewall-rule create \
  --resource-group $RG \
  --server $SQL_SERVER \
  --name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0

MEU_IP=$(curl -s ifconfig.me)
az sql server firewall-rule create \
  --resource-group $RG \
  --server $SQL_SERVER \
  --name AllowMeuIP \
  --start-ip-address $MEU_IP \
  --end-ip-address $MEU_IP

az sql db create \
  --resource-group $RG \
  --server $SQL_SERVER \
  --name $SQL_DB \
  --service-objective Basic

az acr create \
  --resource-group $RG \
  --name $ACR_NAME \
  --sku Basic \
  --admin-enabled true

az container create \
  --resource-group $RG \
  --name $ACI_RABBIT \
  --image rabbitmq:3-management \
  --os-type Linux \
  --dns-name-label $RABBIT_DNS \
  --ports 5672 15672 \
  --cpu 1 \
  --memory 1.5 \
  --environment-variables RABBITMQ_DEFAULT_USER=$RABBIT_USER RABBITMQ_DEFAULT_PASS=$RABBIT_PASS

az appservice plan create \
  --name $PLAN_NAME \
  --resource-group $RG \
  --sku B1 \
  --is-linux

az webapp create \
  --resource-group $RG \
  --plan $PLAN_NAME \
  --name $WEBAPP_NAME \
  --deployment-container-image-name mcr.microsoft.com/appsvc/staticsite:latest

echo "=== ANOTE ESTES DADOS ==="
echo "DB_URL: jdbc:sqlserver://$SQL_SERVER.database.windows.net:1433;database=$SQL_DB;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;"
echo "RABBITMQ_HOST: $RABBIT_DNS.$LOCATION.azurecontainer.io"
echo "ACR_LOGIN_SERVER: $ACR_NAME.azurecr.io"
echo "WEBAPP_NAME: $WEBAPP_NAME"
echo "WEBAPP_URL: https://$WEBAPP_NAME.azurewebsites.net"
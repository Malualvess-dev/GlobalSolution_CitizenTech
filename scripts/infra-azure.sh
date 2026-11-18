#!/usr/bin/env bash
# =====================================================================
# PARADA CERTA | Global Solution 2026/2 | DevOps Tools & Cloud Computing
# Provisiona no Azure, via Azure CLI, os recursos usados pelo Build e pela Release:
#
#   1. Resource Group
#   2. Azure Container Registry (ACR) com a imagem do RabbitMQ (dockerfiles/Dockerfile.rabbitmq)
#   3. Azure Container Instance (ACI) rodando o RabbitMQ (mensageria, opcao A do Java)
#   4. App Service Plan Linux + Web App Java 21 (a API)
#   5. Configuracoes (variaveis de ambiente) da Web App
#
# Banco de dados: Oracle da FIAP (servico PaaS). As tabelas sao criadas com scripts/script-bd.sql.
#
# Uso (Git Bash, Linux, macOS ou Azure Cloud Shell), na raiz do repositorio:
#   az login
#   cp scripts/azure.env.example scripts/azure.env   # e preencha os valores
#   bash scripts/infra-azure.sh
#
# Os segredos ficam em scripts/azure.env, que NAO vai para o Git.
# O script pode ser executado de novo: recursos que ja existem sao atualizados.
# =====================================================================
set -euo pipefail

# Git Bash no Windows: nao converter argumentos como /actuator/health em caminhos do Windows
export MSYS_NO_PATHCONV=1

DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAIZ="$(cd "$DIR_SCRIPT/.." && pwd)"
cd "$RAIZ"

if [[ -f "$DIR_SCRIPT/azure.env" ]]; then
	set -a
	# shellcheck disable=SC1091
	source "$DIR_SCRIPT/azure.env"
	set +a
fi

# ---------------------------------------------------------------------
# Nomes dos recursos (podem ser trocados no azure.env)
# ---------------------------------------------------------------------
SUFIXO="${SUFIXO:-paradacerta}"
LOCALIZACAO="${LOCALIZACAO:-brazilsouth}"
RESOURCE_GROUP="${RESOURCE_GROUP:-rg-paradacerta}"
ACR_NOME="${ACR_NOME:-acr${SUFIXO//-/}}"
RABBIT_ACI="${RABBIT_ACI:-aci-rabbitmq-$SUFIXO}"
RABBIT_DNS="${RABBIT_DNS:-rabbitmq-$SUFIXO}"
PLANO="${PLANO:-plan-$SUFIXO}"
PLANO_SKU="${PLANO_SKU:-B1}"
WEBAPP="${WEBAPP:-api-$SUFIXO}"

# ---------------------------------------------------------------------
# Variaveis obrigatorias (segredos)
# ---------------------------------------------------------------------
obrigatorias=(DB_USERNAME DB_PASSWORD JWT_SECRET HASH_SECRET RABBITMQ_USERNAME RABBITMQ_PASSWORD)
faltando=()
for v in "${obrigatorias[@]}"; do
	[[ -z "${!v:-}" ]] && faltando+=("$v")
done
if (( ${#faltando[@]} )); then
	echo "ERRO: defina em scripts/azure.env: ${faltando[*]}" >&2
	exit 1
fi
if (( ${#JWT_SECRET} < 32 )); then
	echo "ERRO: JWT_SECRET precisa ter pelo menos 32 caracteres" >&2
	exit 1
fi

DB_URL="${DB_URL:-jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL}"

passo() { echo; echo "==> $*"; }

passo "Assinatura: $(az account show --query name -o tsv)"

# ---------------------------------------------------------------------
# 1) Resource Group
# ---------------------------------------------------------------------
passo "1/5 Resource Group $RESOURCE_GROUP ($LOCALIZACAO)"
az group create --name "$RESOURCE_GROUP" --location "$LOCALIZACAO" \
	--tags projeto=parada-certa disciplina=devops -o none

# Provedores usados (a primeira vez pode levar alguns minutos)
for provedor in Microsoft.ContainerRegistry Microsoft.ContainerInstance Microsoft.Web; do
	if [[ "$(az provider show -n "$provedor" --query registrationState -o tsv)" != "Registered" ]]; then
		echo "Registrando o provedor $provedor..."
		az provider register --namespace "$provedor" --wait
	fi
done

# ---------------------------------------------------------------------
# 2) Azure Container Registry + imagem do RabbitMQ
#    O build roda no proprio ACR (az acr build): nao precisa de Docker na maquina.
# ---------------------------------------------------------------------
passo "2/5 Container Registry $ACR_NOME e imagem do RabbitMQ"
if ! az acr show -n "$ACR_NOME" -g "$RESOURCE_GROUP" -o none 2>/dev/null; then
	az acr create -n "$ACR_NOME" -g "$RESOURCE_GROUP" --sku Basic --admin-enabled true -o none
fi
# Assinaturas de estudante podem bloquear o ACR Tasks (TasksOperationsNotAllowed). Nesse caso a
# imagem oficial usada como base no Dockerfile e importada direto para o ACR (az acr import).
if ! az acr build -r "$ACR_NOME" -t citizentech-rabbitmq:4.1 \
	-f dockerfiles/Dockerfile.rabbitmq dockerfiles --no-logs -o none 2>/dev/null; then
	echo "ACR Tasks indisponivel nesta assinatura: importando rabbitmq:4.1-management para o ACR"
	az acr import -n "$ACR_NOME" --source docker.io/library/rabbitmq:4.1-management \
		-t citizentech-rabbitmq:4.1 --force -o none
fi

ACR_SERVIDOR="$(az acr show -n "$ACR_NOME" --query loginServer -o tsv)"
ACR_USUARIO="$(az acr credential show -n "$ACR_NOME" --query username -o tsv)"
ACR_SENHA="$(az acr credential show -n "$ACR_NOME" --query 'passwords[0].value' -o tsv)"

# ---------------------------------------------------------------------
# 3) RabbitMQ no Azure Container Instance
# ---------------------------------------------------------------------
passo "3/5 RabbitMQ no Container Instance $RABBIT_ACI"
az container create -g "$RESOURCE_GROUP" -n "$RABBIT_ACI" \
	--image "$ACR_SERVIDOR/citizentech-rabbitmq:4.1" \
	--registry-login-server "$ACR_SERVIDOR" \
	--registry-username "$ACR_USUARIO" --registry-password "$ACR_SENHA" \
	--os-type Linux --cpu 1 --memory 1.5 \
	--ports 5672 15672 --ip-address Public --dns-name-label "$RABBIT_DNS" \
	--environment-variables RABBITMQ_DEFAULT_USER="$RABBITMQ_USERNAME" \
	--secure-environment-variables RABBITMQ_DEFAULT_PASS="$RABBITMQ_PASSWORD" \
	--restart-policy Always -o none

RABBIT_HOST="$(az container show -g "$RESOURCE_GROUP" -n "$RABBIT_ACI" --query ipAddress.fqdn -o tsv)"
echo "RabbitMQ: $RABBIT_HOST:5672 (painel em http://$RABBIT_HOST:15672)"

# ---------------------------------------------------------------------
# 4) App Service Plan + Web App Java 21
# ---------------------------------------------------------------------
passo "4/5 App Service $WEBAPP (plano $PLANO, $PLANO_SKU)"
az appservice plan create -n "$PLANO" -g "$RESOURCE_GROUP" --is-linux --sku "$PLANO_SKU" -o none
if ! az webapp show -n "$WEBAPP" -g "$RESOURCE_GROUP" -o none 2>/dev/null; then
	az webapp create -n "$WEBAPP" -g "$RESOURCE_GROUP" -p "$PLANO" --runtime "JAVA:21-java21" -o none
fi
az webapp config set -n "$WEBAPP" -g "$RESOURCE_GROUP" \
	--always-on true --http20-enabled true --ftps-state Disabled -o none
az webapp update -n "$WEBAPP" -g "$RESOURCE_GROUP" --https-only true -o none
az webapp update -n "$WEBAPP" -g "$RESOURCE_GROUP" --set siteConfig.healthCheckPath=/actuator/health -o none

# ---------------------------------------------------------------------
# 5) Variaveis de ambiente da API (App Settings)
# ---------------------------------------------------------------------
passo "5/5 Variaveis de ambiente da Web App"
az webapp config appsettings set -n "$WEBAPP" -g "$RESOURCE_GROUP" -o none --settings \
	PORT=80 \
	DB_URL="$DB_URL" \
	DB_USERNAME="$DB_USERNAME" \
	DB_PASSWORD="$DB_PASSWORD" \
	DDL_AUTO="${DDL_AUTO:-validate}" \
	RABBITMQ_HOST="$RABBIT_HOST" \
	RABBITMQ_PORT=5672 \
	RABBITMQ_USERNAME="$RABBITMQ_USERNAME" \
	RABBITMQ_PASSWORD="$RABBITMQ_PASSWORD" \
	MENSAGERIA_MODO=rabbit \
	OPENAI_API_KEY="${OPENAI_API_KEY:-sem-chave}" \
	OPENAI_MODEL="${OPENAI_MODEL:-gpt-4o-mini}" \
	JWT_SECRET="$JWT_SECRET" \
	JWT_EXPIRATION_MINUTES="${JWT_EXPIRATION_MINUTES:-120}" \
	HASH_SECRET="$HASH_SECRET" \
	GESTOR_NOME="${GESTOR_NOME:-Gestor Parada Certa}" \
	GESTOR_EMAIL="${GESTOR_EMAIL:-}" \
	GESTOR_SENHA="${GESTOR_SENHA:-}" \
	CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-*}"

URL="https://$(az webapp show -n "$WEBAPP" -g "$RESOURCE_GROUP" --query defaultHostName -o tsv)"

echo
echo "====================================================================="
echo " Recursos prontos no grupo $RESOURCE_GROUP"
echo "   API (Web App) : $URL"
echo "   Swagger       : $URL/swagger-ui.html"
echo "   RabbitMQ      : http://$RABBIT_HOST:15672"
echo
echo " Proximo passo: rodar a pipeline (azure-pipeline.yml) para publicar o .jar."
echo " Use na pipeline: webAppName=$WEBAPP e resourceGroup=$RESOURCE_GROUP"
echo "====================================================================="

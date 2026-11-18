#!/usr/bin/env bash
# Apaga o Resource Group inteiro criado por infra-azure.sh
set -euo pipefail

DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -f "$DIR_SCRIPT/azure.env" ]]; then
	set -a
	source "$DIR_SCRIPT/azure.env"
	set +a
fi
RESOURCE_GROUP="${RESOURCE_GROUP:-rg-paradacerta}"

read -r -p "Apagar o grupo $RESOURCE_GROUP e todos os seus recursos? Digite o nome do grupo para confirmar: " resposta
if [[ "$resposta" != "$RESOURCE_GROUP" ]]; then
	echo "Cancelado."
	exit 1
fi
az group delete --name "$RESOURCE_GROUP" --yes --no-wait
echo "Exclusao do grupo $RESOURCE_GROUP iniciada (leva alguns minutos)."

#!/usr/bin/env bash
# =====================================================================
# PARADA CERTA | Remove TODOS os recursos criados por infra-azure.sh
# (apaga o Resource Group inteiro). Use depois da avaliacao para nao gastar creditos.
#
#   bash scripts/destroy-azure.sh
# =====================================================================
set -euo pipefail

DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -f "$DIR_SCRIPT/azure.env" ]]; then
	set -a
	# shellcheck disable=SC1091
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

package br.com.fiap.citizentech.dto.request;

import br.com.fiap.citizentech.model.enums.TipoSinal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitacaoEmbarqueRequest(

		@NotNull(message = "O totem e obrigatorio")
		Long totemId,

		Long passageiroId,

		Long linhaId,

		@NotNull(message = "O tipo de sinal e obrigatorio (CARTAO ou BOTAO)")
		TipoSinal tipoSinal,

		@NotBlank(message = "A chave de idempotencia e obrigatoria")
		@Size(max = 80, message = "A chave de idempotencia deve ter no maximo 80 caracteres")
		String chaveIdempotencia,

		@Size(max = 300, message = "A mensagem de voz deve ter no maximo 300 caracteres")
		String mensagemVoz) {
}

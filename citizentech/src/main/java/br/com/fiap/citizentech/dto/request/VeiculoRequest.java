package br.com.fiap.citizentech.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VeiculoRequest(

		@NotBlank(message = "O prefixo e obrigatorio")
		@Size(max = 10, message = "O prefixo deve ter no maximo 10 caracteres")
		String prefixo,

		@NotNull(message = "A linha e obrigatoria")
		Long linhaId,

		Long motoristaId) {
}

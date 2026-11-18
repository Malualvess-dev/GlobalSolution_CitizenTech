package br.com.fiap.citizentech.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CartaoRequest(

		@NotNull(message = "O passageiro e obrigatorio")
		Long passageiroId,

		@NotBlank(message = "O UID do cartao e obrigatorio")
		@Size(max = 64, message = "O UID deve ter no maximo 64 caracteres")
		String uid,

		Boolean ativo) {
}

package br.com.fiap.citizentech.dto.request;

import br.com.fiap.citizentech.model.enums.StatusTotem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TotemRequest(

		@NotNull(message = "A parada e obrigatoria")
		Long paradaId,

		@NotBlank(message = "O identificador e obrigatorio")
		@Size(max = 30, message = "O identificador deve ter no maximo 30 caracteres")
		String identificador,

		@Size(min = 16, max = 200, message = "O token deve ter entre 16 e 200 caracteres")
		String token,

		StatusTotem status) {
}

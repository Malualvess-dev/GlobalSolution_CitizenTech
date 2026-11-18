package br.com.fiap.citizentech.dto.request;

import br.com.fiap.citizentech.model.enums.TipoNecessidade;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PassageiroRequest(

		@NotNull(message = "O cuidador e obrigatorio")
		Long cuidadorId,

		@NotBlank(message = "O nome e obrigatorio")
		@Size(max = 120, message = "O nome deve ter no maximo 120 caracteres")
		String nome,

		@NotNull(message = "O tipo de necessidade e obrigatorio")
		TipoNecessidade tipoNecessidade,

		@NotBlank(message = "O contato de emergencia e obrigatorio")
		@Size(max = 40, message = "O contato de emergencia deve ter no maximo 40 caracteres")
		String contatoEmergencia,

		@NotNull(message = "O aceite do termo de consentimento e obrigatorio")
		@AssertTrue(message = "O cuidador precisa aceitar o termo de consentimento")
		Boolean consentimento,

		@NotBlank(message = "A versao do termo e obrigatoria")
		@Size(max = 20, message = "A versao do termo deve ter no maximo 20 caracteres")
		String versaoTermo) {
}

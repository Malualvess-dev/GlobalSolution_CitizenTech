package br.com.fiap.citizentech.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(

		@NotBlank(message = "O nome e obrigatorio")
		@Size(max = 120, message = "O nome deve ter no maximo 120 caracteres")
		String nome,

		@NotBlank(message = "O e-mail e obrigatorio")
		@Email(message = "E-mail invalido")
		@Size(max = 160, message = "O e-mail deve ter no maximo 160 caracteres")
		String email,

		@NotBlank(message = "A senha e obrigatoria")
		@Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
		String senha,

		@Size(max = 200, message = "O token do Expo deve ter no maximo 200 caracteres")
		String expoPushToken) {
}

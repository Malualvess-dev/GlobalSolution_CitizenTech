package br.com.fiap.citizentech.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

	private static final String SENHA = "senha12345";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void registroLoginEMe() throws Exception {
		mockMvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
				{"nome": "Maria", "email": "Maria@Teste.com", "senha": "%s"}
				""".formatted(SENHA)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.perfil").value("CUIDADOR"));

		String token = login("maria@teste.com");

		mockMvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("maria@teste.com"));
	}

	@Test
	void loginComSenhaErradaDevolve401() throws Exception {
		registrar("ana@teste.com");
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "ana@teste.com", "senha": "senha-errada"}
				"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensagem").value("E-mail ou senha invalidos"));
	}

	@Test
	void semTokenOuComTokenInvalidoDevolve401EmJson() throws Exception {
		mockMvc.perform(get("/api/paradas"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.caminho").value("/api/paradas"));

		mockMvc.perform(get("/api/paradas").header(HttpHeaders.AUTHORIZATION, "Bearer token.falso.invalido"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rotasPublicasNaoPedemToken() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void cuidadorNaoAcessaRotasDeGestor() throws Exception {
		registrar("bia@teste.com");
		String token = login("bia@teste.com");

		mockMvc.perform(get("/api/paradas").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));
		mockMvc.perform(post("/api/paradas").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"codigoSptrans\": 1, \"nome\": \"Parada\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void cuidadorSoAcessaOsPropriosPassageiros() throws Exception {
		long cuidadorA = registrar("a@teste.com");
		registrar("b@teste.com");
		String tokenA = login("a@teste.com");
		String tokenB = login("b@teste.com");

		long passageiroDeA = extrairId(criarPassageiro(tokenA, cuidadorA)
				.andExpect(status().isCreated()));

		mockMvc.perform(get("/api/passageiros/{id}", passageiroDeA)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
				.andExpect(status().isForbidden());

		criarPassageiro(tokenB, cuidadorA).andExpect(status().isForbidden());

		mockMvc.perform(get("/api/passageiros").param("cuidadorId", String.valueOf(cuidadorA))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements").value(0));

		mockMvc.perform(get("/api/passageiros/{id}", passageiroDeA)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
				.andExpect(status().isOk());
	}

	private ResultActions criarPassageiro(String token, long cuidadorId) throws Exception {
		return mockMvc.perform(post("/api/passageiros").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"cuidadorId": %d, "nome": "Seu Jose", "tipoNecessidade": "IDOSO",
						 "contatoEmergencia": "11988887777", "consentimento": true, "versaoTermo": "v1"}
						""".formatted(cuidadorId)));
	}

	private long registrar(String email) throws Exception {
		return extrairId(mockMvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
				{"nome": "Cuidador", "email": "%s", "senha": "%s"}
				""".formatted(email, SENHA)))
				.andExpect(status().isCreated()));
	}

	private String login(String email) throws Exception {
		String corpo = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "%s", "senha": "%s"}
				""".formatted(email, SENHA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("Bearer"))
				.andReturn().getResponse().getContentAsString();
		return extrair(corpo, "\"token\"\\s*:\\s*\"([^\"]+)\"");
	}

	private long extrairId(ResultActions resultado) throws Exception {
		return Long.parseLong(extrair(resultado.andReturn().getResponse().getContentAsString(),
				"\"id\"\\s*:\\s*(\\d+)"));
	}

	private String extrair(String corpo, String regex) {
		Matcher matcher = Pattern.compile(regex).matcher(corpo);
		if (!matcher.find()) {
			throw new IllegalStateException("Valor nao encontrado na resposta: " + corpo);
		}
		return matcher.group(1);
	}

}

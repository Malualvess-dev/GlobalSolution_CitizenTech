package br.com.fiap.citizentech.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.model.Cartao;
import br.com.fiap.citizentech.model.Linha;
import br.com.fiap.citizentech.model.Parada;
import br.com.fiap.citizentech.model.Passageiro;
import br.com.fiap.citizentech.model.RotaFrequente;
import br.com.fiap.citizentech.model.Totem;
import br.com.fiap.citizentech.model.Usuario;
import br.com.fiap.citizentech.model.Veiculo;
import br.com.fiap.citizentech.model.enums.Perfil;
import br.com.fiap.citizentech.model.enums.TipoNecessidade;
import br.com.fiap.citizentech.repository.CartaoRepository;
import br.com.fiap.citizentech.repository.LinhaRepository;
import br.com.fiap.citizentech.repository.ParadaRepository;
import br.com.fiap.citizentech.repository.PassageiroRepository;
import br.com.fiap.citizentech.repository.RotaFrequenteRepository;
import br.com.fiap.citizentech.repository.TotemRepository;
import br.com.fiap.citizentech.repository.UsuarioRepository;
import br.com.fiap.citizentech.repository.VeiculoRepository;
import br.com.fiap.citizentech.service.HashService;
import lombok.RequiredArgsConstructor;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

	public static final String TOKEN_TOTEM_EXEMPLO = "token-totem-0001";

	public static final String UID_CARTAO_EXEMPLO = "04A1B2C3";

	private final UsuarioRepository usuarioRepository;
	private final ParadaRepository paradaRepository;
	private final LinhaRepository linhaRepository;
	private final TotemRepository totemRepository;
	private final VeiculoRepository veiculoRepository;
	private final PassageiroRepository passageiroRepository;
	private final CartaoRepository cartaoRepository;
	private final RotaFrequenteRepository rotaFrequenteRepository;
	private final PasswordEncoder passwordEncoder;
	private final HashService hashService;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (usuarioRepository.count() > 0) {
			return;
		}

		usuario("Gestora Parada Certa", "gestor@paradacerta.com", "Gestor@123", Perfil.GESTOR);
		Usuario cuidador = usuario("Ana Cuidadora", "cuidador@paradacerta.com", "Cuidador@123", Perfil.CUIDADOR);
		Usuario motorista = usuario("Carlos Motorista", "motorista@paradacerta.com", "Motorista@123", Perfil.MOTORISTA);

		Parada paulista = paradaRepository.save(Parada.builder()
				.codigoSptrans(260015039L)
				.nome("Av. Paulista, 1578 - MASP")
				.endereco("Av. Paulista, 1578 - Bela Vista, Sao Paulo - SP")
				.latitude(new BigDecimal("-23.561414"))
				.longitude(new BigDecimal("-46.655881"))
				.build());
		paradaRepository.save(Parada.builder()
				.codigoSptrans(340015329L)
				.nome("Terminal Varginha")
				.endereco("Av. Paulo Guilguer Reimberg - Grajau, Sao Paulo - SP")
				.latitude(new BigDecimal("-23.778231"))
				.longitude(new BigDecimal("-46.692540"))
				.build());

		Linha varginha = linhaRepository.save(Linha.builder()
				.codigoSptrans(1273L).letreiro("6091-10").sentido(1).destino("Terminal Varginha").build());
		linhaRepository.save(Linha.builder()
				.codigoSptrans(33041L).letreiro("875A-10").sentido(1).destino("Aclimacao").build());

		totemRepository.save(Totem.builder()
				.parada(paulista)
				.identificador("TOTEM-0001")
				.tokenHash(hashService.hmacSha256(TOKEN_TOTEM_EXEMPLO))
				.build());

		veiculoRepository.save(Veiculo.builder().prefixo("6 1234").linha(varginha).motorista(motorista).build());

		Passageiro passageiro = passageiroRepository.save(Passageiro.builder()
				.cuidador(cuidador)
				.nome("Joao Passageiro")
				.tipoNecessidade(TipoNecessidade.BAIXA_VISAO)
				.contatoEmergencia("(11) 98888-7777")
				.consentimentoEm(LocalDateTime.now())
				.versaoTermo("1.0")
				.build());

		cartaoRepository.save(Cartao.builder()
				.passageiro(passageiro)
				.uidHash(hashService.hmacSha256(UID_CARTAO_EXEMPLO))
				.ativo(true)
				.build());

		rotaFrequenteRepository.save(RotaFrequente.builder()
				.passageiro(passageiro).parada(paulista).linha(varginha).apelido("Casa para o trabalho").build());

		log.info("Perfil dev: dados de exemplo criados (gestor, cuidador e motorista @paradacerta.com)");
	}

	private Usuario usuario(String nome, String email, String senha, Perfil perfil) {
		return usuarioRepository.save(Usuario.builder()
				.nome(nome)
				.email(email)
				.senhaHash(passwordEncoder.encode(senha))
				.perfil(perfil)
				.build());
	}

}

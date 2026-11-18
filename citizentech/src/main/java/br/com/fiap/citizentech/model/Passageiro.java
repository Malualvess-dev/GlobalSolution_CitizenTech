package br.com.fiap.citizentech.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import br.com.fiap.citizentech.model.enums.TipoNecessidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "passageiro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Passageiro {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cuidador_id", nullable = false)
	private Usuario cuidador;

	@Column(name = "nome", nullable = false, length = 120)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_necessidade", nullable = false, length = 20)
	private TipoNecessidade tipoNecessidade;

	@Column(name = "contato_emergencia", nullable = false, length = 40)
	private String contatoEmergencia;

	@Column(name = "consentimento_em", nullable = false)
	private LocalDateTime consentimentoEm;

	@Column(name = "versao_termo", nullable = false, length = 20)
	private String versaoTermo;

	@CreationTimestamp
	@Column(name = "criado_em", nullable = false, updatable = false)
	private LocalDateTime criadoEm;

}

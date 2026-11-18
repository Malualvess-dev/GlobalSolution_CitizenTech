package br.com.fiap.citizentech.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import br.com.fiap.citizentech.model.enums.ResultadoTentativa;
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
@Table(name = "tentativa_embarque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TentativaEmbarque {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "solicitacao_id", nullable = false)
	private SolicitacaoEmbarque solicitacao;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "linha_id", nullable = false)
	private Linha linha;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "veiculo_id")
	private Veiculo veiculo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "motorista_id")
	private Usuario motorista;

	@Column(name = "previsao_chegada")
	private LocalDateTime previsaoChegada;

	@CreationTimestamp
	@Column(name = "alertado_em", nullable = false, updatable = false)
	private LocalDateTime alertadoEm;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(name = "resultado", nullable = false, length = 15)
	private ResultadoTentativa resultado = ResultadoTentativa.PENDENTE;

	@Column(name = "confirmado_em")
	private LocalDateTime confirmadoEm;

}

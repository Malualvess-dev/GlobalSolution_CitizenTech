package br.com.fiap.citizentech.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import br.com.fiap.citizentech.model.enums.StatusSolicitacao;
import br.com.fiap.citizentech.model.enums.TipoSinal;
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
@Table(name = "solicitacao_embarque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SolicitacaoEmbarque {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "passageiro_id")
	private Passageiro passageiro;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "totem_id", nullable = false)
	private Totem totem;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "linha_id")
	private Linha linha;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_sinal", nullable = false, length = 10)
	private TipoSinal tipoSinal;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusSolicitacao status = StatusSolicitacao.CRIADA;

	@Column(name = "chave_idempotencia", nullable = false, unique = true, length = 80)
	private String chaveIdempotencia;

	@Column(name = "mensagem_voz", length = 300)
	private String mensagemVoz;

	@CreationTimestamp
	@Column(name = "criada_em", nullable = false, updatable = false)
	private LocalDateTime criadaEm;

	@UpdateTimestamp
	@Column(name = "atualizada_em", nullable = false)
	private LocalDateTime atualizadaEm;

}

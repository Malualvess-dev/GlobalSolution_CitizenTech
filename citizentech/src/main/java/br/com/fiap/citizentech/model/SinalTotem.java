package br.com.fiap.citizentech.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "sinal_totem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SinalTotem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "totem_id", nullable = false)
	private Totem totem;

	@Column(name = "chave_idempotencia", nullable = false, unique = true, length = 80)
	private String chaveIdempotencia;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 10)
	private TipoSinal tipo;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "uid_hash", length = 64)
	private String uidHash;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "solicitacao_id")
	private SolicitacaoEmbarque solicitacao;

	@CreationTimestamp
	@Column(name = "recebido_em", nullable = false, updatable = false)
	private LocalDateTime recebidoEm;

}

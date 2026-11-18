package br.com.fiap.citizentech.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import br.com.fiap.citizentech.model.enums.OperacaoAuditoria;
import br.com.fiap.citizentech.model.enums.StatusSolicitacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Immutable
@Table(name = "auditoria_solicitacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuditoriaSolicitacao {

	@Id
	@EqualsAndHashCode.Include
	private Long id;

	@Column(name = "solicitacao_id", nullable = false)
	private Long solicitacaoId;

	@Enumerated(EnumType.STRING)
	@Column(name = "operacao", nullable = false, length = 6)
	private OperacaoAuditoria operacao;

	@Enumerated(EnumType.STRING)
	@Column(name = "status_antigo", length = 20)
	private StatusSolicitacao statusAntigo;

	@Enumerated(EnumType.STRING)
	@Column(name = "status_novo", length = 20)
	private StatusSolicitacao statusNovo;

	@Column(name = "usuario_banco", nullable = false, length = 60)
	private String usuarioBanco;

	@Column(name = "data_hora", nullable = false)
	private LocalDateTime dataHora;

}

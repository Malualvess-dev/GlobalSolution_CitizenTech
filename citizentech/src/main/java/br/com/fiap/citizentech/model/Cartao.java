package br.com.fiap.citizentech.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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
@Table(name = "cartao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cartao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "passageiro_id", nullable = false)
	private Passageiro passageiro;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "uid_hash", nullable = false, unique = true, length = 64)
	private String uidHash;

	@Builder.Default
	@Convert(converter = NumericBooleanConverter.class)
	@Column(name = "ativo", nullable = false, precision = 1)
	private Boolean ativo = Boolean.TRUE;

	@CreationTimestamp
	@Column(name = "criado_em", nullable = false, updatable = false)
	private LocalDateTime criadoEm;

}

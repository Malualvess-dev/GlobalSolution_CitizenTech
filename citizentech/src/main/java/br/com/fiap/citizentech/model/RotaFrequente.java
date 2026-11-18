package br.com.fiap.citizentech.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rota_frequente", uniqueConstraints = @UniqueConstraint(
		name = "uq_rota_pass_parada_linha",
		columnNames = { "passageiro_id", "parada_id", "linha_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RotaFrequente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "passageiro_id", nullable = false)
	private Passageiro passageiro;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "parada_id", nullable = false)
	private Parada parada;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "linha_id", nullable = false)
	private Linha linha;

	@Column(name = "apelido", length = 60)
	private String apelido;

}

package br.com.fiap.citizentech.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Parada {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Column(name = "codigo_sptrans", nullable = false, unique = true, precision = 10)
	private Long codigoSptrans;

	@Column(name = "nome", nullable = false, length = 150)
	private String nome;

	@Column(name = "endereco", length = 250)
	private String endereco;

	@Column(name = "latitude", precision = 9, scale = 6)
	private BigDecimal latitude;

	@Column(name = "longitude", precision = 9, scale = 6)
	private BigDecimal longitude;

}

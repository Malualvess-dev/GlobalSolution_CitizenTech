package br.com.fiap.citizentech.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.citizentech.model.RotaFrequente;

public interface RotaFrequenteRepository extends JpaRepository<RotaFrequente, Long> {

	Page<RotaFrequente> findByPassageiroId(Long passageiroId, Pageable pageable);

	Page<RotaFrequente> findByPassageiroCuidadorId(Long cuidadorId, Pageable pageable);

	boolean existsByPassageiroIdAndParadaIdAndLinhaId(Long passageiroId, Long paradaId, Long linhaId);

	boolean existsByPassageiroIdAndParadaIdAndLinhaIdAndIdNot(Long passageiroId, Long paradaId, Long linhaId, Long id);

}

package br.com.fiap.citizentech.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.citizentech.model.Cartao;

public interface CartaoRepository extends JpaRepository<Cartao, Long> {

	Page<Cartao> findByPassageiroId(Long passageiroId, Pageable pageable);

	Page<Cartao> findByPassageiroCuidadorId(Long cuidadorId, Pageable pageable);

	Optional<Cartao> findByUidHashAndAtivoTrue(String uidHash);

	boolean existsByUidHash(String uidHash);

	boolean existsByUidHashAndIdNot(String uidHash, Long id);

}

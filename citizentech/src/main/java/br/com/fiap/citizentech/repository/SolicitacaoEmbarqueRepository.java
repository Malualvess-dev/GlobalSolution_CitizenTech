package br.com.fiap.citizentech.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.citizentech.model.SolicitacaoEmbarque;
import br.com.fiap.citizentech.model.enums.StatusSolicitacao;

public interface SolicitacaoEmbarqueRepository extends JpaRepository<SolicitacaoEmbarque, Long> {

	Page<SolicitacaoEmbarque> findByStatus(StatusSolicitacao status, Pageable pageable);

	Optional<SolicitacaoEmbarque> findByChaveIdempotencia(String chaveIdempotencia);

	boolean existsByChaveIdempotencia(String chaveIdempotencia);

	boolean existsByPassageiroIdAndStatusIn(Long passageiroId, Collection<StatusSolicitacao> status);

}

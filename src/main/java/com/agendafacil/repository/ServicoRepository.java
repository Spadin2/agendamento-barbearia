package com.agendafacil.repository;

import com.agendafacil.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<Servico> findByEmpresaIdOrderByNomeAsc(Long empresaId);

    List<Servico> findByEmpresaIdAndAtivoTrueOrderByNomeAsc(Long empresaId);

    Optional<Servico> findByIdAndEmpresaId(Long id, Long empresaId);
}

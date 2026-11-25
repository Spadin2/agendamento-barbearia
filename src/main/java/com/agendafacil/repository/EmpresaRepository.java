package com.agendafacil.repository;

import com.agendafacil.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    List<Empresa> findByAtivaTrueOrderByNomeAsc();

    boolean existsByCnpj(String cnpj);
}

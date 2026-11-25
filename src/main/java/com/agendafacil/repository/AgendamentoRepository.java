package com.agendafacil.repository;

import com.agendafacil.dto.AgendamentoResposta;
import com.agendafacil.model.Agendamento;
import com.agendafacil.model.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    Optional<Agendamento> findByIdAndEmpresaId(Long id, Long empresaId);

    // Há sobreposição quando: novoInicio < fimExistente E novoFim > inicioExistente
    @Query("select count(a) from Agendamento a "
         + "where a.funcionarioId = :funcionarioId and a.status <> :cancelado "
         + "and a.inicio < :fim and a.fim > :inicio")
    long contarConflitos(@Param("funcionarioId") Long funcionarioId,
                         @Param("cancelado") StatusAgendamento cancelado,
                         @Param("inicio") LocalDateTime inicio,
                         @Param("fim") LocalDateTime fim);

    @Query("select a from Agendamento a "
         + "where a.funcionarioId = :funcionarioId and a.status <> :cancelado "
         + "and a.inicio < :fim and a.fim > :inicio")
    List<Agendamento> buscarAtivosNoPeriodo(@Param("funcionarioId") Long funcionarioId,
                                            @Param("cancelado") StatusAgendamento cancelado,
                                            @Param("inicio") LocalDateTime inicio,
                                            @Param("fim") LocalDateTime fim);

    // empresaId vem SEMPRE da sessão; funcionarioId/clienteId filtram por perfil (null = sem filtro)
    @Query("select new com.agendafacil.dto.AgendamentoResposta("
         + "a.id, a.inicio, a.fim, a.status, s.nome, s.preco, c.nome, f.nome) "
         + "from Agendamento a, Servico s, Usuario c, Usuario f "
         + "where a.servicoId = s.id and a.clienteId = c.id and a.funcionarioId = f.id "
         + "and a.empresaId = :empresaId "
         + "and (:funcionarioId is null or a.funcionarioId = :funcionarioId) "
         + "and (:clienteId is null or a.clienteId = :clienteId) "
         + "order by a.inicio desc")
    List<AgendamentoResposta> listar(@Param("empresaId") Long empresaId,
                                     @Param("funcionarioId") Long funcionarioId,
                                     @Param("clienteId") Long clienteId);
}

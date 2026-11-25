package com.agendafacil.dto;

import com.agendafacil.model.StatusAgendamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgendamentoResposta(
        Long id,
        LocalDateTime inicio,
        LocalDateTime fim,
        StatusAgendamento status,
        String servico,
        BigDecimal preco,
        String cliente,
        String funcionario) {
}

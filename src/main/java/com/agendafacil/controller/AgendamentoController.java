package com.agendafacil.controller;

import com.agendafacil.dto.AgendamentoResposta;
import com.agendafacil.dto.Dtos.AgendamentoRequest;
import com.agendafacil.security.UsuarioLogado;
import com.agendafacil.service.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @GetMapping
    public List<AgendamentoResposta> listar(@AuthenticationPrincipal UsuarioLogado u) {
        return agendamentoService.listar(u);
    }

    @GetMapping("/horarios")
    public List<String> horarios(@AuthenticationPrincipal UsuarioLogado u,
                                 @RequestParam Long funcionarioId,
                                 @RequestParam Long servicoId,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return agendamentoService.horariosLivres(u, funcionarioId, servicoId, data);
    }

    @PreAuthorize("hasRole('CLIENTE')")
    @PostMapping
    public ResponseEntity<Map<String, Long>> criar(@AuthenticationPrincipal UsuarioLogado u,
                                                   @Valid @RequestBody AgendamentoRequest r) {
        Long id = agendamentoService.criar(u, r);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Long id) {
        agendamentoService.cancelar(u, id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('DONO','FUNCIONARIO')")
    @PatchMapping("/{id}/concluir")
    public ResponseEntity<Void> concluir(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Long id) {
        agendamentoService.concluir(u, id);
        return ResponseEntity.noContent().build();
    }
}

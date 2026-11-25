package com.agendafacil.controller;

import com.agendafacil.dto.Dtos.ServicoRequest;
import com.agendafacil.dto.Dtos.ServicoResposta;
import com.agendafacil.security.UsuarioLogado;
import com.agendafacil.service.ServicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    @GetMapping
    public List<ServicoResposta> listar(@AuthenticationPrincipal UsuarioLogado u) {
        return servicoService.listar(u);
    }

    @PreAuthorize("hasRole('DONO')")
    @PostMapping
    public ResponseEntity<ServicoResposta> criar(@AuthenticationPrincipal UsuarioLogado u,
                                                 @Valid @RequestBody ServicoRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicoService.criar(u, r));
    }

    @PreAuthorize("hasRole('DONO')")
    @PutMapping("/{id}")
    public ServicoResposta atualizar(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Long id,
                                     @Valid @RequestBody ServicoRequest r) {
        return servicoService.atualizar(u, id, r);
    }

    @PreAuthorize("hasRole('DONO')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Long id) {
        servicoService.desativar(u, id);
        return ResponseEntity.noContent().build();
    }
}

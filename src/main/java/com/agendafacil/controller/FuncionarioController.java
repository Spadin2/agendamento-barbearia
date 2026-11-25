package com.agendafacil.controller;

import com.agendafacil.dto.Dtos.FuncionarioRequest;
import com.agendafacil.dto.Dtos.FuncionarioResposta;
import com.agendafacil.security.UsuarioLogado;
import com.agendafacil.service.FuncionarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/funcionarios")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    public FuncionarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    // Cliente precisa da lista para escolher o profissional
    @GetMapping
    public List<FuncionarioResposta> listar(@AuthenticationPrincipal UsuarioLogado u) {
        return funcionarioService.listar(u);
    }

    @PreAuthorize("hasRole('DONO')")
    @PostMapping
    public ResponseEntity<FuncionarioResposta> criar(@AuthenticationPrincipal UsuarioLogado u,
                                                     @Valid @RequestBody FuncionarioRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funcionarioService.criar(u, r));
    }

    @PreAuthorize("hasRole('DONO')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Long id) {
        funcionarioService.desativar(u, id);
        return ResponseEntity.noContent().build();
    }
}

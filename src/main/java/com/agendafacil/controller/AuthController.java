package com.agendafacil.controller;

import com.agendafacil.dto.Dtos.CadastroClienteRequest;
import com.agendafacil.dto.Dtos.CadastroEmpresaRequest;
import com.agendafacil.dto.Dtos.EmpresaPublica;
import com.agendafacil.dto.Dtos.UsuarioResposta;
import com.agendafacil.model.Empresa;
import com.agendafacil.repository.EmpresaRepository;
import com.agendafacil.security.UsuarioLogado;
import com.agendafacil.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final EmpresaRepository empresas;

    public AuthController(AuthService authService, EmpresaRepository empresas) {
        this.authService = authService;
        this.empresas = empresas;
    }

    // Público: cadastro de uma nova barbearia (cria o DONO)
    @PostMapping("/auth/cadastro-empresa")
    public ResponseEntity<Void> cadastrarEmpresa(@Valid @RequestBody CadastroEmpresaRequest r) {
        authService.cadastrarEmpresa(r);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Público: cadastro de cliente em uma barbearia existente
    @PostMapping("/auth/cadastro-cliente")
    public ResponseEntity<Void> cadastrarCliente(@Valid @RequestBody CadastroClienteRequest r) {
        authService.cadastrarCliente(r);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Público: usado na tela de cadastro do cliente
    @GetMapping("/publico/empresas")
    public List<EmpresaPublica> listarEmpresas() {
        return empresas.findByAtivaTrueOrderByNomeAsc().stream()
                .map(e -> new EmpresaPublica(e.getId(), e.getNome()))
                .toList();
    }

    // Quem sou eu? O front usa para saber se há sessão e qual o perfil
    @GetMapping("/me")
    public UsuarioResposta me(@AuthenticationPrincipal UsuarioLogado u) {
        String empresaNome = empresas.findById(u.getEmpresaId()).map(Empresa::getNome).orElse("");
        return new UsuarioResposta(u.getId(), u.getNome(), u.getUsername(), u.getPerfil().name(),
                u.getEmpresaId(), empresaNome);
    }
}

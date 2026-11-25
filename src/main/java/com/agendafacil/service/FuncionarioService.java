package com.agendafacil.service;

import com.agendafacil.dto.Dtos.FuncionarioRequest;
import com.agendafacil.dto.Dtos.FuncionarioResposta;
import com.agendafacil.exception.ApiException;
import com.agendafacil.model.Perfil;
import com.agendafacil.model.Usuario;
import com.agendafacil.repository.UsuarioRepository;
import com.agendafacil.security.UsuarioLogado;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FuncionarioService {

    private final UsuarioRepository usuarios;
    private final AuthService authService;

    public FuncionarioService(UsuarioRepository usuarios, AuthService authService) {
        this.usuarios = usuarios;
        this.authService = authService;
    }

    public List<FuncionarioResposta> listar(UsuarioLogado u) {
        return usuarios.findByEmpresaIdAndPerfilAndAtivoTrueOrderByNomeAsc(u.getEmpresaId(), Perfil.FUNCIONARIO)
                .stream().map(this::paraResposta).toList();
    }

    @Transactional
    public FuncionarioResposta criar(UsuarioLogado dono, FuncionarioRequest r) {
        Usuario novo = authService.criarUsuario(dono.getEmpresaId(), r.nome(), r.email(), r.senha(), Perfil.FUNCIONARIO);
        return paraResposta(novo);
    }

    @Transactional
    public void desativar(UsuarioLogado dono, Long id) {
        Usuario f = usuarios.findByIdAndEmpresaId(id, dono.getEmpresaId())
                .filter(x -> x.getPerfil() == Perfil.FUNCIONARIO)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profissional não encontrado"));
        f.setAtivo(false);
        usuarios.save(f);
    }

    private FuncionarioResposta paraResposta(Usuario f) {
        return new FuncionarioResposta(f.getId(), f.getNome(), f.getEmail());
    }
}

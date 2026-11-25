package com.agendafacil.service;

import com.agendafacil.dto.Dtos.CadastroClienteRequest;
import com.agendafacil.dto.Dtos.CadastroEmpresaRequest;
import com.agendafacil.exception.ApiException;
import com.agendafacil.model.Empresa;
import com.agendafacil.model.Perfil;
import com.agendafacil.model.Usuario;
import com.agendafacil.repository.EmpresaRepository;
import com.agendafacil.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final EmpresaRepository empresas;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public AuthService(EmpresaRepository empresas, UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.empresas = empresas;
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    /** Cria a barbearia (tenant) e o usuário DONO em uma única transação. */
    @Transactional
    public void cadastrarEmpresa(CadastroEmpresaRequest r) {
        String cnpj = (r.cnpj() == null || r.cnpj().isBlank()) ? null : r.cnpj().trim();
        if (cnpj != null && empresas.existsByCnpj(cnpj)) {
            throw new ApiException(HttpStatus.CONFLICT, "CNPJ já cadastrado");
        }
        validarEmailLivre(normalizarEmail(r.email()));

        Empresa empresa = new Empresa();
        empresa.setNome(r.nomeEmpresa().trim());
        empresa.setCnpj(cnpj);
        empresa = empresas.save(empresa);

        criarUsuario(empresa.getId(), r.nome(), r.email(), r.senha(), Perfil.DONO);
    }

    @Transactional
    public void cadastrarCliente(CadastroClienteRequest r) {
        Empresa empresa = empresas.findById(r.empresaId())
                .filter(Empresa::isAtiva)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Barbearia não encontrada"));
        criarUsuario(empresa.getId(), r.nome(), r.email(), r.senha(), Perfil.CLIENTE);
    }

    @Transactional
    public Usuario criarUsuario(Long empresaId, String nome, String email, String senha, Perfil perfil) {
        String emailNormalizado = normalizarEmail(email);
        validarEmailLivre(emailNormalizado);

        Usuario u = new Usuario();
        u.setEmpresaId(empresaId);
        u.setNome(nome.trim());
        u.setEmail(emailNormalizado);
        u.setSenhaHash(encoder.encode(senha));
        u.setPerfil(perfil);
        return usuarios.save(u);
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }

    private void validarEmailLivre(String email) {
        if (usuarios.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
    }
}

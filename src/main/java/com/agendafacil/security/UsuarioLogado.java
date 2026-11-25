package com.agendafacil.security;

import com.agendafacil.model.Perfil;
import com.agendafacil.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Usuário guardado na sessão. O empresaId daqui é a fonte de verdade do tenant:
 * nenhuma regra confia em empresaId enviado pelo front.
 */
public class UsuarioLogado implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final Long empresaId;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final Perfil perfil;
    private final boolean ativo;

    public UsuarioLogado(Usuario u) {
        this.id = u.getId();
        this.empresaId = u.getEmpresaId();
        this.nome = u.getNome();
        this.email = u.getEmail();
        this.senhaHash = u.getSenhaHash();
        this.perfil = u.getPerfil();
        this.ativo = u.isAtivo();
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getNome() { return nome; }
    public Perfil getPerfil() { return perfil; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    @Override public String getPassword() { return senhaHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return ativo; }
}

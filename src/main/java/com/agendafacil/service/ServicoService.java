package com.agendafacil.service;

import com.agendafacil.dto.Dtos.ServicoRequest;
import com.agendafacil.dto.Dtos.ServicoResposta;
import com.agendafacil.exception.ApiException;
import com.agendafacil.model.Perfil;
import com.agendafacil.model.Servico;
import com.agendafacil.repository.ServicoRepository;
import com.agendafacil.security.UsuarioLogado;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicoService {

    private final ServicoRepository servicos;

    public ServicoService(ServicoRepository servicos) {
        this.servicos = servicos;
    }

    /** DONO vê todos (inclusive inativos); os demais perfis só veem os ativos. */
    public List<ServicoResposta> listar(UsuarioLogado u) {
        List<Servico> lista = u.getPerfil() == Perfil.DONO
                ? servicos.findByEmpresaIdOrderByNomeAsc(u.getEmpresaId())
                : servicos.findByEmpresaIdAndAtivoTrueOrderByNomeAsc(u.getEmpresaId());
        return lista.stream().map(this::paraResposta).toList();
    }

    @Transactional
    public ServicoResposta criar(UsuarioLogado u, ServicoRequest r) {
        Servico s = new Servico();
        s.setEmpresaId(u.getEmpresaId());
        preencher(s, r);
        return paraResposta(servicos.save(s));
    }

    @Transactional
    public ServicoResposta atualizar(UsuarioLogado u, Long id, ServicoRequest r) {
        Servico s = buscar(u, id);
        preencher(s, r);
        if (r.ativo() != null) {
            s.setAtivo(r.ativo());
        }
        return paraResposta(servicos.save(s));
    }

    @Transactional
    public void desativar(UsuarioLogado u, Long id) {
        Servico s = buscar(u, id);
        s.setAtivo(false);
        servicos.save(s);
    }

    private Servico buscar(UsuarioLogado u, Long id) {
        // Sempre filtra pela empresa da sessão: DONO de outra barbearia recebe 404
        return servicos.findByIdAndEmpresaId(id, u.getEmpresaId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Serviço não encontrado"));
    }

    private void preencher(Servico s, ServicoRequest r) {
        s.setNome(r.nome().trim());
        s.setDuracaoMin(r.duracaoMin());
        s.setPreco(r.preco());
    }

    private ServicoResposta paraResposta(Servico s) {
        return new ServicoResposta(s.getId(), s.getNome(), s.getDuracaoMin(), s.getPreco(), s.isAtivo());
    }
}

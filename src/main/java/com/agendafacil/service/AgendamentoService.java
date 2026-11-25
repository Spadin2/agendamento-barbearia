package com.agendafacil.service;

import com.agendafacil.dto.AgendamentoResposta;
import com.agendafacil.dto.Dtos.AgendamentoRequest;
import com.agendafacil.exception.ApiException;
import com.agendafacil.model.Agendamento;
import com.agendafacil.model.Perfil;
import com.agendafacil.model.Servico;
import com.agendafacil.model.StatusAgendamento;
import com.agendafacil.model.Usuario;
import com.agendafacil.repository.AgendamentoRepository;
import com.agendafacil.repository.ServicoRepository;
import com.agendafacil.repository.UsuarioRepository;
import com.agendafacil.security.UsuarioLogado;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgendamentoService {

    private static final LocalTime ABERTURA = LocalTime.of(9, 0);
    private static final LocalTime FECHAMENTO = LocalTime.of(18, 0);
    private static final int PASSO_MINUTOS = 30;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final AgendamentoRepository agendamentos;
    private final ServicoRepository servicos;
    private final UsuarioRepository usuarios;

    public AgendamentoService(AgendamentoRepository agendamentos, ServicoRepository servicos,
                              UsuarioRepository usuarios) {
        this.agendamentos = agendamentos;
        this.servicos = servicos;
        this.usuarios = usuarios;
    }

    /** DONO vê a agenda toda da empresa; FUNCIONARIO só a sua; CLIENTE só os seus. */
    public List<AgendamentoResposta> listar(UsuarioLogado u) {
        Long funcionarioId = u.getPerfil() == Perfil.FUNCIONARIO ? u.getId() : null;
        Long clienteId = u.getPerfil() == Perfil.CLIENTE ? u.getId() : null;
        return agendamentos.listar(u.getEmpresaId(), funcionarioId, clienteId);
    }

    /** Horários livres (de 30 em 30 min, das 09:00 às 18:00) de um profissional em um dia. */
    public List<String> horariosLivres(UsuarioLogado u, Long funcionarioId, Long servicoId, LocalDate data) {
        Servico servico = servicoValido(u.getEmpresaId(), servicoId);
        funcionarioValido(u.getEmpresaId(), funcionarioId, false);

        List<Agendamento> ocupados = agendamentos.buscarAtivosNoPeriodo(
                funcionarioId, StatusAgendamento.CANCELADO, data.atStartOfDay(), data.plusDays(1).atStartOfDay());

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime limite = data.atTime(FECHAMENTO);
        List<String> livres = new ArrayList<>();

        for (LocalDateTime ini = data.atTime(ABERTURA); ; ini = ini.plusMinutes(PASSO_MINUTOS)) {
            LocalDateTime inicioSlot = ini;
            LocalDateTime fimSlot = inicioSlot.plusMinutes(servico.getDuracaoMin());
            if (fimSlot.isAfter(limite)) {
                break;
            }
            if (inicioSlot.isBefore(agora)) {
                continue;
            }
            boolean livre = ocupados.stream()
                    .noneMatch(a -> inicioSlot.isBefore(a.getFim()) && fimSlot.isAfter(a.getInicio()));
            if (livre) {
                livres.add(inicioSlot.toLocalTime().format(HORA));
            }
        }
        return livres;
    }

    /**
     * Cria o agendamento. A linha do profissional é travada (SELECT ... FOR UPDATE) durante a
     * transação: dois clientes tentando o mesmo horário ao mesmo tempo são colocados em fila,
     * e o segundo recebe 409 em vez de gerar conflito.
     */
    @Transactional
    public Long criar(UsuarioLogado cliente, AgendamentoRequest r) {
        Long empresaId = cliente.getEmpresaId();
        Servico servico = servicoValido(empresaId, r.servicoId());
        funcionarioValido(empresaId, r.funcionarioId(), true);

        LocalDateTime inicio = r.inicio().withSecond(0).withNano(0);
        LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMin());
        validarJanela(inicio, fim);

        if (agendamentos.contarConflitos(r.funcionarioId(), StatusAgendamento.CANCELADO, inicio, fim) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Horário indisponível para este profissional");
        }

        Agendamento a = new Agendamento();
        a.setEmpresaId(empresaId);
        a.setServicoId(servico.getId());
        a.setClienteId(cliente.getId());
        a.setFuncionarioId(r.funcionarioId());
        a.setInicio(inicio);
        a.setFim(fim);
        a.setStatus(StatusAgendamento.AGENDADO);
        return agendamentos.save(a).getId();
    }

    @Transactional
    public void cancelar(UsuarioLogado u, Long id) {
        Agendamento a = buscar(u, id);
        boolean permitido = switch (u.getPerfil()) {
            case DONO -> true;
            case CLIENTE -> a.getClienteId().equals(u.getId());
            case FUNCIONARIO -> a.getFuncionarioId().equals(u.getId());
        };
        if (!permitido) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Você não pode alterar este agendamento");
        }
        exigirAgendado(a, "Só é possível cancelar agendamentos ativos");
        a.setStatus(StatusAgendamento.CANCELADO);
        agendamentos.save(a);
    }

    @Transactional
    public void concluir(UsuarioLogado u, Long id) {
        Agendamento a = buscar(u, id);
        boolean permitido = switch (u.getPerfil()) {
            case DONO -> true;
            case FUNCIONARIO -> a.getFuncionarioId().equals(u.getId());
            case CLIENTE -> false;
        };
        if (!permitido) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Você não pode alterar este agendamento");
        }
        exigirAgendado(a, "Só é possível concluir agendamentos ativos");
        a.setStatus(StatusAgendamento.CONCLUIDO);
        agendamentos.save(a);
    }

    // ---------- auxiliares ----------

    private Agendamento buscar(UsuarioLogado u, Long id) {
        return agendamentos.findByIdAndEmpresaId(id, u.getEmpresaId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Agendamento não encontrado"));
    }

    private void exigirAgendado(Agendamento a, String mensagem) {
        if (a.getStatus() != StatusAgendamento.AGENDADO) {
            throw new ApiException(HttpStatus.CONFLICT, mensagem);
        }
    }

    private Servico servicoValido(Long empresaId, Long servicoId) {
        return servicos.findByIdAndEmpresaId(servicoId, empresaId)
                .filter(Servico::isAtivo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Serviço não encontrado"));
    }

    private Usuario funcionarioValido(Long empresaId, Long funcionarioId, boolean travar) {
        var encontrado = travar ? usuarios.findByIdForUpdate(funcionarioId) : usuarios.findById(funcionarioId);
        return encontrado
                .filter(f -> f.getEmpresaId().equals(empresaId)
                        && f.getPerfil() == Perfil.FUNCIONARIO
                        && f.isAtivo())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profissional não encontrado"));
    }

    private void validarJanela(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Escolha um horário no futuro");
        }
        if (inicio.getMinute() % PASSO_MINUTOS != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Horário inválido: use intervalos de 30 minutos");
        }
        LocalDate dia = inicio.toLocalDate();
        if (inicio.isBefore(dia.atTime(ABERTURA)) || fim.isAfter(dia.atTime(FECHAMENTO))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Fora do horário de funcionamento (09:00 às 18:00)");
        }
    }
}

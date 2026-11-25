package com.agendafacil.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Objetos de entrada (Request) e saída (Resposta) da API. */
public final class Dtos {

    private Dtos() {
    }

    // ---------- Entrada ----------

    public record CadastroEmpresaRequest(
            @NotBlank(message = "Informe o nome da barbearia")
            @Size(max = 120, message = "Nome da barbearia muito longo") String nomeEmpresa,
            @Size(max = 18, message = "CNPJ inválido") String cnpj,
            @NotBlank(message = "Informe seu nome")
            @Size(max = 120, message = "Nome muito longo") String nome,
            @NotBlank(message = "Informe o e-mail")
            @Email(message = "E-mail inválido")
            @Size(max = 150, message = "E-mail muito longo") String email,
            @NotBlank(message = "Informe a senha")
            @Size(min = 6, max = 72, message = "A senha deve ter de 6 a 72 caracteres") String senha) {
    }

    public record CadastroClienteRequest(
            @NotNull(message = "Escolha a barbearia") Long empresaId,
            @NotBlank(message = "Informe seu nome")
            @Size(max = 120, message = "Nome muito longo") String nome,
            @NotBlank(message = "Informe o e-mail")
            @Email(message = "E-mail inválido")
            @Size(max = 150, message = "E-mail muito longo") String email,
            @NotBlank(message = "Informe a senha")
            @Size(min = 6, max = 72, message = "A senha deve ter de 6 a 72 caracteres") String senha) {
    }

    public record FuncionarioRequest(
            @NotBlank(message = "Informe o nome")
            @Size(max = 120, message = "Nome muito longo") String nome,
            @NotBlank(message = "Informe o e-mail")
            @Email(message = "E-mail inválido")
            @Size(max = 150, message = "E-mail muito longo") String email,
            @NotBlank(message = "Informe a senha")
            @Size(min = 6, max = 72, message = "A senha deve ter de 6 a 72 caracteres") String senha) {
    }

    public record ServicoRequest(
            @NotBlank(message = "Informe o nome do serviço")
            @Size(max = 100, message = "Nome do serviço muito longo") String nome,
            @NotNull(message = "Informe a duração")
            @Min(value = 5, message = "Duração mínima de 5 minutos")
            @Max(value = 480, message = "Duração máxima de 480 minutos") Integer duracaoMin,
            @NotNull(message = "Informe o preço")
            @DecimalMin(value = "0.00", message = "Preço inválido")
            @Digits(integer = 8, fraction = 2, message = "Preço inválido") BigDecimal preco,
            Boolean ativo) {
    }

    public record AgendamentoRequest(
            @NotNull(message = "Escolha o serviço") Long servicoId,
            @NotNull(message = "Escolha o profissional") Long funcionarioId,
            @NotNull(message = "Escolha o horário") LocalDateTime inicio) {
    }

    // ---------- Saída ----------

    public record UsuarioResposta(Long id, String nome, String email, String perfil,
                                  Long empresaId, String empresaNome) {
    }

    public record EmpresaPublica(Long id, String nome) {
    }

    public record ServicoResposta(Long id, String nome, Integer duracaoMin, BigDecimal preco, boolean ativo) {
    }

    public record FuncionarioResposta(Long id, String nome, String email) {
    }
}

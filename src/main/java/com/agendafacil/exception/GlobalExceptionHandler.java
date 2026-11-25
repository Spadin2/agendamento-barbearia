package com.agendafacil.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Converte exceções em JSON {"erro": "mensagem"}.
 * Não existe handler genérico para Exception de propósito: o AccessDeniedException
 * (403) precisa chegar ao Spring Security.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> regraDeNegocio(ApiException e) {
        return resposta(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Dados inválidos");
        return resposta(HttpStatus.BAD_REQUEST, mensagem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpoInvalido(HttpMessageNotReadableException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida: confira os formatos de data e número");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> banco(DataAccessException e) {
        String causa = NestedExceptionUtils.getMostSpecificCause(e).getMessage();
        // Mensagem lançada pelo trigger do schema.sql
        if (causa != null && causa.contains("Conflito de horario")) {
            return resposta(HttpStatus.CONFLICT, "Horário indisponível para este profissional");
        }
        if (e instanceof DataIntegrityViolationException) {
            return resposta(HttpStatus.CONFLICT, "Registro duplicado ou inválido");
        }
        log.error("Erro ao acessar o banco de dados", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao acessar o banco de dados");
    }

    private ResponseEntity<Map<String, String>> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem));
    }
}

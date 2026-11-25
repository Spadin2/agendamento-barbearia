package com.agendafacil.exception;

import org.springframework.http.HttpStatus;

/** Erro de regra de negócio com o status HTTP que deve ser devolvido ao front. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

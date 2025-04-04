package com.systems.fele.memendex_server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "No such meme")
public class NoSuchMemeError extends RuntimeException {

    public NoSuchMemeError() { this("No such meme"); }

    public NoSuchMemeError(String message) {
        super(message);
    }
}

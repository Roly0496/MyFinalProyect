package com.horneo.backend.exception;

public class CredencialesIncorrectasException extends RuntimeException {

	public CredencialesIncorrectasException() {
		super("Usuario o contraseña incorrectos");
	}
}

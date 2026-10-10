package com.horneo.backend.exception;

public class AccesoDenegadoException extends RuntimeException {

	public AccesoDenegadoException() {
		super("Esta operación solo la puede hacer un administrador");
	}
}

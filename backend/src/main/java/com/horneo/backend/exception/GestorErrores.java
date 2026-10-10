package com.horneo.backend.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GestorErrores {

	@ExceptionHandler(RecursoNoEncontradoException.class)
	public ProblemDetail recursoNoEncontrado(RecursoNoEncontradoException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(ReglaNegocioException.class)
	public ProblemDetail reglaNegocio(ReglaNegocioException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	@ExceptionHandler(CredencialesIncorrectasException.class)
	public ProblemDetail credencialesIncorrectas(CredencialesIncorrectasException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
	}

	@ExceptionHandler(NoAutenticadoException.class)
	public ProblemDetail noAutenticado(NoAutenticadoException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
	}

	@ExceptionHandler(AccesoDenegadoException.class)
	public ProblemDetail accesoDenegado(AccesoDenegadoException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail datosNoValidos(MethodArgumentNotValidException e) {
		Map<String, String> errores = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors()
				.forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Hay datos no válidos");
		problema.setProperty("errores", errores);
		return problema;
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail parametroNoValido(MethodArgumentTypeMismatchException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"El valor '" + e.getValue() + "' no es válido para '" + e.getName() + "'");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail integridadDatos(DataIntegrityViolationException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"La operación no es compatible con los datos guardados");
	}
}

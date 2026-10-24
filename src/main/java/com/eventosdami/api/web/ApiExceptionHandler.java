package com.eventosdami.api.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.eventosdami.api.error.RecursoNoEncontradoException;
import com.eventosdami.api.error.StockInsuficienteException;

/**
 * Traduce las excepciones de negocio a respuestas HTTP (RFC 9457).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(RecursoNoEncontradoException.class)
	public ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
		return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), "/errores/not-found");
	}

	@ExceptionHandler(StockInsuficienteException.class)
	public ProblemDetail stockInsuficiente(StockInsuficienteException ex) {
		return problema(HttpStatus.CONFLICT, "Entradas insuficientes", ex.getMessage(), "/errores/stock-insuficiente");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail datosInvalidos(MethodArgumentNotValidException ex) {
		ProblemDetail detalle = problema(HttpStatus.BAD_REQUEST, "Datos inválidos",
				"La petición contiene campos inválidos", "/errores/validacion");

		Map<String, String> errores = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));

		detalle.setProperty("errores", errores);
		return detalle;
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
		return problema(HttpStatus.BAD_REQUEST, "Solicitud inválida", ex.getMessage(), "/errores/solicitud-invalida");
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail errorInesperado(Exception ex) {
		log.error("Error inesperado atendiendo una petición", ex);
		return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
				"Ocurrió un error inesperado", "/errores/interno");
	}

	private ProblemDetail problema(HttpStatus status, String titulo, String detalle, String tipo) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
		problema.setTitle(titulo);
		problema.setType(URI.create(tipo));
		return problema;
	}

}

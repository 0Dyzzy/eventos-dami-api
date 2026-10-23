package com.eventosdami.api.error;

public class StockInsuficienteException extends RuntimeException {

	public StockInsuficienteException(String mensaje) {
		super(mensaje);
	}

}

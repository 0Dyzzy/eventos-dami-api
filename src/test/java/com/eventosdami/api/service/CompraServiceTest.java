package com.eventosdami.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventosdami.api.domain.Compra;
import com.eventosdami.api.domain.Evento;
import com.eventosdami.api.error.RecursoNoEncontradoException;
import com.eventosdami.api.error.StockInsuficienteException;
import com.eventosdami.api.repository.CompraRepository;
import com.eventosdami.api.repository.EventoRepository;
import com.eventosdami.api.web.dto.CompraRequest;
import com.eventosdami.api.web.dto.CompraResponse;

@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

	@Mock
	private EventoRepository eventoRepository;

	@Mock
	private CompraRepository compraRepository;

	@InjectMocks
	private CompraService compraService;

	@Test
	void registraLaCompraCalculaElTotalYDescuentaStock() {
		Evento evento = new Evento("Concierto de Rock", LocalDateTime.of(2030, 5, 20, 20, 0),
				new BigDecimal("15000.00"), 10);

		when(eventoRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(evento));
		when(compraRepository.save(any(Compra.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CompraResponse respuesta = compraService.comprar(new CompraRequest(1L, 3));

		assertThat(respuesta.evento().nombre()).isEqualTo("Concierto de Rock");
		assertThat(respuesta.cantidad()).isEqualTo(3);
		assertThat(respuesta.precioUnitario()).isEqualByComparingTo("15000.00");
		assertThat(respuesta.precioTotal()).isEqualByComparingTo("45000.00");

		assertThat(evento.getEntradasDisponibles()).isEqualTo(7);

		ArgumentCaptor<Compra> captor = ArgumentCaptor.forClass(Compra.class);
		verify(compraRepository).save(captor.capture());
		Compra guardada = captor.getValue();
		assertThat(guardada.getEvento()).isSameAs(evento);
		assertThat(guardada.getCantidad()).isEqualTo(3);
		assertThat(guardada.getPrecioUnitario()).isEqualByComparingTo("15000.00");
		assertThat(guardada.getPrecioTotal()).isEqualByComparingTo("45000.00");
		assertThat(guardada.getFechaCompra()).isNotNull();
	}

	@Test
	void rechazaLaCompraSiNoHayStockSuficiente() {
		Evento evento = new Evento("Concierto de Rock", LocalDateTime.of(2030, 5, 20, 20, 0),
				new BigDecimal("15000.00"), 1);

		when(eventoRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(evento));

		assertThatThrownBy(() -> compraService.comprar(new CompraRequest(1L, 5)))
				.isInstanceOf(StockInsuficienteException.class)
				.hasMessageContaining("solo tiene 1 entradas disponibles");

		assertThat(evento.getEntradasDisponibles()).isEqualTo(1);
		verify(compraRepository, never()).save(any(Compra.class));
	}

	@Test
	void rechazaLaCompraSiElEventoNoExiste() {
		when(eventoRepository.buscarParaActualizar(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> compraService.comprar(new CompraRequest(99L, 1)))
				.isInstanceOf(RecursoNoEncontradoException.class)
				.hasMessageContaining("No existe un evento con id 99");

		verify(compraRepository, never()).save(any(Compra.class));
	}

}

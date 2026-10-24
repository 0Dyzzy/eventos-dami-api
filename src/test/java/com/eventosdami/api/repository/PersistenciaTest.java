package com.eventosdami.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.eventosdami.api.domain.Compra;
import com.eventosdami.api.domain.Evento;

@DataJpaTest
class PersistenciaTest {

	@Autowired
	private EventoRepository eventoRepository;

	@Autowired
	private CompraRepository compraRepository;

	@Test
	void guardaYRecuperaEventos() {
		Evento guardado = eventoRepository.save(new Evento("Concierto de Rock",
				LocalDateTime.of(2030, 5, 20, 20, 0), new BigDecimal("15000.00"), 100));

		Evento recuperado = eventoRepository.findById(guardado.getId()).orElseThrow();

		assertThat(recuperado.getNombre()).isEqualTo("Concierto de Rock");
		assertThat(recuperado.getPrecio()).isEqualByComparingTo("15000.00");
		assertThat(recuperado.getEntradasDisponibles()).isEqualTo(100);
	}

	@Test
	void guardaCompraAsociadaAUnEvento() {
		Evento evento = eventoRepository.save(new Evento("Festival de Jazz",
				LocalDateTime.of(2030, 6, 1, 19, 30), new BigDecimal("25000.00"), 50));

		compraRepository.save(new Compra(evento, 2, new BigDecimal("25000.00"),
				new BigDecimal("50000.00"), Instant.now()));

		assertThat(compraRepository.findByEventoIdOrderByFechaCompraDesc(evento.getId()))
				.singleElement()
				.satisfies(compra -> {
					assertThat(compra.getEvento().getId()).isEqualTo(evento.getId());
					assertThat(compra.getCantidad()).isEqualTo(2);
					assertThat(compra.getPrecioTotal()).isEqualByComparingTo("50000.00");
				});
	}

	@Test
	void buscarParaActualizarDevuelveElEventoConLock() {
		Evento evento = eventoRepository.save(new Evento("Obra de Teatro",
				LocalDateTime.of(2030, 7, 10, 18, 0), new BigDecimal("8000.00"), 30));

		assertThat(eventoRepository.buscarParaActualizar(evento.getId()))
				.get()
				.extracting(Evento::getNombre)
				.isEqualTo("Obra de Teatro");
	}

}

package com.eventosdami.api.resumen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import com.eventosdami.api.web.dto.CompraResponse;
import com.eventosdami.api.web.dto.EventoResponse;

class GeneradorResumenCompraTest {

	private final GeneradorResumenCompra generador = new GeneradorResumenCompra();

	@Test
	void generaUnPdfConLosDatosDeLaCompra() throws IOException {
		CompraResponse compra = new CompraResponse(
				10L,
				new EventoResponse(1L, "Concierto de Rock", LocalDateTime.of(2030, 5, 20, 20, 0),
						new BigDecimal("15000.00"), 97),
				3,
				new BigDecimal("15000.00"),
				new BigDecimal("45000.00"),
				Instant.parse("2030-01-01T15:00:00Z"));

		ResumenCompra resumen = generador.generar(compra);

		assertThat(resumen.nombreArchivo()).isEqualTo("resumen-compra-10.pdf");
		assertThat(resumen.tipoContenido()).isEqualTo(ClaveResumen.TIPO_CONTENIDO);
		assertThat(resumen.contenido()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));

		try (PdfReader lector = new PdfReader(resumen.contenido())) {
			String texto = new PdfTextExtractor(lector).getTextFromPage(1);

			assertThat(texto)
					.contains("Resumen de compra")
					.contains("Concierto de Rock")
					.contains("Cantidad de entradas")
					.contains("45.000,00");
		}
	}

	@Test
	void usaElNumeroDeLaCompraComoNombreDeArchivo() {
		CompraResponse compra = new CompraResponse(
				42L,
				new EventoResponse(1L, "Obra de Teatro", LocalDateTime.of(2030, 7, 10, 18, 0),
						new BigDecimal("8000.00"), 30),
				1,
				new BigDecimal("8000.00"),
				new BigDecimal("8000.00"),
				Instant.parse("2030-01-01T15:00:00Z"));

		assertThat(generador.generar(compra).nombreArchivo()).isEqualTo("resumen-compra-42.pdf");
	}

}

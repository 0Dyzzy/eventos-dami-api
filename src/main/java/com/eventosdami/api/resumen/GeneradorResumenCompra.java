package com.eventosdami.api.resumen;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import com.eventosdami.api.web.dto.CompraResponse;

/**
 * Genera el resumen de compra como un archivo PDF en memoria, para que el
 * cliente pueda descargarlo y guardarlo en su computador.
 */
@Component
public class GeneradorResumenCompra {

	private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Santiago");

	private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

	private static final DecimalFormat MONEDA = new DecimalFormat("#,##0.00",
			DecimalFormatSymbols.getInstance(Locale.of("es", "CL")));

	private static final Font TITULO = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(0x1F, 0x2A, 0x44));

	private static final Font SUBTITULO = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(0x6B, 0x72, 0x80));

	private static final Font ETIQUETA = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(0x1F, 0x2A, 0x44));

	private static final Font VALOR = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(0x11, 0x18, 0x27));

	private static final Font TOTAL = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(0x11, 0x18, 0x27));

	private static final Font PIE = new Font(Font.HELVETICA, 9, Font.ITALIC, new Color(0x6B, 0x72, 0x80));

	public ResumenCompra generar(CompraResponse compra) {
		Document documento = new Document(PageSize.A4, 48, 48, 48, 48);
		ByteArrayOutputStream salida = new ByteArrayOutputStream();

		try {
			PdfWriter.getInstance(documento, salida);
			documento.open();
			agregarContenido(documento, compra);
		}
		catch (DocumentException ex) {
			throw new IllegalStateException("No se pudo generar el resumen de la compra " + compra.id(), ex);
		}
		finally {
			documento.close();
		}

		return new ResumenCompra(ClaveResumen.nombreArchivo(compra.id()), ClaveResumen.TIPO_CONTENIDO,
				salida.toByteArray());
	}

	private void agregarContenido(Document documento, CompraResponse compra) throws DocumentException {
		Paragraph titulo = new Paragraph("Resumen de compra", TITULO);
		titulo.setAlignment(Element.ALIGN_CENTER);
		documento.add(titulo);

		Paragraph subtitulo = new Paragraph("Eventos Dami - comprobante N° " + compra.id(), SUBTITULO);
		subtitulo.setAlignment(Element.ALIGN_CENTER);
		subtitulo.setSpacingAfter(20f);
		documento.add(subtitulo);

		PdfPTable tabla = new PdfPTable(2);
		tabla.setWidthPercentage(100);
		tabla.setWidths(new float[] { 2f, 3f });
		tabla.setSpacingAfter(24f);

		agregarFila(tabla, "N° de resumen", String.valueOf(compra.id()), false);
		agregarFila(tabla, "Evento", compra.evento().nombre(), false);
		agregarFila(tabla, "Fecha del evento", formatear(compra.evento().fecha()), false);
		agregarFila(tabla, "Fecha de la compra", formatear(compra.fechaCompra()), false);
		agregarFila(tabla, "Cantidad de entradas", String.valueOf(compra.cantidad()), false);
		agregarFila(tabla, "Precio unitario", formatearMoneda(compra.precioUnitario()), false);
		agregarFila(tabla, "Total a pagar", formatearMoneda(compra.precioTotal()), true);

		documento.add(tabla);

		Paragraph pie = new Paragraph(
				"Documento generado automáticamente por Eventos Dami. Conserve este comprobante como respaldo de su compra.",
				PIE);
		pie.setAlignment(Element.ALIGN_CENTER);
		documento.add(pie);
	}

	private void agregarFila(PdfPTable tabla, String etiqueta, String valor, boolean destacada) {
		PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, ETIQUETA));
		PdfPCell celdaValor = new PdfPCell(new Phrase(valor, destacada ? TOTAL : VALOR));

		for (PdfPCell celda : new PdfPCell[] { celdaEtiqueta, celdaValor }) {
			celda.setBorder(Rectangle.BOTTOM);
			celda.setBorderColor(new Color(0xD1, 0xD5, 0xDB));
			celda.setPaddingTop(9f);
			celda.setPaddingBottom(9f);
			celda.setPaddingLeft(4f);
			celda.setPaddingRight(4f);
		}

		tabla.addCell(celdaEtiqueta);
		tabla.addCell(celdaValor);
	}

	private String formatear(java.time.Instant instante) {
		return FECHA_HORA.format(instante.atZone(ZONA_HORARIA));
	}

	private String formatear(java.time.LocalDateTime fecha) {
		return FECHA_HORA.format(fecha);
	}

	private String formatearMoneda(BigDecimal monto) {
		return "$ " + MONEDA.format(monto);
	}

}

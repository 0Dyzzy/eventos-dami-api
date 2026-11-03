package com.eventosdami.api.web;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.eventosdami.api.almacenamiento.ResumenAlmacenado;
import com.eventosdami.api.error.ArchivoResumenInvalidoException;
import com.eventosdami.api.resumen.ClaveResumen;
import com.eventosdami.api.resumen.ResumenCompra;
import com.eventosdami.api.service.ResumenCompraService;
import com.eventosdami.api.web.dto.ResumenAlmacenadoResponse;

/**
 * Administra el archivo de resumen de una compra.
 *
 * <ul>
 * <li>{@code GET /api/compras/{id}/resumen}: genera el PDF y lo entrega como
 * archivo descargable (se puede guardar en el computador).</li>
 * <li>{@code POST /api/compras/{id}/resumen}: sube el resumen a S3.</li>
 * <li>{@code GET /api/compras/{id}/resumen/s3}: descarga el resumen desde S3.</li>
 * <li>{@code PUT /api/compras/{id}/resumen}: reemplaza el resumen en S3.</li>
 * <li>{@code DELETE /api/compras/{id}/resumen}: elimina el resumen de S3.</li>
 * <li>{@code GET /api/compras/{id}/resumen/info}: metadatos del resumen
 * guardado.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/compras/{id}/resumen")
public class ResumenCompraController {

	private final ResumenCompraService resumenService;

	public ResumenCompraController(ResumenCompraService resumenService) {
		this.resumenService = resumenService;
	}

	/** Genera el resumen de la compra y lo descarga como archivo PDF. */
	@GetMapping(produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> generar(@PathVariable Long id) {
		return archivoPdf(resumenService.generar(id));
	}

	/**
	 * Sube el resumen de la compra al bucket de S3. Acepta el archivo en el
	 * campo multipart {@code archivo}; si no se envía, el PDF se genera a
	 * partir de la compra registrada.
	 */
	@PostMapping
	public ResponseEntity<ResumenAlmacenadoResponse> subir(@PathVariable Long id,
			@RequestParam(value = "archivo", required = false) MultipartFile archivo) {

		ResumenAlmacenado resumen = resumenService.subir(id, leer(archivo));

		return ResponseEntity
				.created(URI.create("/api/compras/" + id + "/resumen/s3"))
				.body(ResumenAlmacenadoResponse.desde(id, resumen));
	}

	/**
	 * Reemplaza el resumen guardado en S3, por ejemplo para corregir un error.
	 * Acepta el archivo en el campo multipart {@code archivo}; si no se envía,
	 * se regenera el PDF desde la compra.
	 */
	@PutMapping
	public ResumenAlmacenadoResponse reemplazar(@PathVariable Long id,
			@RequestParam(value = "archivo", required = false) MultipartFile archivo) {

		return ResumenAlmacenadoResponse.desde(id, resumenService.reemplazar(id, leer(archivo)));
	}

	/** Descarga el resumen guardado en S3. */
	@GetMapping(path = "/s3", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
		byte[] contenido = resumenService.descargar(id);

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, adjunto(ClaveResumen.nombreArchivo(id)))
				.contentLength(contenido.length)
				.body(contenido);
	}

	/** Elimina el resumen de S3. */
	@DeleteMapping
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		resumenService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

	/** Metadatos del resumen guardado (bucket, carpeta, clave, tamaño, ETag y URL). */
	@GetMapping(path = "/info")
	public ResumenAlmacenadoResponse info(@PathVariable Long id) {
		return ResumenAlmacenadoResponse.desde(id, resumenService.obtener(id));
	}

	private ResponseEntity<byte[]> archivoPdf(ResumenCompra resumen) {
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, adjunto(resumen.nombreArchivo()))
				.contentLength(resumen.tamanio())
				.body(resumen.contenido());
	}

	private String adjunto(String nombreArchivo) {
		return ContentDisposition.attachment()
				.filename(nombreArchivo, StandardCharsets.UTF_8)
				.build()
				.toString();
	}

	private byte[] leer(MultipartFile archivo) {
		if (archivo == null) {
			return null;
		}
		try {
			return archivo.getBytes();
		}
		catch (IOException ex) {
			throw new ArchivoResumenInvalidoException("No se pudo leer el archivo enviado", ex);
		}
	}

}

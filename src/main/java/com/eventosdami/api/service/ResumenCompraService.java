package com.eventosdami.api.service;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;

import com.eventosdami.api.almacenamiento.AlmacenResumenes;
import com.eventosdami.api.almacenamiento.ResumenAlmacenado;
import com.eventosdami.api.error.ArchivoResumenInvalidoException;
import com.eventosdami.api.resumen.GeneradorResumenCompra;
import com.eventosdami.api.resumen.ResumenCompra;
import com.eventosdami.api.web.dto.CompraResponse;

/**
 * Orquesta la generación del resumen de compra (semana 1) y su administración
 * en el almacenamiento: subir, descargar, reemplazar y eliminar.
 */
@Service
public class ResumenCompraService {

	private static final Logger log = LoggerFactory.getLogger(ResumenCompraService.class);

	private static final byte[] FIRMA_PDF = "%PDF".getBytes(StandardCharsets.US_ASCII);

	private final CompraService compraService;

	private final GeneradorResumenCompra generador;

	private final AlmacenResumenes almacen;

	private final DataSize tamanoMaximo;

	public ResumenCompraService(CompraService compraService, GeneradorResumenCompra generador,
			AlmacenResumenes almacen, @Value("${app.resumen.tamano-maximo:10MB}") DataSize tamanoMaximo) {
		this.compraService = compraService;
		this.generador = generador;
		this.almacen = almacen;
		this.tamanoMaximo = tamanoMaximo;
	}

	/**
	 * Genera el PDF del resumen para que el cliente lo guarde en su computador.
	 */
	@Transactional(readOnly = true)
	public ResumenCompra generar(Long compraId) {
		CompraResponse compra = compraService.obtener(compraId);
		return generador.generar(compra);
	}

	/**
	 * Sube el resumen de la compra al almacenamiento. Si no se recibe un
	 * archivo, se genera el PDF a partir de la compra registrada.
	 */
	public ResumenAlmacenado subir(Long compraId, byte[] archivo) {
		CompraResponse compra = compraService.obtener(compraId);
		byte[] contenido = archivo == null ? generador.generar(compra).contenido() : validar(archivo);

		ResumenAlmacenado almacenado = almacen.subir(compraId, contenido);
		log.info("Resumen de la compra {} guardado en {}/{}", compraId, almacenado.bucket(), almacenado.clave());
		return almacenado;
	}

	/**
	 * Reemplaza el resumen ya guardado (por ejemplo, si se corrigió un error).
	 * Si no se recibe un archivo, se vuelve a generar el PDF desde la compra.
	 */
	public ResumenAlmacenado reemplazar(Long compraId, byte[] archivo) {
		CompraResponse compra = compraService.obtener(compraId);
		byte[] contenido = archivo == null ? generador.generar(compra).contenido() : validar(archivo);

		ResumenAlmacenado almacenado = almacen.reemplazar(compraId, contenido);
		log.info("Resumen de la compra {} reemplazado en {}/{}", compraId, almacenado.bucket(), almacenado.clave());
		return almacenado;
	}

	/** Metadatos del resumen guardado. */
	public ResumenAlmacenado obtener(Long compraId) {
		compraService.obtener(compraId);
		return almacen.obtener(compraId);
	}

	/** Contenido del resumen guardado. */
	public byte[] descargar(Long compraId) {
		compraService.obtener(compraId);
		return almacen.descargar(compraId);
	}

	/** Elimina el resumen guardado. */
	public void eliminar(Long compraId) {
		compraService.obtener(compraId);
		almacen.eliminar(compraId);
		log.info("Resumen de la compra {} eliminado del almacenamiento", compraId);
	}

	private byte[] validar(byte[] archivo) {
		if (archivo.length == 0) {
			throw new ArchivoResumenInvalidoException("El archivo del resumen está vacío");
		}
		if (archivo.length > tamanoMaximo.toBytes()) {
			throw new ArchivoResumenInvalidoException(
					"El archivo del resumen supera el tamaño máximo de " + tamanoMaximo.toMegabytes() + " MB");
		}
		if (!esPdf(archivo)) {
			throw new ArchivoResumenInvalidoException("El archivo del resumen debe ser un PDF");
		}
		return archivo;
	}

	private boolean esPdf(byte[] contenido) {
		if (contenido.length < FIRMA_PDF.length) {
			return false;
		}
		for (int i = 0; i < FIRMA_PDF.length; i++) {
			if (contenido[i] != FIRMA_PDF[i]) {
				return false;
			}
		}
		return true;
	}

}

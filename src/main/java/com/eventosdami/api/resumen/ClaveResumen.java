package com.eventosdami.api.resumen;

/**
 * Convenciones de nombres usadas para guardar los resúmenes de compra.
 * <p>
 * Cada resumen vive en una carpeta cuyo nombre es el número del resumen
 * (el id de la compra) y dentro se guarda un único PDF:
 *
 * <pre>
 * &lt;bucket&gt;/
 *   10/
 *     resumen-compra-10.pdf
 * </pre>
 */
public final class ClaveResumen {

	/** Tipo de contenido con el que se generan y almacenan los resúmenes. */
	public static final String TIPO_CONTENIDO = "application/pdf";

	/** Extensión de los archivos de resumen. */
	public static final String EXTENSION = ".pdf";

	private ClaveResumen() {
	}

	/** Carpeta del resumen: el número del resumen. */
	public static String carpeta(Long compraId) {
		return String.valueOf(compraId);
	}

	/** Nombre del archivo PDF del resumen. */
	public static String nombreArchivo(Long compraId) {
		return "resumen-compra-" + compraId + EXTENSION;
	}

	/** Clave completa dentro del bucket (o del disco): {@code <id>/resumen-compra-<id>.pdf}. */
	public static String clave(Long compraId) {
		return carpeta(compraId) + "/" + nombreArchivo(compraId);
	}

}

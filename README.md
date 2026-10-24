# API de Eventos y Compras

API REST en Spring Boot (Java 21) para publicar eventos y vender entradas.

## Modelo de datos

| Tabla | Campos |
| --- | --- |
| `eventos` | `id`, `nombre`, `fecha`, `precio`, `entradas_disponibles` |
| `compras` | `id`, `evento_id`, `cantidad`, `precio_unitario`, `precio_total`, `fecha_compra` |

`compras` guarda el precio unitario y total al momento de la compra, para que un
cambio posterior en el evento no altere los comprobantes ya emitidos. 

## Endpoints

| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET` | `/api/eventos` | Lista los eventos disponibles (nombre, fecha, precio y stock). |
| `POST` | `/api/eventos` | Crea un evento y lo persiste en la base de datos. |
| `GET` | `/api/eventos/{id}` | Obtiene un evento por id. |
| `POST` | `/api/compras` | Compra entradas de un evento y devuelve el resumen. |
| `GET` | `/api/compras/{id}` | Recupera una compra almacenada. |
| `GET` | `/health` | Health check para probes. |

package com.eventosdami.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eventosdami.api.domain.Evento;

import jakarta.persistence.LockModeType;

public interface EventoRepository extends JpaRepository<Evento, Long> {

	/**
	 * Obtiene el evento con bloqueo pesimista para evitar vender más entradas
	 * que las disponibles cuando hay compras concurrentes.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Evento e where e.id = :id")
	Optional<Evento> buscarParaActualizar(@Param("id") Long id);

}

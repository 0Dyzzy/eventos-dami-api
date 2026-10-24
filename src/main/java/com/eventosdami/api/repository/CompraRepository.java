package com.eventosdami.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eventosdami.api.domain.Compra;

public interface CompraRepository extends JpaRepository<Compra, Long> {

	List<Compra> findByEventoIdOrderByFechaCompraDesc(Long eventoId);

}

package com.horneo.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.horneo.backend.entity.DetalleVenta;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

	@EntityGraph(attributePaths = "producto")
	List<DetalleVenta> findByVentaIdOrderByIdAsc(Long ventaId);
}

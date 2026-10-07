package com.horneo.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.horneo.backend.entity.Venta;

public interface VentaRepository extends JpaRepository<Venta, Long> {

	@EntityGraph(attributePaths = "usuario")
	List<Venta> findTop100ByOrderByFechaDescIdDesc();

	@EntityGraph(attributePaths = "usuario")
	List<Venta> findByFechaBetweenOrderByFechaDescIdDesc(LocalDateTime desde, LocalDateTime hasta);
}

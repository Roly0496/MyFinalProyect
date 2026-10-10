package com.horneo.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.horneo.backend.entity.Produccion;

public interface ProduccionRepository extends JpaRepository<Produccion, Long> {

	@EntityGraph(attributePaths = { "producto", "usuario" })
	Optional<Produccion> findConDetalleById(Long id);

	@EntityGraph(attributePaths = { "producto", "usuario" })
	List<Produccion> findTop100ByOrderByFechaDescIdDesc();

	@EntityGraph(attributePaths = { "producto", "usuario" })
	List<Produccion> findByFechaBetweenOrderByFechaDescIdDesc(LocalDateTime desde, LocalDateTime hasta);
}

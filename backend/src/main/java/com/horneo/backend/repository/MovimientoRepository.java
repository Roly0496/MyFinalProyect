package com.horneo.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.horneo.backend.entity.Movimiento;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

	@EntityGraph(attributePaths = "usuario")
	List<Movimiento> findByMateriaPrimaIdOrderByFechaDescIdDesc(Long materiaPrimaId);

	@EntityGraph(attributePaths = "usuario")
	List<Movimiento> findByProductoIdOrderByFechaDescIdDesc(Long productoId);

	@EntityGraph(attributePaths = { "materiaPrima", "producto", "usuario" })
	List<Movimiento> findTop100ByOrderByFechaDescIdDesc();
}

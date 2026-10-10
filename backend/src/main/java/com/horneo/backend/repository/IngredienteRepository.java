package com.horneo.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.horneo.backend.entity.Ingrediente;

public interface IngredienteRepository extends JpaRepository<Ingrediente, Long> {

	@EntityGraph(attributePaths = "materiaPrima")
	List<Ingrediente> findByProductoId(Long productoId);

	@EntityGraph(attributePaths = "materiaPrima")
	Optional<Ingrediente> findConMateriaPrimaById(Long id);

	boolean existsByProductoIdAndMateriaPrimaId(Long productoId, Long materiaPrimaId);
}

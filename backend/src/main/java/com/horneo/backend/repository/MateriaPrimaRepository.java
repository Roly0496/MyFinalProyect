package com.horneo.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.horneo.backend.entity.MateriaPrima;

public interface MateriaPrimaRepository extends JpaRepository<MateriaPrima, Long> {

	Optional<MateriaPrima> findByNombre(String nombre);

	boolean existsByNombre(String nombre);

	List<MateriaPrima> findByActivoTrueOrderByNombreAsc();
}

package com.horneo.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.horneo.backend.entity.Producto;
import com.horneo.backend.entity.TipoProducto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

	Optional<Producto> findByNombre(String nombre);

	boolean existsByNombre(String nombre);

	List<Producto> findByActivoTrueOrderByNombreAsc();

	List<Producto> findByTipoAndActivoTrueOrderByNombreAsc(TipoProducto tipo);
}

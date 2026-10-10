package com.horneo.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.horneo.backend.entity.Rol;
import com.horneo.backend.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByUsername(String username);

	boolean existsByUsername(String username);

	List<Usuario> findAllByOrderByNombreAsc();

	long countByRolAndActivoTrue(Rol rol);
}

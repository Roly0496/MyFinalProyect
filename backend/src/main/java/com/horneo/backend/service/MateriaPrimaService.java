package com.horneo.backend.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.horneo.backend.entity.MateriaPrima;
import com.horneo.backend.entity.Unidad;
import com.horneo.backend.exception.RecursoNoEncontradoException;
import com.horneo.backend.exception.ReglaNegocioException;
import com.horneo.backend.repository.MateriaPrimaRepository;

@Service
@Transactional(readOnly = true)
public class MateriaPrimaService {

	private final MateriaPrimaRepository materiaPrimaRepository;

	public MateriaPrimaService(MateriaPrimaRepository materiaPrimaRepository) {
		this.materiaPrimaRepository = materiaPrimaRepository;
	}

	public List<MateriaPrima> listarActivas() {
		return materiaPrimaRepository.findByActivoTrueOrderByNombreAsc();
	}

	public MateriaPrima obtener(Long id) {
		return materiaPrimaRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la materia prima con id " + id));
	}

	@Transactional
	public MateriaPrima crear(String nombre, Unidad unidad, BigDecimal stockMinimo, BigDecimal precioUnitario) {
		String nombreLimpio = nombre.trim();
		if (materiaPrimaRepository.existsByNombre(nombreLimpio)) {
			throw new ReglaNegocioException("Ya existe una materia prima llamada " + nombreLimpio);
		}
		MateriaPrima materiaPrima = new MateriaPrima(nombreLimpio, unidad);
		materiaPrima.setStockMinimo(stockMinimo);
		materiaPrima.setPrecioUnitario(precioUnitario);
		return materiaPrimaRepository.save(materiaPrima);
	}

	@Transactional
	public MateriaPrima actualizar(Long id, String nombre, BigDecimal stockMinimo, BigDecimal precioUnitario) {
		MateriaPrima materiaPrima = obtener(id);
		String nombreLimpio = nombre.trim();
		boolean nombreUsadoPorOtra = materiaPrimaRepository.findByNombre(nombreLimpio)
				.filter(otra -> !otra.getId().equals(id))
				.isPresent();
		if (nombreUsadoPorOtra) {
			throw new ReglaNegocioException("Ya existe una materia prima llamada " + nombreLimpio);
		}
		materiaPrima.setNombre(nombreLimpio);
		materiaPrima.setStockMinimo(stockMinimo);
		materiaPrima.setPrecioUnitario(precioUnitario);
		return materiaPrima;
	}

	@Transactional
	public void desactivar(Long id) {
		MateriaPrima materiaPrima = obtener(id);
		materiaPrima.setActivo(false);
	}
}

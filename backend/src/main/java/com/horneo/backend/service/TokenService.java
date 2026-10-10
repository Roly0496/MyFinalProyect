package com.horneo.backend.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.horneo.backend.entity.Usuario;
import com.horneo.backend.exception.NoAutenticadoException;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class TokenService {

	private final SecretKey clave;
	private final Duration duracion;

	public TokenService(@Value("${horneo.jwt.secreto}") String secreto,
			@Value("${horneo.jwt.duracion-horas:12}") long duracionHoras) {
		this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
		this.duracion = Duration.ofHours(duracionHoras);
	}

	public String generar(Usuario usuario) {
		Instant ahora = Instant.now();
		return Jwts.builder()
				.subject(usuario.getId().toString())
				.claim("rol", usuario.getRol().name())
				.issuedAt(Date.from(ahora))
				.expiration(Date.from(ahora.plus(duracion)))
				.signWith(clave)
				.compact();
	}

	public Long obtenerUsuarioId(String token) {
		try {
			String subject = Jwts.parser()
					.verifyWith(clave)
					.build()
					.parseSignedClaims(token)
					.getPayload()
					.getSubject();
			return Long.valueOf(subject);
		} catch (JwtException | IllegalArgumentException e) {
			throw new NoAutenticadoException("La sesión no es válida o ha caducado");
		}
	}
}

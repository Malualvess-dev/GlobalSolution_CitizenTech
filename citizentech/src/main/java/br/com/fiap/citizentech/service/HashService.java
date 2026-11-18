package br.com.fiap.citizentech.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HashService {

	private static final String ALGORITMO = "HmacSHA256";

	private final SecretKeySpec chave;

	public HashService(@Value("${citizentech.hash.secret}") String segredo) {
		this.chave = new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), ALGORITMO);
	}

	public String hmacSha256(String valor) {
		try {
			Mac mac = Mac.getInstance(ALGORITMO);
			mac.init(chave);
			byte[] hash = mac.doFinal(valor.trim().getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Nao foi possivel gerar o hash HMAC-SHA256", e);
		}
	}

}

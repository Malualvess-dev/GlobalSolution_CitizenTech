package br.com.fiap.citizentech.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.com.fiap.citizentech.dto.response.ErroResponse;
import br.com.fiap.citizentech.dto.response.ErroResponse.CampoInvalido;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
		return responder(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(ConflitoException.class)
	public ResponseEntity<ErroResponse> conflito(ConflitoException ex, HttpServletRequest request) {
		return responder(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(RegraNegocioException.class)
	public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex, HttpServletRequest request) {
		return responder(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
				.map(erro -> new CampoInvalido(erro.getField(), erro.getDefaultMessage()))
				.toList();
		return responder(HttpStatus.BAD_REQUEST, "Dados invalidos na requisicao", request, campos);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
		return responder(HttpStatus.BAD_REQUEST,
				"Corpo da requisicao invalido: confira o JSON e os valores dos campos", request, List.of());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException ex,
			HttpServletRequest request) {
		return responder(HttpStatus.BAD_REQUEST,
				"Valor invalido para o parametro '" + ex.getName() + "'", request, List.of());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException ex, HttpServletRequest request) {
		log.warn("Violacao de integridade em {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
		return responder(HttpStatus.CONFLICT,
				"Operacao viola uma regra do banco de dados (registro duplicado ou em uso por outro cadastro)",
				request, List.of());
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErroResponse> credenciaisInvalidas(BadCredentialsException ex, HttpServletRequest request) {
		return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErroResponse> naoAutenticado(AuthenticationException ex, HttpServletRequest request) {
		return responder(HttpStatus.UNAUTHORIZED,
				"Autenticacao necessaria: envie um token JWT valido no header Authorization", request, List.of());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException ex, HttpServletRequest request) {
		return responder(HttpStatus.FORBIDDEN, "Voce nao tem permissao para acessar este recurso", request, List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErroResponse> erroInesperado(Exception ex, HttpServletRequest request) {
		log.error("Erro inesperado em {}", request.getRequestURI(), ex);
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor", request, List.of());
	}

	private ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem, HttpServletRequest request,
			List<CampoInvalido> campos) {
		ErroResponse corpo = new ErroResponse(
				LocalDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				mensagem,
				request.getRequestURI(),
				campos);
		return ResponseEntity.status(status).body(corpo);
	}

}

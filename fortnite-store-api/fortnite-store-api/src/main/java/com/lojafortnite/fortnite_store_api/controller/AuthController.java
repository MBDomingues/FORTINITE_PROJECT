package com.lojafortnite.fortnite_store_api.controller;

import com.lojafortnite.fortnite_store_api.dto.LoginRequest;
import com.lojafortnite.fortnite_store_api.dto.LoginResponse;
import com.lojafortnite.fortnite_store_api.dto.UsuarioCadastroRequest;
import com.lojafortnite.fortnite_store_api.entity.Usuario;
import com.lojafortnite.fortnite_store_api.service.TokenService;
import com.lojafortnite.fortnite_store_api.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para Autenticação.
 * Gerencia as rotas públicas de cadastro e login.
 * Endpoint Base: /api/v1/auth
 */

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;   // Lógica de negócio de usuários (salvar no banco)

    // O Gerente de Autenticação do Spring Security (configurado no SecurityConfig)
    @Autowired private AuthenticationManager authenticationManager;

    // Serviço para criar o Token JWT após o login
    @Autowired
    private TokenService tokenService;

    /**
     * Rota para registrar um novo usuário.
     * Metodo: POST
     * URL: /api/v1/auth/cadastro
     */
    @PostMapping("/cadastro")
    public ResponseEntity<String> cadastrar(@RequestBody UsuarioCadastroRequest dto) {
        try {
            usuarioService.cadastrarUsuario(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body("Usuário cadastrado com sucesso!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Rota para realizar Login e obter o Token.
     * Método: POST
     * URL: /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest dto) {
        try {
            // 1. Cria o "pacote" de autenticação com os dados (ainda não validados)
            var authenticationToken = new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getSenha());

            // 2. O AuthenticationManager (do SecurityConfig) valida a senha
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // 3. Se chegou aqui, o login é válido. Pegamos os dados do usuário.
            Usuario usuarioAutenticado = (Usuario) authentication.getPrincipal();

            // 4. Geramos o Token JWT
            String token = tokenService.gerarToken(usuarioAutenticado);

            // 5. Retornamos o token para o cliente
            return ResponseEntity.ok(new LoginResponse(token));

        } catch (Exception e) {
            // Se a autenticação falhar (senha errada, etc.)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
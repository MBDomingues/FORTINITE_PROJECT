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



@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;   

    
    @Autowired private AuthenticationManager authenticationManager;

    
    @Autowired
    private TokenService tokenService;

    
    @PostMapping("/cadastro")
    public ResponseEntity<String> cadastrar(@RequestBody UsuarioCadastroRequest dto) {
        try {
            usuarioService.cadastrarUsuario(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body("Usuário cadastrado com sucesso!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest dto) {
        try {
            
            var authenticationToken = new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getSenha());

            
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            
            Usuario usuarioAutenticado = (Usuario) authentication.getPrincipal();

            
            String token = tokenService.gerarToken(usuarioAutenticado);

            
            return ResponseEntity.ok(new LoginResponse(token));

        } catch (Exception e) {
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
package com.lojafortnite.fortnite_store_api.config;

import com.lojafortnite.fortnite_store_api.repository.UsuarioRepository;
import com.lojafortnite.fortnite_store_api.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;



@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService; 

    @Autowired
    private UsuarioRepository usuarioRepository; 


    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class);


    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.info("Filtro JWT: Processando requisição para: {}", request.getRequestURI());

        
        String token = extrairToken(request);

        if (token != null) {
            log.info("1. Filtro JWT: Token extraído com sucesso.");
            String emailUsuario = null;

            try {
                
                emailUsuario = tokenService.getSubject(token);
            } catch (Exception e) {
                log.warn("Filtro JWT: Falha ao processar o token (Expirado ou Inválido): {}", e.getMessage());
            }
            
            if (emailUsuario != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.info("2. Filtro JWT: Email do subject: {}", emailUsuario);

                
                UserDetails usuario = usuarioRepository.findByEmail(emailUsuario).orElse(null);

                if (usuario != null) {
                    log.info("3. Filtro JWT: Usuário encontrado no banco: {}", usuario.getUsername());

                    
                    var authentication = new UsernamePasswordAuthenticationToken(
                            usuario, null, usuario.getAuthorities());

                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("4. Filtro JWT: Usuário autenticado e salvo no SecurityContext.");
                } else {
                    log.warn("Filtro JWT: Usuário com email {} não encontrado no banco de dados.", emailUsuario);
                }
            } else if (emailUsuario == null) {
                log.warn("Filtro JWT: Email (subject) no token é nulo ou token inválido.");
            }

        } else {
            log.warn("Filtro JWT: Token não encontrado no cabeçalho 'Authorization'. Rota será tratada como anônima.");
        }

        
        filterChain.doFilter(request, response);
    }

    
    private String extrairToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}
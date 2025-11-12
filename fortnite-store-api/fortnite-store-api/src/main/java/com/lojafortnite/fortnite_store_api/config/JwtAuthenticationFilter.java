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


/**
 * Filtro de Autenticação JWT.
 * Intercepta todas as requisições HTTP para verificar se existe um token válido.
 * Se o token for válido, ele autentica o usuário no contexto do Spring Security.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService; // Serviço responsável por validar e ler o Token JWT

    @Autowired
    private UsuarioRepository usuarioRepository; // Acesso ao banco de dados para buscar o usuário


    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class);


    /**
     * Metodo principal do filtro. É executado uma vez a cada requisição.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.info("Filtro JWT: Processando requisição para: {}", request.getRequestURI());

        // 1. Tenta extrair o token do cabeçalho "Authorization"
        String token = extrairToken(request);

        if (token != null) {
            log.info("1. Filtro JWT: Token extraído com sucesso.");
            String emailUsuario = null;

            try {
                // 2. Valida o token e recupera o email (Subject) contido nele
                emailUsuario = tokenService.getSubject(token);
            } catch (Exception e) {
                log.warn("Filtro JWT: Falha ao processar o token (Expirado ou Inválido): {}", e.getMessage());
            }
            // 3. Se o token é válido e o usuário ainda não está autenticado no contexto atual
            if (emailUsuario != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.info("2. Filtro JWT: Email do subject: {}", emailUsuario);

                // 4. Busca o usuário completo no banco de dados
                UserDetails usuario = usuarioRepository.findByEmail(emailUsuario).orElse(null);

                if (usuario != null) {
                    log.info("3. Filtro JWT: Usuário encontrado no banco: {}", usuario.getUsername());

                    // 5. Cria o objeto de autenticação do Spring Security (com as permissões/roles)
                    var authentication = new UsernamePasswordAuthenticationToken(
                            usuario, null, usuario.getAuthorities());

                    // 6. Salva a autenticação no contexto (O usuário está "logado" para esta requisição)
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

        // 7. Passa a requisição para o próximo filtro ou para o Controller
        filterChain.doFilter(request, response);
    }

    /**
     * Metodo auxiliar para pegar o token do Header.
     * Remove o prefixo "Bearer " e retorna apenas a string do token.
     */
    private String extrairToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}
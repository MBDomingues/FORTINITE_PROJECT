package com.lojafortnite.fortnite_store_api.config;

import com.lojafortnite.fortnite_store_api.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


import java.util.Arrays;


/**
 * Classe de Configuração Principal do Spring Security.
 * Define as regras de acesso (quem pode ver o quê), filtros e configurações de sessão.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Serviço para buscar usuários no banco de dados durante o login
    @Autowired
    private AuthenticationService authenticationService;

    // filtro customizado que valida o Token JWT
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // Utilitário para criptografar e verificar senhas (Hash)
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Configura a corrente de filtros de segurança (Security Filter Chain).
     * É aqui que definimos as regras de HTTP.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desabilita CSRF (proteção contra ataques de sessão), pois usamos JWT (Stateless)
                .csrf(csrf -> csrf.disable())

                // Define que a API não guardará estado (sessão) do usuário no servidor
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Ativa as configurações de CORS definidas mais abaixo
                .cors(c -> c.configurationSource(corsConfigurationSource()))

                // --- Definição de Autorizações (Rotas) ---
                .authorizeHttpRequests(authorize -> authorize

                        // --- 1. ROTAS PÚBLICAS (permitAll) ---
                        // Autenticação (Login/Cadastro)
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()

                        // Cosméticos (Listagem pública)
                        .requestMatchers(HttpMethod.GET, "/api/v1/cosmeticos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/cosmeticos/**").permitAll()

                        // Perfis (Listagem pública e busca por ID pública)
                        .requestMatchers(HttpMethod.GET, "/api/v1/perfis").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/perfis/{id:[0-9+]}").permitAll()

                        // 2. Rotas Fechadas (Privadas)
                        // Qualquer outra rota não listada acima exige autenticação (Token JWT válido)
                        // Ex: Comprar, Devolver, Ver perfil "me"
                        .anyRequest().authenticated()
                )

                // Adicionar nosso filtro JWT
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Exponibiliza o "Gerente de Autenticação" para ser usado no AuthController (Login).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // Este Bean é necessário para autenticar o usuário no AuthController
        return config.getAuthenticationManager();
    }

    /**
     * Configuração Global de Autenticação.
     * Ensina ao Spring Security qual serviço usar para buscar usuários e qual encoder usar para senhas.
     */
    @Autowired
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(authenticationService)
                .passwordEncoder(passwordEncoder);
    }

    /**
     * Configuração de CORS (Cross-Origin Resource Sharing).
     * Define quem pode fazer requisições para esta API (neste caso, liberado para todos '*').
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Permite requisições do seu frontend (localhost ou qualquer lugar)
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
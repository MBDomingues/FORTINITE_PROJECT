package com.lojafortnite.fortnite_store_api.service;

import com.lojafortnite.fortnite_store_api.dto.HistoricoTransacaoDTO;
import com.lojafortnite.fortnite_store_api.dto.UsuarioCadastroRequest;
import com.lojafortnite.fortnite_store_api.dto.UsuarioPerfilResponseDTO;
import com.lojafortnite.fortnite_store_api.entity.HistoricoTransacao;
import com.lojafortnite.fortnite_store_api.entity.Usuario;
import com.lojafortnite.fortnite_store_api.repository.HistoricoTransacaoRepository;
import com.lojafortnite.fortnite_store_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HistoricoTransacaoRepository historicoRepository;

    @Transactional
    public Usuario cadastrarUsuario(UsuarioCadastroRequest dto) {

        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Este e-mail já está em uso.");
        }

        Usuario novoUsuario = new Usuario();
        novoUsuario.setEmail(dto.getEmail());
        novoUsuario.setSenha(passwordEncoder.encode(dto.getSenha()));

        novoUsuario.setNome(dto.getNome());

        return usuarioRepository.save(novoUsuario);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioPerfilResponseDTO> listarTodosPerfis(Pageable pageable) {
        Page<Usuario> usuariosPage = usuarioRepository.findAll(pageable);

        List<UsuarioPerfilResponseDTO> dtoList = usuariosPage.getContent().stream()
                .map(UsuarioPerfilResponseDTO::new)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, usuariosPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public UsuarioPerfilResponseDTO obterPerfilComItens(Long id) {
        Usuario usuario = usuarioRepository.findByIdWithItens(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        return new UsuarioPerfilResponseDTO(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioPerfilResponseDTO obterPerfilComHistorico(Long id) {
        Usuario usuario = usuarioRepository.findByIdWithItens(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        return new UsuarioPerfilResponseDTO(usuario);
    }

    @Transactional(readOnly = true)
    public Page<HistoricoTransacaoDTO> listarHistoricoDoUsuario(Long usuarioId, Pageable pageable) {

        // 1. Busca a página de transações (agora o 'historicoRepository' existe)
        Page<HistoricoTransacao> historicoPage = historicoRepository.findByUsuarioId(usuarioId, pageable);

        // 2. Converte a Page<Entidade> para Page<DTO>
        return historicoPage.map(HistoricoTransacaoDTO::new);
    }
}
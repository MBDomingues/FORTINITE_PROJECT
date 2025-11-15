package com.lojafortnite.fortnite_store_api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lojafortnite.fortnite_store_api.entity.*;
import com.lojafortnite.fortnite_store_api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompraService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CosmeticoRepository cosmeticoRepository;
    @Autowired private ItemAdquiridoRepository itemAdquiridoRepository;
    @Autowired private HistoricoTransacaoRepository historicoRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ERRO_NAO_ENCONTRADO = "Item/Usuário não encontrado.";
    private static final String ERRO_SALDO = "Saldo insuficiente para esta compra.";
    private static final String ERRO_JA_ADQUIRIDO = "Este cosmético já foi adquirido por este usuário.";

    @Transactional
    public void comprarCosmetico(Long usuarioId, String cosmeticoId) {
        // ... (A LÓGICA DE COMPRA PERMANECE A MESMA QUE JÁ FUNCIONA) ...
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException(ERRO_NAO_ENCONTRADO));

        Cosmetico cosmeticoPrincipal = cosmeticoRepository.findById(cosmeticoId)
                .orElseThrow(() -> new RuntimeException(ERRO_NAO_ENCONTRADO));

        if (itemAdquiridoRepository.existsByUsuarioIdAndCosmeticoId(usuarioId, cosmeticoId)) {
            throw new RuntimeException(ERRO_JA_ADQUIRIDO);
        }

        if (usuario.getCreditos() < cosmeticoPrincipal.getPreco()) {
            throw new RuntimeException(ERRO_SALDO);
        }

        usuario.setCreditos(usuario.getCreditos() - cosmeticoPrincipal.getPreco());
        usuarioRepository.save(usuario);

        adicionarItemAoUsuario(usuario, cosmeticoPrincipal);

        registrarHistorico(usuario, cosmeticoPrincipal, HistoricoTransacao.TipoTransacao.COMPRA, cosmeticoPrincipal.getPreco());

        // Lógica de adicionar filhos do Bundle na compra
        if (Boolean.TRUE.equals(cosmeticoPrincipal.getIsBundle())) {
            processarItensDoBundle(usuarioId, cosmeticoPrincipal, true);
        }
    }

    @Transactional
    public void devolverCosmetico(Long usuarioId, String cosmeticoId) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException(ERRO_NAO_ENCONTRADO));

        // 1. Encontrar o registro de posse do item principal (o que está sendo devolvido)
        ItemAdquirido itemAdquirido = itemAdquiridoRepository
                .findByUsuarioIdAndCosmeticoId(usuarioId, cosmeticoId)
                .orElseThrow(() -> new RuntimeException("Cosmético não possuído."));

        // 2. Recupera o objeto cosmético para saber preço e se é bundle
        Cosmetico cosmetico = itemAdquirido.getCosmetico();
        Integer valorEstorno = cosmetico.getPreco();

        // 3. Estornar Saldo
        usuario.setCreditos(usuario.getCreditos() + valorEstorno);
        usuarioRepository.save(usuario);

        // 4. Remover Posse do Item Principal
        itemAdquiridoRepository.delete(itemAdquirido);

        // 5. Registrar Histórico
        registrarHistorico(usuario, cosmetico, HistoricoTransacao.TipoTransacao.DEVOLUCAO, valorEstorno);

        // --- 6. NOVA LÓGICA: REMOVER ITENS DO BUNDLE ---
        if (Boolean.TRUE.equals(cosmetico.getIsBundle())) {
            // Passamos 'false' para indicar que é uma remoção
            processarItensDoBundle(usuarioId, cosmetico, false);
        }
    }

    private void processarItensDoBundle(Long usuarioId, Cosmetico bundle, boolean isCompra) {
        String jsonItens = bundle.getBundleItemsJson();

        if (jsonItens != null && !jsonItens.isEmpty()) {
            try {
                List<String> idsFilhos = objectMapper.readValue(jsonItens, new TypeReference<List<String>>() {});

                for (String idFilho : idsFilhos) {
                    if (isCompra) {
                        // --- LÓGICA DE ADICIONAR (COMPRA) ---
                        boolean jaTemFilho = itemAdquiridoRepository.existsByUsuarioIdAndCosmeticoId(usuarioId, idFilho);
                        if (!jaTemFilho) {
                            cosmeticoRepository.findById(idFilho).ifPresent(itemFilho -> {
                                // Precisamos do objeto Usuario completo para salvar
                                usuarioRepository.findById(usuarioId).ifPresent(u -> adicionarItemAoUsuario(u, itemFilho));
                            });
                        }
                    } else {
                        // --- LÓGICA DE REMOVER (DEVOLUÇÃO) ---
                        // Busca o registro de posse desse item filho específico para este usuário
                        itemAdquiridoRepository.findByUsuarioIdAndCosmeticoId(usuarioId, idFilho)
                                .ifPresent(itemFilhoAdquirido -> {
                                    // Deleta o item filho do inventário
                                    itemAdquiridoRepository.delete(itemFilhoAdquirido);
                                });
                    }
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar itens do bundle (Ação: " + (isCompra ? "Compra" : "Devolução") + "): " + e.getMessage());
                // Em produção, você pode decidir lançar uma exceção aqui para fazer rollback de tudo
            }
        }
    }

    private void adicionarItemAoUsuario(Usuario usuario, Cosmetico cosmetico) {
        ItemAdquirido item = new ItemAdquirido();
        item.setUsuario(usuario);
        item.setCosmetico(cosmetico);
        item.setUniqueUserCosmeticoKey(usuario.getId() + "-" + cosmetico.getId());
        itemAdquiridoRepository.save(item);
    }

    private void registrarHistorico(Usuario u, Cosmetico c, HistoricoTransacao.TipoTransacao tipo, Integer valor) {
        HistoricoTransacao h = new HistoricoTransacao();
        h.setUsuario(u);
        h.setCosmetico(c);
        h.setTipo(tipo);
        h.setValor(valor);
        h.setDataTransacao(LocalDateTime.now());
        historicoRepository.save(h);
    }
}
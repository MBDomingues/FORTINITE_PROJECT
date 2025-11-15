package com.lojafortnite.fortnite_store_api.service;

import com.fasterxml.jackson.core.type.TypeReference; // Importe isto
import com.fasterxml.jackson.databind.ObjectMapper; // Importe isto
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

    // Precisamos do ObjectMapper para ler o JSON dos itens do bundle
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ERRO_NAO_ENCONTRADO = "Item/Usuário não encontrado.";
    private static final String ERRO_SALDO = "Saldo insuficiente para esta compra.";
    private static final String ERRO_JA_ADQUIRIDO = "Este cosmético já foi adquirido por este usuário.";

    @Transactional
    public void comprarCosmetico(Long usuarioId, String cosmeticoId) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException(ERRO_NAO_ENCONTRADO));

        Cosmetico cosmeticoPrincipal = cosmeticoRepository.findById(cosmeticoId)
                .orElseThrow(() -> new RuntimeException(ERRO_NAO_ENCONTRADO));

        // 1. Verifica se o usuário JÁ TEM esse item específico
        if (itemAdquiridoRepository.existsByUsuarioIdAndCosmeticoId(usuarioId, cosmeticoId)) {
            throw new RuntimeException(ERRO_JA_ADQUIRIDO);
        }

        // 2. Checagem de Saldo
        if (usuario.getCreditos() < cosmeticoPrincipal.getPreco()) {
            throw new RuntimeException(ERRO_SALDO);
        }

        // 3. Processar Compra (Debitar Saldo)
        usuario.setCreditos(usuario.getCreditos() - cosmeticoPrincipal.getPreco());
        usuarioRepository.save(usuario); // Salva o novo saldo

        // 4. Registrar Posse do Item Principal (O que foi clicado na loja)
        adicionarItemAoUsuario(usuario, cosmeticoPrincipal);

        // 5. Registrar Histórico (Apenas 1 registro da transação financeira)
        registrarHistorico(usuario, cosmeticoPrincipal, HistoricoTransacao.TipoTransacao.COMPRA, cosmeticoPrincipal.getPreco());

        // 6. LÓGICA DE BUNDLE: Entregar os itens filhos "de brinde"
        // (Esta é a parte nova que faltava no seu código)
        if (Boolean.TRUE.equals(cosmeticoPrincipal.getIsBundle())) {
            String jsonItens = cosmeticoPrincipal.getBundleItemsJson();

            if (jsonItens != null && !jsonItens.isEmpty()) {
                try {
                    // Converte a String JSON do banco em uma Lista de IDs
                    List<String> idsFilhos = objectMapper.readValue(jsonItens, new TypeReference<List<String>>(){});

                    for (String idFilho : idsFilhos) {
                        // Verifica se o usuário já tem o item filho (pode ter comprado avulso antes)
                        boolean jaTemFilho = itemAdquiridoRepository.existsByUsuarioIdAndCosmeticoId(usuarioId, idFilho);

                        if (!jaTemFilho) {
                            // Busca o objeto do item filho no banco e adiciona
                            cosmeticoRepository.findById(idFilho).ifPresent(itemFilho -> {
                                adicionarItemAoUsuario(usuario, itemFilho);
                            });
                        }
                    }
                } catch (Exception e) {
                    // Logar erro, mas não travar a compra principal se possível
                    System.err.println("Erro ao entregar itens do bundle: " + e.getMessage());
                }
            }
        }
    }

    @Transactional
    public void devolverCosmetico(Long usuarioId, String cosmeticoId) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException(ERRO_NAO_ENCONTRADO));

        // 1. Encontrar o registro de posse
        ItemAdquirido itemAdquirido = itemAdquiridoRepository
                .findByUsuarioIdAndCosmeticoId(usuarioId, cosmeticoId)
                .orElseThrow(() -> new RuntimeException("Cosmético não possuído."));

        // 2. Dados para estorno
        Cosmetico cosmetico = itemAdquirido.getCosmetico();
        Integer valorEstorno = cosmetico.getPreco();

        // 3. Estornar Saldo
        usuario.setCreditos(usuario.getCreditos() + valorEstorno);
        usuarioRepository.save(usuario);

        // 4. Remover Posse
        itemAdquiridoRepository.delete(itemAdquirido);

        // 5. Registrar Histórico
        registrarHistorico(usuario, cosmetico, HistoricoTransacao.TipoTransacao.DEVOLUCAO, valorEstorno);

        // NOTA: A devolução de bundles é complexa.
        // Se o usuário devolver um bundle, deveríamos remover os itens filhos também?
        // Por simplicidade, neste código, removemos apenas o item principal.
        // Se quiser remover os filhos, teria que repetir a lógica de leitura do JSON aqui e deletar um por um.
    }

    // --- Métodos Auxiliares para limpar o código ---

    private void adicionarItemAoUsuario(Usuario usuario, Cosmetico cosmetico) {
        ItemAdquirido item = new ItemAdquirido();
        item.setUsuario(usuario);
        item.setCosmetico(cosmetico);
        // A chave única evita duplicidade no banco
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
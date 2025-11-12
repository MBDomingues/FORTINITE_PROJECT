package com.lojafortnite.fortnite_store_api.service;

import com.lojafortnite.fortnite_store_api.entity.*;
import com.lojafortnite.fortnite_store_api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompraService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CosmeticoRepository cosmeticoRepository;
    @Autowired private ItemAdquiridoRepository itemAdquiridoRepository;
    @Autowired private HistoricoTransacaoRepository historicoRepository;

    private static final String ERRO_NAO_ENCONTRADO = "Item/Usuário não encontrado.";
    private static final String ERRO_SALDO = "Saldo insuficiente para esta compra.";
    private static final String ERRO_JA_ADQUIRIDO = "Este cosmético já foi adquirido por este usuário.";


    @Transactional // Garante que se algo falhar (ex: debitar), tudo volta ao estado anterior
    public void comprarCosmetico(Long usuarioId, String cosmeticoId) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException(ERRO_NAO_ENCONTRADO));

        Cosmetico cosmetico = cosmeticoRepository.findById(cosmeticoId)
                .orElseThrow(() -> new RuntimeException(ERRO_NAO_ENCONTRADO));


        if (itemAdquiridoRepository.existsByUsuarioIdAndCosmeticoId(usuarioId, cosmeticoId)) {
            throw new RuntimeException(ERRO_JA_ADQUIRIDO);
        }

        // 2. Checagem de Saldo
        if (usuario.getCreditos() < cosmetico.getPreco()) {
            throw new RuntimeException(ERRO_SALDO);
        }

        // 3. Processar Compra
        usuario.setCreditos(usuario.getCreditos() - cosmetico.getPreco());

        // 4. Registrar Posse (Item Adquirido)
        ItemAdquirido itemAdquirido = new ItemAdquirido();
        itemAdquirido.setUsuario(usuario);
        itemAdquirido.setCosmetico(cosmetico);
        itemAdquirido.setUniqueUserCosmeticoKey(usuarioId + "-" + cosmeticoId);
        itemAdquiridoRepository.save(itemAdquirido);

        // 5. Registrar Histórico
        HistoricoTransacao historico = new HistoricoTransacao();
        historico.setUsuario(usuario);
        historico.setCosmetico(cosmetico);
        historico.setTipo(HistoricoTransacao.TipoTransacao.COMPRA);
        historico.setValor(cosmetico.getPreco());
        historicoRepository.save(historico);


        usuarioRepository.save(usuario); // Salva o novo saldo
    }

    @Transactional
    public void devolverCosmetico(Long usuarioId, String cosmeticoId) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException(ERRO_NAO_ENCONTRADO));

        // 1. Encontrar o registro de posse
        ItemAdquirido itemAdquirido = itemAdquiridoRepository
                .findByUsuarioIdAndCosmeticoId(usuarioId, cosmeticoId)
                .orElseThrow(() -> new RuntimeException("Cosmético não possuído."));

        // 2. Encontrar o preço original da compra (o preço está no objeto Cosmetico)
        Cosmetico cosmetico = itemAdquirido.getCosmetico();
        Integer valorEstorno = cosmetico.getPreco();

        usuario.setCreditos(usuario.getCreditos() + valorEstorno);

        // 4. Remover Posse
        itemAdquiridoRepository.delete(itemAdquirido);

        // 5. Registrar Histórico
        HistoricoTransacao historico = new HistoricoTransacao();
        historico.setUsuario(usuario);
        historico.setCosmetico(cosmetico);
        historico.setTipo(HistoricoTransacao.TipoTransacao.DEVOLUCAO);
        historico.setValor(valorEstorno);
        historicoRepository.save(historico);

        usuarioRepository.save(usuario);
    }
}
package com.lojafortnite.fortnite_store_api.service;

import com.lojafortnite.fortnite_store_api.dto.*;
import com.lojafortnite.fortnite_store_api.entity.Cosmetico;
import com.lojafortnite.fortnite_store_api.repository.CosmeticoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class FortniteSyncService {

    // URLs da API externa (constantes)
    private static final String API_URL_COSMETICS_ALL = "https://fortnite-api.com/v2/cosmetics?language=pt-BR";
    private static final String API_URL_COSMETICS_NEW = "https://fortnite-api.com/v2/cosmetics/new?language=pt-BR";
    private static final String API_URL_SHOP = "https://fortnite-api.com/v2/shop?language=pt-BR";

    private final RestTemplate restTemplate;
    private final CosmeticoRepository cosmeticoRepository;

    @Autowired
    public FortniteSyncService(CosmeticoRepository cosmeticoRepository) {
        this.cosmeticoRepository = cosmeticoRepository;
        this.restTemplate = new RestTemplate();
    }

    @Async // Roda em uma thread separada
    public void runInitialSync() {
        System.out.println("DISPARADOR: Verificando status do banco de dados para sincronização inicial...");
        try {
            // 1. VERIFICAR SE O BANCO JÁ ESTÁ POVOADO
            long itemCount = cosmeticoRepository.count();

            // 2. SE JÁ TIVER ITENS
            if (itemCount > 0) {
                System.out.println("DISPARADOR: Banco já populado com " + itemCount + " itens. Pulando sincronização base.");

                // 3. APENAS ATUALIZAR A LOJA E OS ITENS NOVOS (sem o delay de 30s)
                // Isso é rápido e garante que a loja esteja correta ao reiniciar.
                syncShopAndNewStatusInternal();

                System.out.println("DISPARADOR: Sincronização de status (loja/novos) concluída.");
                return; // Encerra o método aqui
            }

            // 4. SE O BANCO ESTIVER VAZIO (itemCount == 0)
            // Isso só vai acontecer na PRIMEIRA vez que você rodar, ou se apagar o volume.
            System.out.println("DISPARADOR: Banco de dados vazio. Aguardando 30s para o Flyway...");
            Thread.sleep(30000); // 30 segundos

            // 5. RODA A SINCRONIZAÇÃO COMPLETA
            syncAllBaseCosmeticsAndStatus();

            System.out.println("DISPARADOR: Sincronização inicial completa concluída.");

        } catch (InterruptedException e) {
            System.err.println("DISPARADOR: Thread de sincronização interrompida.");
            Thread.currentThread().interrupt();
        }
    }


    // 1. A CADA HORA (no minuto 05): Atualiza Loja e Novos (Rápido)
    // Ex: 13:05, 14:05... 21:05 (Logo após a loja virar)
    @Scheduled(cron = "0 5 * * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void updateShopStatus() {
        System.out.println("AGENDADOR: [MINUTO 05] Atualizando apenas status da Loja/Novos...");
        syncShopAndNewStatusInternal();
    }
    
    @Scheduled(cron = "0 0 5 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void fullDatabaseSync() {
        System.out.println("AGENDADOR: [05:00 AM] Iniciando Sincronização COMPLETA...");

        syncAllBaseCosmeticsAndStatus();
    }

    /**
     * TAREFA PRINCIPAL UNIFICADA: Sincroniza a base de dados (UPSERT) e o status.
     * Este metodo NÃO tem @Scheduled, ele é chamado pelos métodos acima.
     */
    @Transactional
    public void syncAllBaseCosmeticsAndStatus() {
        System.out.println("INICIANDO: Sincronização de todos os cosméticos base e status...");
        try {
            // 1. Sincronização da Base Completa (UPSERT)

            FortniteApiResponseDTO response = restTemplate.getForObject(API_URL_COSMETICS_ALL, FortniteApiResponseDTO.class);

            if (response != null &&
                    response.getData() != null &&
                    response.getData().getBr() != null) {

                List<CosmeticoApiDTO> todosOsItens = response.getData().getBr();

                for (CosmeticoApiDTO dto : todosOsItens) {
                    Optional<Cosmetico> existing = cosmeticoRepository.findById(dto.getId());
                    Cosmetico cosmetico = existing.orElseGet(Cosmetico::new);

                    cosmetico.setId(dto.getId());
                    cosmetico.setNome(dto.getName());
                    cosmetico.setDescricao(dto.getDescription());
                    cosmetico.setDataInclusao(dto.getAdded());

                    if (dto.getType() != null) {
                        cosmetico.setTipo(dto.getType().getDisplayValue());
                    }
                    if (dto.getRarity() != null) {
                        cosmetico.setRaridade(dto.getRarity().getDisplayValue());
                    }

                    if (dto.getImages() != null) {
                        String imageUrl = (dto.getImages().getIcon() != null)
                                ? dto.getImages().getIcon()
                                : dto.getImages().getSmallIcon();
                        cosmetico.setUrlImagem(imageUrl);
                    }

                    cosmeticoRepository.save(cosmetico);
                }

                syncShopAndNewStatusInternal();

                System.out.println("SUCESSO: Sincronização base e status concluída. Itens processados: " + todosOsItens.size());

            } else {
                System.out.println("AVISO: A resposta da API de cosméticos (ALL) veio vazia ou nula.");
            }

        } catch (Exception e) {
            System.err.println("ERRO fatal ao sincronizar cosméticos base: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


    /**
     * TAREFA SECUNDÁRIA INTERNA: Sincroniza status (À Venda e Novos).
     * Este metodo NÃO tem @Scheduled.
     */
    @Transactional
    public void syncShopAndNewStatusInternal() {
        System.out.println("INICIANDO: Sincronização interna de status (Loja e Novos)...");
        try {
            // 1. Resetar status antigos (Usando os métodos do Repository)
            cosmeticoRepository.resetAllIsForSaleStatus();
            cosmeticoRepository.resetAllIsNewStatus();

            // 2. Sincronizar Loja (Preço e 'isForSale')
            Set<String> itemsOnSale = new HashSet<>();
            RestTemplate statusRestTemplate = new RestTemplate();

            // Esta linha continua igual
            FortniteShopResponseDTO shopResponse = statusRestTemplate.getForObject(API_URL_SHOP, FortniteShopResponseDTO.class);

            if (shopResponse != null && shopResponse.getData() != null && shopResponse.getData().getEntries() != null) {
                processShopSection(shopResponse.getData().getEntries(), itemsOnSale);
            }

            System.out.println("SUCESSO: Loja sincronizada. Itens à venda: " + itemsOnSale.size());

            FortniteNewApiResponseDTO newResponse = statusRestTemplate.getForObject(API_URL_COSMETICS_NEW, FortniteNewApiResponseDTO.class);

            if (newResponse != null &&
                    newResponse.getData() != null &&
                    newResponse.getData().getItems() != null &&
                    newResponse.getData().getItems().getBr() != null) {

                List<CosmeticoApiDTO> novosItens = newResponse.getData().getItems().getBr();

                for (CosmeticoApiDTO dto : novosItens) {
                    cosmeticoRepository.findById(dto.getId()).ifPresent(cosmetico -> {
                        cosmetico.setIsNew(true);
                        cosmeticoRepository.save(cosmetico);
                    });
                }
                System.out.println("SUCESSO: Itens novos sincronizados: " + novosItens.size());
            } else {
                System.out.println("SUCESSO: Nenhum item novo encontrado.");
            }

        } catch (Exception e) {
            System.err.println("ERRO ao sincronizar status da loja/novos: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void processShopSection(List<ShopItemDTO> entries, Set<String> itemsOnSale) {
        if (entries == null) {
            return;
        }

        // 1. Itera sobre cada "quadrado" da loja (agora chamados de 'entries')
        for (ShopItemDTO entry : entries) {

            // 2. Pega o preço desse "quadrado"
            Integer precoFinal = entry.getFinalPrice();

            // 3. Pega a lista de cosméticos reais (brItems) dentro desse "quadrado"
            List<CosmeticoApiDTO> brItems = entry.getBrItems();
            if (brItems == null) {
                continue; // Pula este "quadrado" se ele não tiver itens
            }

            // 4. Itera sobre os cosméticos REAIS (brItems)
            for (CosmeticoApiDTO brItem : brItems) {

                String cosmeticId = brItem.getId(); // Este é o ID (ex: "CID_...")

                // 5. Verifica se já não processamos esse item
                if (cosmeticId != null && !itemsOnSale.contains(cosmeticId)) {

                    // 6. Busca o cosmético no nosso banco de dados
                    cosmeticoRepository.findById(cosmeticId).ifPresent(cosmetico -> {

                        // 7. ATUALIZA o cosmético com o status da loja
                        cosmetico.setIsForSale(true);
                        cosmetico.setPreco(precoFinal); // Aplica o preço do "quadrado"

                        cosmeticoRepository.save(cosmetico); // Salva a atualização

                        // 8. Adiciona ao Set para não processar de novo
                        itemsOnSale.add(cosmetico.getId());
                    });
                }
            }
        }
    }
}
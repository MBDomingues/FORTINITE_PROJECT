package com.lojafortnite.fortnite_store_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lojafortnite.fortnite_store_api.dto.*;
import com.lojafortnite.fortnite_store_api.entity.Cosmetico;
import com.lojafortnite.fortnite_store_api.repository.CosmeticoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FortniteSyncService {

    // --- LISTA DE URLs PARA O CATÁLOGO COMPLETO ---
    private static final List<String> URLS_CATALOGO = Arrays.asList(
            "https://fortnite-api.com/v2/cosmetics/br?language=pt-BR",
            "https://fortnite-api.com/v2/cosmetics/cars?language=pt-BR",
            "https://fortnite-api.com/v2/cosmetics/tracks?language=pt-BR",
            "https://fortnite-api.com/v2/cosmetics/instruments?language=pt-BR",
            "https://fortnite-api.com/v2/cosmetics/lego?language=pt-BR",
            "https://fortnite-api.com/v2/cosmetics/beans?language=pt-BR",
            "https://fortnite-api.com/v2/cosmetics/lego/kits?language=pt-BR"
    );

    private static final String API_URL_COSMETICS_NEW = "https://fortnite-api.com/v2/cosmetics/new?language=pt-BR";
    private static final String API_URL_SHOP = "https://fortnite-api.com/v2/shop?language=pt-BR";

    private final RestTemplate restTemplate;
    private final CosmeticoRepository cosmeticoRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public FortniteSyncService(CosmeticoRepository cosmeticoRepository) {
        this.cosmeticoRepository = cosmeticoRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Async
    public void runInitialSync() {
        System.out.println("DISPARADOR: Verificando status do banco...");
        try {
            long itemCount = cosmeticoRepository.count();
            if (itemCount > 0) {
                System.out.println("DISPARADOR: Banco populado (" + itemCount + " itens). Atualizando apenas loja.");
                syncShopAndNewStatusInternal();
                System.out.println("DISPARADOR: Sincronização rápida concluída.");
                return;
            }

            System.out.println("DISPARADOR: Banco vazio. Aguardando 30s...");
            Thread.sleep(30000);

            syncAllBaseCosmeticsAndStatus();
            System.out.println("DISPARADOR: Sincronização inicial completa concluída.");

        } catch (InterruptedException e) {
            System.err.println("DISPARADOR: Interrompido.");
            Thread.currentThread().interrupt();
        }
    }

    @Scheduled(cron = "0 5 * * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void updateShopStatus() {
        System.out.println("AGENDADOR: [MINUTO 05] Atualizando loja...");
        syncShopAndNewStatusInternal();
    }

    @Scheduled(cron = "0 0 5 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void fullDatabaseSync() {
        System.out.println("AGENDADOR: [05:00 AM] Sincronização COMPLETA...");
        syncAllBaseCosmeticsAndStatus();
    }

    /**
     * TAREFA PRINCIPAL: Sincroniza TODAS as categorias (BR, Carros, Lego...)
     */
    @Transactional
    public void syncAllBaseCosmeticsAndStatus() {
        System.out.println("INICIANDO: Sincronização de TODAS as categorias...");
        int totalProcessado = 0;

        // Itera sobre a lista de URLs (br, cars, tracks...)
        for (String url : URLS_CATALOGO) {
            try {
                // Usa o DTO de Lista (FortniteApiResponseDTO)
                FortniteApiResponseDTO response = restTemplate.getForObject(url, FortniteApiResponseDTO.class);

                if (response != null && response.getData() != null) {
                    List<CosmeticoApiDTO> listaDeItens = response.getData();

                    for (CosmeticoApiDTO dto : listaDeItens) {
                        saveCosmeticoFromDTO(dto); // Salva ou atualiza o item
                    }
                    totalProcessado += listaDeItens.size();
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar URL " + url + ": " + e.getMessage());
            }
        }

        syncShopAndNewStatusInternal(); // Atualiza preços e status da loja
        System.out.println("SUCESSO: Sincronização completa. Total itens: " + totalProcessado);
    }

    /**
     * Método auxiliar para salvar um item individual do catálogo
     */
    private void saveCosmeticoFromDTO(CosmeticoApiDTO dto) {
        try {
            Optional<Cosmetico> existing = cosmeticoRepository.findById(dto.getId());
            Cosmetico cosmetico = existing.orElseGet(Cosmetico::new);

            cosmetico.setId(dto.getId());
            cosmetico.setNome(dto.getName());
            cosmetico.setDescricao(dto.getDescription());
            cosmetico.setDataInclusao(dto.getAdded());

            // Usa os métodos auxiliares do DTO para garantir valores
            cosmetico.setTipo(dto.getTipoTexto());
            cosmetico.setRaridade(dto.getRaridadeTexto());
            cosmetico.setUrlImagem(dto.getImagemPrincipal());

            // Salva cores da série (Marvel, DC...) se houver
            if (dto.getSeries() != null && dto.getSeries().getColors() != null) {
                cosmetico.setCoresJson(objectMapper.writeValueAsString(dto.getSeries().getColors()));
            }

            cosmeticoRepository.save(cosmetico);
        } catch (Exception e) {
            // Ignora erros pontuais para não parar o loop
        }
    }


    /**
     * Sincroniza APENAS o status da Loja e Novos Itens.
     */
    @Transactional
    public void syncShopAndNewStatusInternal() {
        System.out.println("INICIANDO: Atualização de Loja e Novos...");
        try {
            cosmeticoRepository.resetAllIsForSaleStatus();
            cosmeticoRepository.resetAllIsNewStatus();

            // 1. Processa a Loja (Bundles e Itens)
            Set<String> itemsOnSale = new HashSet<>();
            FortniteShopResponseDTO shopResponse = restTemplate.getForObject(API_URL_SHOP, FortniteShopResponseDTO.class);

            if (shopResponse != null && shopResponse.getData() != null && shopResponse.getData().getEntries() != null) {
                processShopSection(shopResponse.getData().getEntries(), itemsOnSale);
            }
            System.out.println("SUCESSO: Loja sincronizada. Itens à venda: " + itemsOnSale.size());

            // 2. Processa Novos Itens (DTO Aninhado)
            FortniteNewApiResponseDTO newResponse = restTemplate.getForObject(API_URL_COSMETICS_NEW, FortniteNewApiResponseDTO.class);

            if (newResponse != null && newResponse.getData() != null && newResponse.getData().getItems() != null) {
                // Pega TODOS os tipos de itens novos (br, cars, tracks...)
                List<CosmeticoApiDTO> novosItens = newResponse.getData().getItems().getTodosOsItens();

                for (CosmeticoApiDTO dto : novosItens) {
                    cosmeticoRepository.findById(dto.getId()).ifPresent(cosmetico -> {
                        cosmetico.setIsNew(true);
                        cosmeticoRepository.save(cosmetico);
                    });
                }
                System.out.println("SUCESSO: Itens novos sincronizados: " + novosItens.size());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- LÓGICA DE BUNDLE E LOJA ---
    private void processShopSection(List<ShopItemDTO> entries, Set<String> itemsOnSale) {
        if (entries == null) return;

        for (ShopItemDTO entry : entries) {
            Integer precoFinal = entry.getFinalPrice();
            List<CosmeticoApiDTO> brItems = entry.getBrItems();

            if (brItems == null || brItems.isEmpty()) continue;

            // A. É UM BUNDLE
            if (entry.getBundle() != null) {
                try {
                    String bundleId = "BUNDLE_" + entry.getBundle().getName().replaceAll("[^a-zA-Z0-9]", "");

                    Cosmetico pacote = cosmeticoRepository.findById(bundleId).orElse(new Cosmetico());
                    pacote.setId(bundleId);
                    pacote.setNome(entry.getBundle().getName());
                    pacote.setDescricao(entry.getBundle().getInfo());
                    pacote.setUrlImagem(entry.getBundle().getImage());
                    pacote.setPreco(precoFinal);
                    pacote.setIsForSale(true);
                    pacote.setIsBundle(true);
                    pacote.setTipo("Pacotão");
                    pacote.setRaridade("Comum");

                    List<String> idsFilhos = brItems.stream().map(CosmeticoApiDTO::getId).collect(Collectors.toList());
                    pacote.setBundleItemsJson(objectMapper.writeValueAsString(idsFilhos));

                    if (entry.getColors() != null) {
                        pacote.setCoresJson(objectMapper.writeValueAsString(entry.getColors().values()));
                    }

                    cosmeticoRepository.save(pacote);
                    itemsOnSale.add(bundleId);

                    // Garante que os filhos existam no banco (para poder entregar na compra)
                    for (CosmeticoApiDTO brItem : brItems) {
                        saveCosmeticoFromDTO(brItem);
                    }

                } catch (Exception e) {
                    System.err.println("Erro Bundle: " + e.getMessage());
                }
            }

            // B. É ITEM AVULSO (SEM BUNDLE)
            else {
                for (CosmeticoApiDTO brItem : brItems) {
                    String cosmeticId = brItem.getId();
                    if (cosmeticId != null) {
                        cosmeticoRepository.findById(cosmeticId).ifPresent(cosmetico -> {
                            cosmetico.setIsForSale(true);
                            cosmetico.setPreco(precoFinal);

                            if (entry.getColors() != null) {
                                try {
                                    cosmetico.setCoresJson(objectMapper.writeValueAsString(entry.getColors().values()));
                                } catch (Exception e) {}
                            }

                            cosmeticoRepository.save(cosmetico);
                            itemsOnSale.add(cosmetico.getId());
                        });
                    }
                }
            }
        }
    }
}
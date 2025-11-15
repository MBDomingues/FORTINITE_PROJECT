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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FortniteSyncService {

    // URLs da API externa (constantes)
    private static final String API_URL_COSMETICS_ALL = "https://fortnite-api.com/v2/cosmetics?language=pt-BR";
    private static final String API_URL_COSMETICS_NEW = "https://fortnite-api.com/v2/cosmetics/new?language=pt-BR";
    private static final String API_URL_SHOP = "https://fortnite-api.com/v2/shop?language=pt-BR";

    private final RestTemplate restTemplate;
    private final CosmeticoRepository cosmeticoRepository;
    private final ObjectMapper objectMapper; // Usado para converter objetos em JSON String

    @Autowired
    public FortniteSyncService(CosmeticoRepository cosmeticoRepository) {
        this.cosmeticoRepository = cosmeticoRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
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
                syncShopAndNewStatusInternal();

                System.out.println("DISPARADOR: Sincronização de status (loja/novos) concluída.");
                return; // Encerra o método aqui
            }

            // 4. SE O BANCO ESTIVER VAZIO (itemCount == 0)
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
    @Scheduled(cron = "0 5 * * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void updateShopStatus() {
        System.out.println("AGENDADOR: [MINUTO 05] Atualizando apenas status da Loja/Novos...");
        syncShopAndNewStatusInternal();
    }

    // 2. UMA VEZ POR DIA (05:00 AM): Atualiza TUDO (Lento)
    @Scheduled(cron = "0 0 5 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void fullDatabaseSync() {
        System.out.println("AGENDADOR: [05:00 AM] Iniciando Sincronização COMPLETA...");
        syncAllBaseCosmeticsAndStatus();
    }

    /**
     * TAREFA PRINCIPAL UNIFICADA: Sincroniza a base de dados (UPSERT) e o status.
     */
    @Transactional
    public void syncAllBaseCosmeticsAndStatus() {
        System.out.println("INICIANDO: Sincronização de todos os cosméticos base e status...");
        try {
            // 1. Sincronização da Base Completa (UPSERT)
            // Usamos o FortniteApiResponseDTO (Lista)
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

                    // --- NOVA LÓGICA: CORES DA SÉRIE ---
                    if (dto.getSeries() != null && dto.getSeries().getColors() != null) {
                        try {
                            // Salva as cores da série (ex: Marvel Red) no JSON
                            String coresJson = objectMapper.writeValueAsString(dto.getSeries().getColors());
                            cosmetico.setCoresJson(coresJson);
                        } catch (Exception e) {
                            System.err.println("Erro ao converter cores da série para JSON: " + e.getMessage());
                        }
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
     */
    @Transactional
    public void syncShopAndNewStatusInternal() {
        System.out.println("INICIANDO: Sincronização interna de status (Loja e Novos)...");
        try {
            // 1. Resetar status antigos
            cosmeticoRepository.resetAllIsForSaleStatus();
            cosmeticoRepository.resetAllIsNewStatus();

            // 2. Sincronizar Loja (Preço, 'isForSale', Bundles e Cores)
            Set<String> itemsOnSale = new HashSet<>();
            RestTemplate statusRestTemplate = new RestTemplate();

            FortniteShopResponseDTO shopResponse = statusRestTemplate.getForObject(API_URL_SHOP, FortniteShopResponseDTO.class);

            if (shopResponse != null && shopResponse.getData() != null && shopResponse.getData().getEntries() != null) {
                processShopSection(shopResponse.getData().getEntries(), itemsOnSale);
            }

            System.out.println("SUCESSO: Loja sincronizada. Itens à venda (incluindo bundles): " + itemsOnSale.size());

            // 3. Sincronizar Novos Itens ('isNew')
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

    // --- METODO COM A LÓGICA DE BUNDLE E CORES ---
    private void processShopSection(List<ShopItemDTO> entries, Set<String> itemsOnSale) {
        if (entries == null) return;

        for (ShopItemDTO entry : entries) {
            Integer precoDaOferta = entry.getFinalPrice();
            List<CosmeticoApiDTO> brItems = entry.getBrItems();

            if (brItems == null || brItems.isEmpty()) continue;

            // CENÁRIO A: É UM BUNDLE (PACOTÃO)
            if (entry.getBundle() != null) {
                try {
                    // 1. Cria/Atualiza o objeto do PACOTE
                    String bundleId = "BUNDLE_" + entry.getBundle().getName().replaceAll("[^a-zA-Z0-9]", "");

                    Cosmetico pacote = cosmeticoRepository.findById(bundleId).orElse(new Cosmetico());
                    pacote.setId(bundleId);
                    pacote.setNome(entry.getBundle().getName());
                    pacote.setDescricao(entry.getBundle().getInfo());
                    pacote.setUrlImagem(entry.getBundle().getImage());
                    pacote.setPreco(precoDaOferta); // Preço do PACOTE
                    pacote.setIsForSale(true);
                    pacote.setIsBundle(true);
                    pacote.setTipo("Pacotão");
                    pacote.setRaridade("Comum");

                    // Salva os IDs dos filhos para entregar na compra
                    List<String> idsFilhos = brItems.stream().map(CosmeticoApiDTO::getId).collect(Collectors.toList());
                    pacote.setBundleItemsJson(objectMapper.writeValueAsString(idsFilhos));

                    if (entry.getColors() != null) {
                        pacote.setCoresJson(objectMapper.writeValueAsString(entry.getColors().values()));
                    }

                    cosmeticoRepository.save(pacote);
                    itemsOnSale.add(bundleId);

                    // 2. Garante que os itens FILHOS existem no banco, mas NÃO define o preço deles aqui.
                    //    (Se eles forem vendidos separadamente, aparecerão em OUTRA entrada da lista 'entries'
                    //     ou a gente precisa de uma lógica de "fallback" se a API não mandar separado).
                    for (CosmeticoApiDTO brItem : brItems) {
                        // Apenas garante que o item base existe (sem setar isForSale=true ainda)
                        // Quem vai setar isForSale=true é o CENÁRIO B, se ele acontecer.
                        salvarItemBaseSeNaoExistir(brItem);
                    }

                } catch (Exception e) {
                    System.err.println("Erro ao processar Bundle: " + e.getMessage());
                }
            }

            // CENÁRIO B: É UM ITEM ÚNICO (OU UM ITEM DE BUNDLE VENDIDO SEPARADO)
            // A API do Fortnite às vezes manda uma entrada SÓ para o Bundle e
            // outras entradas separadas para os itens.
            // SE não tem objeto 'bundle', é um item avulso à venda.
            else {
                for (CosmeticoApiDTO brItem : brItems) {
                    String cosmeticId = brItem.getId();
                    if (cosmeticId != null) {
                        cosmeticoRepository.findById(cosmeticId).ifPresent(cosmetico -> {
                            cosmetico.setIsForSale(true);
                            cosmetico.setPreco(precoDaOferta); // Preço INDIVIDUAL

                            // Salva cores se houver
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

    // Método auxiliar simples para garantir integridade referencial
    private void salvarItemBaseSeNaoExistir(CosmeticoApiDTO dto) {
        if (!cosmeticoRepository.existsById(dto.getId())) {
            Cosmetico c = new Cosmetico();
            c.setId(dto.getId());
            c.setNome(dto.getName());
            // ... preencher dados básicos ...
            cosmeticoRepository.save(c);
        }
    }
}
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

    // --- MÉTODO COM A LÓGICA DE BUNDLE E CORES ---
    private void processShopSection(List<ShopItemDTO> entries, Set<String> itemsOnSale) {
        if (entries == null) {
            return;
        }

        for (ShopItemDTO entry : entries) {
            Integer precoFinal = entry.getFinalPrice();
            List<CosmeticoApiDTO> brItems = entry.getBrItems();

            if (brItems == null || brItems.isEmpty()) {
                continue; // Pula se não tiver itens
            }

            // 1. LÓGICA DE BUNDLE (PACOTES)
            if (entry.getBundle() != null) {
                try {
                    // Cria um ID único para o Bundle baseado no nome (remove espaços e caracteres especiais)
                    String bundleId = "BUNDLE_" + entry.getBundle().getName().replaceAll("[^a-zA-Z0-9]", "");

                    // Verifica se já existe ou cria novo
                    Cosmetico pacote = cosmeticoRepository.findById(bundleId).orElse(new Cosmetico());

                    pacote.setId(bundleId);
                    pacote.setNome(entry.getBundle().getName());
                    pacote.setDescricao(entry.getBundle().getInfo()); // "Bundle" ou descrição se houver
                    pacote.setUrlImagem(entry.getBundle().getImage());
                    pacote.setPreco(precoFinal);
                    pacote.setIsForSale(true);
                    pacote.setIsBundle(true); // <-- Importante para o CompraService saber
                    pacote.setTipo("Pacotão"); // Para aparecer nos filtros como Pacotão
                    pacote.setRaridade("Comum"); // Default, ou pegue do primeiro item

                    // A. Salva os IDs dos itens filhos no JSON
                    List<String> idsFilhos = brItems.stream()
                            .map(CosmeticoApiDTO::getId)
                            .collect(Collectors.toList());
                    pacote.setBundleItemsJson(objectMapper.writeValueAsString(idsFilhos));

                    // B. Salva as CORES do Bundle (vindo de entry.getColors())
                    if (entry.getColors() != null) {
                        // Converte os valores do mapa de cores em uma lista JSON
                        String coresJson = objectMapper.writeValueAsString(entry.getColors().values());
                        pacote.setCoresJson(coresJson);
                    }

                    cosmeticoRepository.save(pacote);
                    itemsOnSale.add(bundleId); // Marca que o bundle está na loja

                } catch (Exception e) {
                    System.err.println("Erro ao processar/salvar Bundle: " + e.getMessage());
                }
            }

            // 2. LÓGICA DE ITENS INDIVIDUAIS
            for (CosmeticoApiDTO brItem : brItems) {
                String cosmeticId = brItem.getId();

                // Só processa se não for um item já processado
                if (cosmeticId != null && !itemsOnSale.contains(cosmeticId)) {

                    // Se o item existe no banco (sincronizado pelo Catálogo Geral)
                    cosmeticoRepository.findById(cosmeticId).ifPresent(cosmetico -> {

                        // Lógica Simplificada: Se está na lista 'brItems' da loja, está à venda.
                        // Mesmo que seja parte de um bundle, ele geralmente pode ser comprado,
                        // ou pelo menos exibido.
                        cosmetico.setIsForSale(true);

                        // ATENÇÃO: Se for parte de um bundle, o preço aqui pode ser o do bundle.
                        // Se o entry.getBundle() for NULO, o preço é do item com certeza.
                        if (entry.getBundle() == null) {
                            cosmetico.setPreco(precoFinal);
                        }
                        // Se for bundle, não alteramos o preço do item individual para não colocar o preço do pacote nele.

                        cosmeticoRepository.save(cosmetico);
                        itemsOnSale.add(cosmetico.getId());
                    });
                }
            }
        }
    }
}
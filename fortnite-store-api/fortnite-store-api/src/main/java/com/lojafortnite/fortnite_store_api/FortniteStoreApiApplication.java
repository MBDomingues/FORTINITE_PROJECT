package com.lojafortnite.fortnite_store_api;

import com.lojafortnite.fortnite_store_api.service.FortniteSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner; // Importar
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; // Importar
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync // Habilita o @Async no método runInitialSync do serviço
public class FortniteStoreApiApplication implements CommandLineRunner { // Implementa o Runner

    @Autowired
    private FortniteSyncService fortniteSyncService;

    public static void main(String[] args) {
        SpringApplication.run(FortniteStoreApiApplication.class, args);
    }

    /**
     * Este método é executado DEPOIS que o Flyway e o contexto JPA estão prontos.
     */
    @Override
    public void run(String... args) throws Exception {
        // Dispara a sincronização inicial em uma THREAD SEPARADA (devido ao @Async)
        // Isso garante que a thread principal do Spring termine de iniciar (e o Flyway finalize)
        fortniteSyncService.runInitialSync();
    }
}
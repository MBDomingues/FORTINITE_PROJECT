package com.lojafortnite.fortnite_store_api;

import com.lojafortnite.fortnite_store_api.service.FortniteSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner; 
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; 
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync 
public class FortniteStoreApiApplication implements CommandLineRunner { 

    @Autowired
    private FortniteSyncService fortniteSyncService;

    public static void main(String[] args) {
        SpringApplication.run(FortniteStoreApiApplication.class, args);
    }

    
    @Override
    public void run(String... args) throws Exception {
        
        
        fortniteSyncService.runInitialSync();
    }
}
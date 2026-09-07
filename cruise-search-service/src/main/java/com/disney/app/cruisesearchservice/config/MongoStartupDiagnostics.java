package com.disney.app.cruisesearchservice.config;

import com.mongodb.reactivestreams.client.MongoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class MongoStartupDiagnostics implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoStartupDiagnostics.class);

    private final ReactiveMongoTemplate mongoTemplate;
    private final String configuredUri;
    private final String configuredDatabase;

    public MongoStartupDiagnostics(
            ReactiveMongoTemplate mongoTemplate,
            @Value("${spring.mongodb.uri}") String configuredUri,
            @Value("${spring.mongodb.database:}") String configuredDatabase
    ) {
        this.mongoTemplate = mongoTemplate;
        this.configuredUri = configuredUri;
        this.configuredDatabase = configuredDatabase;
    }

    @Override
    public void run(ApplicationArguments args) {
        mongoTemplate.getMongoDatabase()
                .map(MongoDatabase::getName)
                .doOnNext(actualDatabase -> log.info(
                        "mongo_database_configured uri={} databaseProperty={} actualDatabase={}",
                        configuredUri,
                        configuredDatabase,
                        actualDatabase
                ))
                .subscribe();
    }
}

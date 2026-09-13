package com.disney.app.cruisesearchservice.config;

import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MongoStartupDiagnostics implements ApplicationRunner {

    private final ReactiveMongoTemplate mongoTemplate;
    private final String configuredUri;
    private final String configuredDatabase;

    public MongoStartupDiagnostics(
            ReactiveMongoTemplate mongoTemplate,
            AppMongoProperties properties
    ) {
        this.mongoTemplate = mongoTemplate;
        this.configuredUri = properties.uri();
        this.configuredDatabase = properties.database();
    }

    @Override
    public void run(ApplicationArguments args) {
        mongoTemplate.getMongoDatabase()
                .map(MongoDatabase::getName)
                .doOnNext(actualDatabase -> log.info(
                        "MongoDB connection is ready. uri={} configuredDatabase={} actualDatabase={}",
                        configuredUri,
                        configuredDatabase,
                        actualDatabase
                ))
                .subscribe();
    }
}

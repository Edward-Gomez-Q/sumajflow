package ucb.edu.bo.sumajflow.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.concurrent.TimeUnit;

/**
 * Configuración optimizada de MongoDB para el sistema de tracking
 */
@Slf4j
@Configuration
@EnableMongoRepositories(basePackages = "ucb.edu.bo.sumajflow.repository.mongodb")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${MONGO_DB:sumajflow_tracking}")
    private String databaseName;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(builder -> builder
                        .maxSize(20)                          // Pool máximo de conexiones
                        .minSize(5)                           // Pool mínimo de conexiones
                        .maxWaitTime(30, TimeUnit.SECONDS)    // Tiempo máximo de espera
                        .maxConnectionIdleTime(60, TimeUnit.SECONDS)
                        .maxConnectionLifeTime(300, TimeUnit.SECONDS))
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(10, TimeUnit.SECONDS) // Timeout de conexión
                        .readTimeout(30, TimeUnit.SECONDS))   // Timeout de lectura
                .applyToServerSettings(builder -> builder
                        .heartbeatFrequency(10, TimeUnit.SECONDS))
                .retryWrites(true)                            // Reintentar escrituras en caso de fallo
                .retryReads(true)                             // Reintentar lecturas en caso de fallo
                .build();

        log.info("Conectando a MongoDB: {} - Base de datos: {}",
                connectionString.getConnectionString().replaceAll("://.*@", "://*****@"),
                databaseName);

        return MongoClients.create(settings);
    }

    @Override
    protected boolean autoIndexCreation() {
        // Ya está habilitado en application.yml con auto-index-creation: true
        return true;
    }

    /**
     * Configuración de conversiones personalizadas si es necesario
     * Por ejemplo, para manejar tipos de datos especiales
     */
    @Bean
    @Override
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(java.util.Collections.emptyList());
    }

}
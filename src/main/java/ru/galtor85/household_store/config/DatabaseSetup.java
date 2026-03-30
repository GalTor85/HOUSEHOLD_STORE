package ru.galtor85.household_store.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import ru.galtor85.household_store.service.i18n.MessageService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

@Slf4j
@Configuration
@Profile("!docker")
public class DatabaseSetup {

    private final MessageService messageService;

    @Value("${POSTGRES_HOST:localhost}")
    private String host;

    @Value("${POSTGRES_PORT:5432}")
    private String port;

    @Value("${POSTGRES_USER:postgres}")
    private String adminUser;

    @Value("${POSTGRES_PASSWORD:postgres}")
    private String adminPassword;

    @Value("${POSTGRES_DB:household_store}")
    private String databaseName;

    public DatabaseSetup(MessageService messageService) {
        this.messageService = messageService;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        log.info(messageService.get("database.setup.local.mode"));
        createDatabaseIfNotExists();
        createSchemaIfNotExists();
        return createDataSource();
    }

    private DataSource createDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(String.format(
                "jdbc:postgresql://%s:%s/%s?currentSchema=household_schema&useUnicode=true&characterEncoding=UTF-8",
                host, port, databaseName));
        dataSource.setUsername(adminUser);
        dataSource.setPassword(adminPassword);
        log.debug(messageService.get("database.setup.datasource.created", adminUser, databaseName));
        return dataSource;
    }

    private void createDatabaseIfNotExists() {
        String adminUrl = String.format("jdbc:postgresql://%s:%s/", host, port);
        try (Connection conn = DriverManager.getConnection(adminUrl, adminUser, adminPassword);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + databaseName + "'");
            if (!rs.next()) {
                String createDbSql = String.format(
                        "CREATE DATABASE %s WITH ENCODING='UTF8' LC_COLLATE='ru_RU.UTF-8' LC_CTYPE='ru_RU.UTF-8' TEMPLATE=template0",
                        databaseName);
                stmt.executeUpdate(createDbSql);
                log.info(messageService.get("database.setup.database.created"), databaseName);
            } else {
                log.debug(messageService.get("database.setup.database.exists", databaseName));
            }
        } catch (Exception e) {
            log.error(messageService.get("database.setup.database.check.failed"), e.getMessage(), e);
            throw new RuntimeException("Database check/create failed", e);
        }
    }

    private void createSchemaIfNotExists() {
        String dbUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, databaseName);
        try (Connection conn = DriverManager.getConnection(dbUrl, adminUser, adminPassword);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS household_schema");
            stmt.executeUpdate("SET client_encoding = 'UTF8'");
            log.debug(messageService.get("database.setup.schema.configured"));
        } catch (Exception e) {
            log.error(messageService.get("database.setup.schema.create.failed"), e.getMessage(), e);
            throw new RuntimeException("Schema creation failed", e);
        }
    }
}
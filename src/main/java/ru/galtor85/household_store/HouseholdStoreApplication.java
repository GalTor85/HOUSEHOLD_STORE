package ru.galtor85.household_store;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@SpringBootApplication
@EnableScheduling
public class HouseholdStoreApplication {

    private final Environment environment;


    public HouseholdStoreApplication(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public SpringLiquibase springLiquibase(DataSource dataSource) {
        SpringLiquibase springLiquibase = new SpringLiquibase();
        springLiquibase.setDataSource(dataSource);
        springLiquibase.setChangeLog(
                environment.getProperty("spring.liquibase.change-log", "classpath:db/changelog/db.changelog-master.xml")
        );
        springLiquibase.setDefaultSchema(
                environment.getProperty("spring.liquibase.default-schema", "household_schema")
        );
        springLiquibase.setDatabaseChangeLogTable(
                environment.getProperty("spring.liquibase.database-change-log-table", "DATABASECHANGELOG")
        );
        springLiquibase.setDatabaseChangeLogLockTable(
                environment.getProperty("spring.liquibase.database-change-log-lock-table", "DATABASECHANGELOGLOCK")
        );
        springLiquibase.setDropFirst(
                environment.getProperty("spring.liquibase.drop-first", Boolean.class, false)
        );
        springLiquibase.setShouldRun(
                environment.getProperty("spring.liquibase.enabled", Boolean.class, true)
        );
        springLiquibase.setContexts(
                environment.getProperty("spring.liquibase.contexts", "main")
        );

        return springLiquibase;
    }

    public static void main(String[] args) {
        SpringApplication.run(HouseholdStoreApplication.class, args);
    }
}

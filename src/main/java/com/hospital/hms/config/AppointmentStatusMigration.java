package com.hospital.hms.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AppointmentStatusMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public AppointmentStatusMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.update("UPDATE appointments SET status = 'PENDING' WHERE status IS NULL");
        jdbcTemplate.execute("ALTER TABLE appointments MODIFY COLUMN status VARCHAR(16) NOT NULL");
        jdbcTemplate.update("UPDATE appointments SET status = 'APPROVED' WHERE status = 'CONFIRMED'");
    }
}

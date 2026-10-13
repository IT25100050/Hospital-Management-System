package com.hospital.hms.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MedicalRecordChangeRequesterMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public MedicalRecordChangeRequesterMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String requesterColumnType = jdbcTemplate.queryForObject(
                "SELECT COLUMN_TYPE FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = 'medical_record_change_requests' " +
                        "AND COLUMN_NAME = 'requested_by_user_id'",
                String.class);
        if (requesterColumnType == null) {
            throw new IllegalStateException("Medical record change requester column was not found.");
        }
        jdbcTemplate.update("UPDATE medical_record_change_requests request " +
                "JOIN users account ON account.id = request.requested_by_user_id " +
                "SET request.requested_by_username = account.username " +
                "WHERE request.requested_by_username IS NULL");
        jdbcTemplate.execute("ALTER TABLE medical_record_change_requests MODIFY COLUMN " +
                "requested_by_user_id " + requesterColumnType + " NULL");
    }
}

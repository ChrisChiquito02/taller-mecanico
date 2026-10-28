package com.tallermecanico.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Quita del esquema heredado la columna de rol de usuario, que ya no forma parte del modelo. */
@Component
public class LegacyUserRoleColumnMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    /** Recibe el acceso SQL para inspeccionar y ajustar el esquema existente. */
    public LegacyUserRoleColumnMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Elimina users.role si existe; no modifica el resto de datos de la tabla. */
    @Override
    public void run(ApplicationArguments args) {
        Integer columns = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'role'",
                Integer.class);
        if (columns != null && columns > 0) {
            jdbcTemplate.execute("ALTER TABLE users DROP COLUMN role");
        }
    }
}

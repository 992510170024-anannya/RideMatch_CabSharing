CREATE DATABASE IF NOT EXISTS ridematch_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- Create a least-privilege application user separately and provide its
-- username/password through MYSQL_USER and MYSQL_PASSWORD environment variables.
-- Example (run as a MySQL administrator, replacing the placeholder securely):
-- CREATE USER 'ridematch_app'@'localhost' IDENTIFIED BY '<set-a-local-secret>';
-- GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
--     ON ridematch_db.* TO 'ridematch_app'@'localhost';

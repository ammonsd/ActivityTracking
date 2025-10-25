-- Test data for H2 database
-- Insert test users for integration testing

INSERT INTO users (username, userpassword, userrole, enabled, created_date) VALUES 
('testuser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER', true, CURRENT_TIMESTAMP),
('testadmin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ADMIN', true, CURRENT_TIMESTAMP);

INSERT INTO dropdownvalues (category, itemvalue, displayorder, isactive) VALUES
('CLIENT', 'Test Client', 1, true),
('PROJECT', 'Test Project', 1, true),
('PHASE', 'Test Phase', 1, true);
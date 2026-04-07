INSERT INTO users (username, email, password_hash, role) VALUES
    ('Miha', 'miha@example.com', '$2a$12$LME/M1AOYUWJgEyjU5ATxuTrC4AmmhCGUt1te0M00Y9jqMJhDiGlq', 'USER'),
    ('Brina', 'brina@example.com', '$2a$12$LME/M1AOYUWJgEyjU5ATxuTrC4AmmhCGUt1te0M00Y9jqMJhDiGlq', 'USER'),
    ('Tone', 'tone@example.com', '$2a$12$LME/M1AOYUWJgEyjU5ATxuTrC4AmmhCGUt1te0M00Y9jqMJhDiGlq', 'OPERATOR'),
    ('Peter', 'peter@example.com', '$2a$12$LME/M1AOYUWJgEyjU5ATxuTrC4AmmhCGUt1te0M00Y9jqMJhDiGlq', 'OPERATOR');
INSERT INTO rooms (name) VALUES
    ('Tehnika'),
    ('Storitve'),
    ('Pogovor');
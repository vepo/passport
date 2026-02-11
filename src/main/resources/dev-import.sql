DO $$
BEGIN
    INSERT INTO tb_profiles (name) VALUES ('Domain Manager');
    INSERT INTO tb_profiles (name) VALUES ('Domain Viewer');
    INSERT INTO tb_profiles (name) VALUES ('Passport Admin');
    INSERT INTO tb_roles (name) VALUES ('Domain.Editor');
    INSERT INTO tb_roles (name) VALUES ('Domain.Stats.Viewer');
    INSERT INTO tb_roles (name) VALUES ('passport.admin');
    
    INSERT INTO tb_profile_roles (profile_id, role_id) 
    VALUES ((SELECT id FROM tb_profiles WHERE name = 'Domain Manager'),
            (SELECT id FROM tb_roles WHERE name = 'Domain.Editor')), 
           ((SELECT id FROM tb_profiles WHERE name = 'Domain Manager'),
            (SELECT id FROM tb_roles WHERE name = 'Domain.Stats.Viewer')), 
           ((SELECT id FROM tb_profiles WHERE name = 'Domain Viewer'),
            (SELECT id FROM tb_roles WHERE name = 'Domain.Stats.Viewer')), 
           ((SELECT id FROM tb_profiles WHERE name = 'Passport Admin'),
            (SELECT id FROM tb_roles WHERE name = 'passport.admin'));

    -- -- Usuário sem roles (se aplicável)
    INSERT INTO tb_users (username, name, email, encoded_password) VALUES 
                         ('guest-user', 'Guest User', 'guest@passport.vepo.dev', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==');
    -- Combinação ADMIN + PROJECT_MANAGER + USER (Super Usuário)
    INSERT INTO tb_users (username, name, email, encoded_password) VALUES 
                         ('cto-boss', 'Chief Technology Officer', 'cto@passport.vepo.dev', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==');
    -- Usuário com apenas a role USER
    INSERT INTO tb_users (username, name, email, encoded_password) VALUES 
                         ('junior', 'Junior Developer', 'junior_dev@passport.vepo.dev', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==');

    INSERT INTO tb_users_profiles (user_id, profile_id) 
    VALUES ((SELECT id FROM tb_users WHERE username = 'cto-boss'),
            (SELECT id FROM tb_profiles WHERE name = 'Domain Manager')),
           ((SELECT id FROM tb_users WHERE username = 'cto-boss'),
            (SELECT id FROM tb_profiles WHERE name = 'Domain Viewer')),
           ((SELECT id FROM tb_users WHERE username = 'cto-boss'),
            (SELECT id FROM tb_profiles WHERE name = 'Passport Admin'));
END $$;
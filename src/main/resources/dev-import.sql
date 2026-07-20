DO $$
BEGIN
    INSERT INTO tb_profiles (name) VALUES ('Domain Manager');
    INSERT INTO tb_profiles (name) VALUES ('Domain Viewer');
    INSERT INTO tb_profiles (name) VALUES ('Passport Admin');
    INSERT INTO tb_profiles (name, disabled) VALUES ('Cursos Admin', false)
    ON CONFLICT (name) DO UPDATE SET disabled = false;
    INSERT INTO tb_roles (name) VALUES ('Domain.Editor');
    INSERT INTO tb_roles (name) VALUES ('Domain.Stats.Viewer');
    INSERT INTO tb_roles (name) VALUES ('passport.admin');
    INSERT INTO tb_roles (name) VALUES ('domains.admin');
    INSERT INTO tb_roles (name) VALUES ('engage.admin');
    INSERT INTO tb_roles (name) VALUES ('cursos.admin')
    ON CONFLICT (name) DO NOTHING;
    
    INSERT INTO tb_profile_roles (profile_id, role_id) 
    VALUES ((SELECT id FROM tb_profiles WHERE name = 'Domain Manager'),
            (SELECT id FROM tb_roles WHERE name = 'Domain.Editor')), 
           ((SELECT id FROM tb_profiles WHERE name = 'Domain Manager'),
            (SELECT id FROM tb_roles WHERE name = 'domains.admin')), 
           ((SELECT id FROM tb_profiles WHERE name = 'Domain Manager'),
            (SELECT id FROM tb_roles WHERE name = 'Domain.Stats.Viewer')), 
           ((SELECT id FROM tb_profiles WHERE name = 'Domain Manager'),
            (SELECT id FROM tb_roles WHERE name = 'engage.admin')), 
           ((SELECT id FROM tb_profiles WHERE name = 'Domain Viewer'),
            (SELECT id FROM tb_roles WHERE name = 'Domain.Stats.Viewer')), 
           ((SELECT id FROM tb_profiles WHERE name = 'Passport Admin'),
            (SELECT id FROM tb_roles WHERE name = 'passport.admin')),
           ((SELECT id FROM tb_profiles WHERE name = 'Passport Admin'),
            (SELECT id FROM tb_roles WHERE name = 'engage.admin'));

    INSERT INTO tb_profile_roles (profile_id, role_id)
    VALUES ((SELECT id FROM tb_profiles WHERE name = 'Cursos Admin'),
            (SELECT id FROM tb_roles WHERE name = 'cursos.admin'))
    ON CONFLICT (profile_id, role_id) DO NOTHING;

    -- -- Usuário sem roles (se aplicável)
    INSERT INTO tb_users (username, name, email, encoded_password) VALUES 
                         ('guest-user', 'Guest User', 'guest@passport.vepo.dev', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==');
    -- Combinação ADMIN + PROJECT_MANAGER + USER (Super Usuário)
    INSERT INTO tb_users (username, name, email, encoded_password, description) VALUES 
                         ('cto-boss', 'Chief Technology Officer', 'cto@passport.vepo.dev', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==',
                          'CTO e autor de cursos sobre Quarkus, Java e plataformas internas.');
    -- Usuário com apenas a role USER
    INSERT INTO tb_users (username, name, email, encoded_password, description) VALUES 
                         ('junior', 'Junior Developer', 'junior_dev@passport.vepo.dev', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==',
                          'Desenvolvedor júnior e autor do curso Angular na prática.');

    -- Extra Cursos students / second teacher (stable order after clean seed: ids 4–8)
    INSERT INTO tb_users (username, name, email, encoded_password, description) VALUES
                         ('alice', 'Alice Santos', 'alice@passport.vepo.dev', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==',
                          'Estudante de backend; matriculada em Quarkus com progresso parcial.');
    INSERT INTO tb_users (username, name, email, encoded_password, description) VALUES
                         ('bob', 'Bob Oliveira', 'bob@passport.vepo.dev', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==',
                          'Estudante que concluiu Introdução ao Quarkus (certificado).');
    INSERT INTO tb_users (username, name, email, encoded_password, description) VALUES
                         ('carol', 'Carol Mendes', 'carol@passport.vepo.dev', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==',
                          'Estudante com solicitação de matrícula pendente.');
    INSERT INTO tb_users (username, name, email, encoded_password, description) VALUES
                         ('diego', 'Diego Costa', 'diego@passport.vepo.dev', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==',
                          'Estudante com matrícula recusada (para testar REJECTED).');
    INSERT INTO tb_users (username, name, email, encoded_password, description) VALUES
                         ('mentor', 'Ana Mentora', 'mentor@passport.vepo.dev', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==',
                          'Mentora e autora do curso DevOps com containers.');

    INSERT INTO tb_users_profiles (user_id, profile_id) 
    VALUES ((SELECT id FROM tb_users WHERE username = 'cto-boss'),
            (SELECT id FROM tb_profiles WHERE name = 'Domain Manager')),
           ((SELECT id FROM tb_users WHERE username = 'cto-boss'),
            (SELECT id FROM tb_profiles WHERE name = 'Domain Viewer')),
           ((SELECT id FROM tb_users WHERE username = 'cto-boss'),
            (SELECT id FROM tb_profiles WHERE name = 'Passport Admin'));

    INSERT INTO tb_users_profiles (user_id, profile_id)
    VALUES ((SELECT id FROM tb_users WHERE username = 'cto-boss'),
            (SELECT id FROM tb_profiles WHERE name = 'Cursos Admin'))
    ON CONFLICT (user_id, profile_id) DO NOTHING;

    -- Channel follows (Engage tb_channels.id = 1 from Engage seed)
    INSERT INTO tb_channel_follows (user_id, engage_channel_id)
    VALUES ((SELECT id FROM tb_users WHERE username = 'cto-boss'), 1);
END $$;
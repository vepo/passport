DO $$
BEGIN
    -- Usuário com apenas a role PROJECT_MANAGER
    INSERT INTO tb_users (username, name, email, encoded_password, roles) VALUES 
                         ('proj-leader', 'Project Lead', 'project_lead@morpho-board.ui', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==', '{PROJECT_MANAGER}');

    -- Usuário com apenas a role USER
    INSERT INTO tb_users (username, name, email, encoded_password, roles) VALUES 
                         ('junior', 'Junior Developer', 'junior_dev@morpho-board.ui', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==', '{USER}');

    -- Combinação ADMIN + PROJECT_MANAGER
    INSERT INTO tb_users (username, name, email, encoded_password, roles) VALUES 
                         ('project-boss', 'Director of Projects', 'director_projects@morpho-board.ui', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==', '{ADMIN,PROJECT_MANAGER}');

    -- Combinação ADMIN + USER
    INSERT INTO tb_users (username, name, email, encoded_password, roles) VALUES 
                         ('tech-lead', 'Tech Lead', 'tech_lead@morpho-board.ui', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==', '{ADMIN,USER}');

    -- Combinação PROJECT_MANAGER + USER
    INSERT INTO tb_users (username, name, email, encoded_password, roles) VALUES 
                         ('senior', 'Senior Developer', 'senior_dev@morpho-board.ui', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==', '{PROJECT_MANAGER,USER}');

    -- Combinação ADMIN + PROJECT_MANAGER + USER (Super Usuário)
    INSERT INTO tb_users (username, name, email, encoded_password, roles) VALUES 
                         ('cto-boss', 'Chief Technology Officer', 'cto@morpho-board.ui', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==', '{ADMIN,PROJECT_MANAGER,USER}');

    -- Usuário sem roles (se aplicável)
    INSERT INTO tb_users (username, name, email, encoded_password, roles) VALUES 
                         ('guest-user', 'Guest User', 'guest@morpho-board.ui', 'IwS3Mm4oGEfpwPDC3Vom20ViYgXhVCxHeBGr8aluY9tC9o668ghxJ2fMQQUwq+7GWJkzX1HguXOtdwVkblUzTw==', '{}');

END $$;
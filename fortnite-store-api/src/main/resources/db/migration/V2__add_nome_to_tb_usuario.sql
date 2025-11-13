-- Adiciona a coluna 'nome' à tabela de usuários
ALTER TABLE TB_USUARIO
    ADD nome VARCHAR2(255 CHAR);

UPDATE TB_USUARIO SET nome = 'Usuario' WHERE nome IS NULL;
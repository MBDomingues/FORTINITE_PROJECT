-- V1__create_tables.sql
-- Este script cria o schema inicial para a aplicação Fortnite Store API no Oracle.

-- 1. Tabela de Usuários
CREATE TABLE TB_USUARIO (
    id NUMBER(19) NOT NULL PRIMARY KEY,
    email VARCHAR2(255 CHAR) NOT NULL UNIQUE,
    senha VARCHAR2(255 CHAR) NOT NULL,
    creditos NUMBER(10) DEFAULT 10000 NOT NULL
);

-- Sequência para os IDs da TB_USUARIO
CREATE SEQUENCE USUARIO_SEQ START WITH 1 INCREMENT BY 1;


-- 2. Tabela de Cosméticos (povoada pela API)
CREATE TABLE TB_COSMETICO (
    id VARCHAR2(255 CHAR) NOT NULL PRIMARY KEY,
    nome VARCHAR2(255 CHAR),
    tipo VARCHAR2(100 CHAR),
    raridade VARCHAR2(100 CHAR),
    descricao VARCHAR2(1000 CHAR),
    url_imagem VARCHAR2(500 CHAR),
    preco NUMBER(10) DEFAULT 0 NOT NULL,
    is_new NUMBER(1) DEFAULT 0 NOT NULL,
    is_for_sale NUMBER(1) DEFAULT 0 NOT NULL,
    data_inclusao TIMESTAMP WITH TIME ZONE,
    is_bundle NUMBER(1) DEFAULT 0 NOT NULL,
    bundle_items_json CLOB
);


-- 3. Tabela de Itens Adquiridos (Tabela de Junção)
CREATE TABLE TB_ITEM_ADQUIRIDO (
    id NUMBER(19) NOT NULL PRIMARY KEY,
    usuario_id NUMBER(19) NOT NULL,
    cosmetico_id VARCHAR2(255 CHAR) NOT NULL,
    data_compra TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    unique_user_cosmetico_key VARCHAR2(512 CHAR) NOT NULL UNIQUE,

    -- Chave estrangeira para o usuário
   CONSTRAINT fk_item_usuario
       FOREIGN KEY (usuario_id)
           REFERENCES TB_USUARIO(id)
           ON DELETE CASCADE,

    -- Chave estrangeira para o cosmético
   CONSTRAINT fk_item_cosmetico
       FOREIGN KEY (cosmetico_id)
           REFERENCES TB_COSMETICO(id)
           ON DELETE CASCADE
);

-- Sequência para os IDs da TB_ITEM_ADQUIRIDO
CREATE SEQUENCE ITEM_ADQUIRIDO_SEQ START WITH 1 INCREMENT BY 1;


-- 4. Tabela de Histórico de Transações
CREATE TABLE TB_HISTORICO_TRANSACAO (
    id NUMBER(19) NOT NULL PRIMARY KEY,
    usuario_id NUMBER(19) NOT NULL,
    cosmetico_id VARCHAR2(255 CHAR),
    tipo VARCHAR2(50 CHAR) NOT NULL,
    valor NUMBER(10) NOT NULL,
    data_transacao TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,

    -- Chave estrangeira para o usuário
    CONSTRAINT fk_hist_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES TB_USUARIO(id)
            ON DELETE CASCADE,

    -- Chave estrangeira para o cosmético (opcional)
    CONSTRAINT fk_hist_cosmetico
        FOREIGN KEY (cosmetico_id)
            REFERENCES TB_COSMETICO(id)
            ON DELETE SET NULL,

    -- Garante que o tipo seja um dos valores esperados
    CONSTRAINT chk_tipo_transacao
        CHECK (tipo IN ('COMPRA', 'DEVOLUCAO'))
);

-- Sequência para os IDs da TB_HISTORICO_TRANSACAO
CREATE SEQUENCE HISTORICO_SEQ START WITH 1 INCREMENT BY 1;
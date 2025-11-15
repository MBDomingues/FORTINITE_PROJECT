-- Adiciona coluna para armazenar as cores (JSON)
ALTER TABLE TB_COSMETICO ADD cores_json VARCHAR2(1000 CHAR);

-- Garante que não seja nulo (opcional, coloca '[]' se for nulo)
UPDATE TB_COSMETICO SET cores_json = '[]' WHERE cores_json IS NULL;
CREATE TABLE USUARIO (
                         id BIGINT IDENTITY(1,1) PRIMARY KEY,
                         nome VARCHAR(150) NOT NULL,
                         email VARCHAR(150) NOT NULL UNIQUE,
                         senha_hash VARCHAR(255) NOT NULL,
                         perfil VARCHAR(30) NOT NULL,
                         criado_em DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE PASSAGEIRO (
                            id BIGINT IDENTITY(1,1) PRIMARY KEY,
                            cuidador_id BIGINT NOT NULL,
                            nome VARCHAR(150) NOT NULL,
                            tipo_necessidade VARCHAR(50) NOT NULL,
                            contato_emergencia VARCHAR(100) NOT NULL,
                            consentimento_em DATETIME2 NOT NULL,
                            CONSTRAINT fk_passageiro_cuidador FOREIGN KEY (cuidador_id) REFERENCES USUARIO(id)
);

CREATE TABLE CARTAO (
                        id BIGINT IDENTITY(1,1) PRIMARY KEY,
                        passageiro_id BIGINT NOT NULL,
                        uid_hash VARCHAR(255) NOT NULL UNIQUE,
                        ativo BIT NOT NULL DEFAULT 1,
                        CONSTRAINT fk_cartao_passageiro FOREIGN KEY (passageiro_id) REFERENCES PASSAGEIRO(id)
);

CREATE TABLE PARADA (
                        id BIGINT IDENTITY(1,1) PRIMARY KEY,
                        codigo_sptrans VARCHAR(50) NOT NULL UNIQUE,
                        nome VARCHAR(150) NOT NULL,
                        endereco VARCHAR(255) NOT NULL,
                        latitude DECIMAL(10,7),
                        longitude DECIMAL(10,7)
);

CREATE TABLE LINHA (
                       id BIGINT IDENTITY(1,1) PRIMARY KEY,
                       codigo_sptrans VARCHAR(50) NOT NULL UNIQUE,
                       letreiro VARCHAR(100) NOT NULL,
                       sentido VARCHAR(50) NOT NULL
);

CREATE TABLE ROTA_FREQUENTE (
                                id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                passageiro_id BIGINT NOT NULL,
                                parada_id BIGINT NOT NULL,
                                linha_id BIGINT NOT NULL,
                                apelido VARCHAR(100) NOT NULL,
                                CONSTRAINT fk_rota_passageiro FOREIGN KEY (passageiro_id) REFERENCES PASSAGEIRO(id),
                                CONSTRAINT fk_rota_parada FOREIGN KEY (parada_id) REFERENCES PARADA(id),
                                CONSTRAINT fk_rota_linha FOREIGN KEY (linha_id) REFERENCES LINHA(id)
);

CREATE TABLE TOTEM (
                       id BIGINT IDENTITY(1,1) PRIMARY KEY,
                       parada_id BIGINT NOT NULL,
                       identificador VARCHAR(50) NOT NULL UNIQUE,
                       token_hash VARCHAR(255) NOT NULL,
                       status VARCHAR(30) NOT NULL,
                       ultimo_sinal_em DATETIME2,
                       CONSTRAINT fk_totem_parada FOREIGN KEY (parada_id) REFERENCES PARADA(id)
);

CREATE TABLE VEICULO (
                         id BIGINT IDENTITY(1,1) PRIMARY KEY,
                         prefixo VARCHAR(30) NOT NULL UNIQUE,
                         linha_id BIGINT NOT NULL,
                         motorista_id BIGINT,
                         CONSTRAINT fk_veiculo_linha FOREIGN KEY (linha_id) REFERENCES LINHA(id),
                         CONSTRAINT fk_veiculo_motorista FOREIGN KEY (motorista_id) REFERENCES USUARIO(id)
);

CREATE TABLE SOLICITACAO_EMBARQUE (
                                      id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                      passageiro_id BIGINT,
                                      totem_id BIGINT NOT NULL,
                                      linha_id BIGINT,
                                      veiculo_id BIGINT,
                                      status VARCHAR(30) NOT NULL,
                                      chave_idempotencia VARCHAR(100) NOT NULL UNIQUE,
                                      mensagem_voz VARCHAR(500),
                                      previsao_chegada DATETIME2,
                                      criada_em DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                                      atualizada_em DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                                      CONSTRAINT fk_sol_passageiro FOREIGN KEY (passageiro_id) REFERENCES PASSAGEIRO(id),
                                      CONSTRAINT fk_sol_totem FOREIGN KEY (totem_id) REFERENCES TOTEM(id),
                                      CONSTRAINT fk_sol_linha FOREIGN KEY (linha_id) REFERENCES LINHA(id),
                                      CONSTRAINT fk_sol_veiculo FOREIGN KEY (veiculo_id) REFERENCES VEICULO(id)
);

CREATE TABLE AUDITORIA_SOLICITACAO (
                                       id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                       solicitacao_id BIGINT NOT NULL,
                                       operacao VARCHAR(30) NOT NULL,
                                       status_antigo VARCHAR(30),
                                       status_novo VARCHAR(30) NOT NULL,
                                       usuario_banco VARCHAR(100) NOT NULL,
                                       data_hora DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                                       CONSTRAINT fk_aud_solicitacao FOREIGN KEY (solicitacao_id) REFERENCES SOLICITACAO_EMBARQUE(id)
);
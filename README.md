# 🎮 Fortnite Store - Desafio Técnico Full Stack

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Oracle](https://img.shields.io/badge/Oracle-F80000?style=for-the-badge&logo=oracle&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![Azure](https://img.shields.io/badge/azure-%230072C6.svg?style=for-the-badge&logo=microsoftazure&logoColor=white)

Aplicação Web Full Stack desenvolvida como parte do processo seletivo para **Desenvolvedor Web** no **Sistema ESO**. O sistema simula uma loja virtual de cosméticos do jogo Fortnite, consumindo dados reais de uma API externa e gerenciando compras, créditos e usuários.

---

## 🚀 Deploy (Acesse Online)

A aplicação está rodando em infraestrutura de nuvem na **Microsoft Azure**:

🔗 **Acesse a Loja:** [http://130.213.12.104](http://130.213.12.104)
*(Frontend servido via Nginx e Backend via Docker na mesma instância)*

---

## 🛠️ Tecnologias Utilizadas

### Backend (API)
* **Java 21 (LTS):** Linguagem base.
* **Spring Boot 3:** Framework principal.
* **Spring Security + JWT:** Autenticação e Autorização Stateless.
* **Spring Data JPA (Hibernate):** Persistência de dados.
* **Flyway:** Versionamento e migração de banco de dados.
* **Oracle Database 21c XE:** Banco de dados relacional (rodando em container).

### Frontend (Cliente)
* **HTML5 & CSS3:** Estrutura e estilização.
* **JavaScript (Vanilla ES6+):** Lógica do cliente, consumo de API (Fetch) e gerenciamento de DOM.
* **Bootstrap 5:** Responsividade e componentes de UI.

### Infraestrutura & DevOps
* **Docker & Docker Compose:** Orquestração dos serviços (App + Banco).
* **Azure Virtual Machine (Linux Ubuntu):** Servidor de produção.
* **Nginx:** Servidor web reverso para o Frontend.

---

## 💡 Decisões Técnicas Relevantes

Durante o desenvolvimento, algumas decisões arquiteturais foram tomadas para garantir robustez e atender aos requisitos:

1.  **Estratégia de "Race Condition" (Oracle vs API):**
    * O banco Oracle em container demora para inicializar. Para evitar falhas na startup da API, foi implementado um mecanismo de `delay` assíncrono (`@Async`) no serviço de sincronização inicial e o uso de `depends_on` (healthchecks) no Docker Compose, garantindo que a aplicação só tente acessar o banco quando ele estiver 100% pronto.

2.  **Sincronização de Dados (Cron):**
    * Foi utilizado o `@Scheduled` do Spring para rodar uma tarefa automática a cada **1 hora**. Isso mantém a vitrine local sincronizada com a API oficial do Fortnite (que atualiza a loja diariamente), sem sobrecarregar o servidor externo a cada requisição de usuário.

3.  **Arquitetura Híbrida no Deploy:**
    * Para contornar restrições de *Mixed Content* (HTTPS vs HTTP) e CORS em ambientes de teste sem domínio SSL, optei por hospedar tanto o Frontend (via Nginx) quanto o Backend na mesma VM da Azure. Isso simplificou a rede, eliminou latência de conexão e garantiu que o sistema funcionasse de forma integrada.

4.  **Flyway para Migrations:**
    * Para garantir a integridade do schema do banco de dados (especialmente lidando com Oracle), desativei o `ddl-auto` do Hibernate e utilizei o **Flyway**. Isso garante que as tabelas (`TB_USUARIO`, `TB_COSMETICO`) sejam criadas de forma determinística e segura em qualquer ambiente.

---

## 💻 Como Rodar Localmente

O projeto foi 100% dockerizado para facilitar a execução em qualquer máquina.

### Pré-requisitos
* [Docker](https://www.docker.com/) e Docker Compose instalados.
* Git.

### Passo a Passo

1.  **Clone o repositório:**
    ```bash
    git clone [https://github.com/SEU_USUARIO/NOME_DO_REPO.git](https://github.com/SEU_USUARIO/NOME_DO_REPO.git)
    cd NOME_DA_PASTA
    ```

2.  **Inicie a aplicação:**
    Este comando irá baixar as imagens (Oracle e Java), compilar o projeto e iniciar os containers.
    *A primeira execução pode demorar alguns minutos devido ao download do Oracle.*
    ```bash
    docker-compose up --build
    ```

3.  **Aguarde a Inicialização:**
    * Acompanhe os logs. O Oracle levará um tempo para ficar "Healthy".
    * Após a API iniciar, aguarde cerca de **30 segundos** para a sincronização inicial dos itens.
    * Procure no log por: `SUCESSO: Sincronização base e status concluída`.

4.  **Acesse:**
    * **Frontend:** Abra a pasta `fortnite-frontend/index.html` (Recomendado usar Live Server do VS Code) ou ajuste a URL no `auth.js` para `localhost:8080`.
    * **API (Swagger/JSON):** [http://localhost:8080/api/v1/cosmeticos](http://localhost:8080/api/v1/cosmeticos)

---

## 🔌 Endpoints Principais

| Método | Rota | Descrição | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/cadastro` | Cria um novo usuário (ganha 10k V-Bucks) | Pública |
| `POST` | `/api/v1/auth/login` | Autentica e retorna Token JWT | Pública |
| `GET` | `/api/v1/cosmeticos` | Lista cosméticos (filtros e paginação) | Pública |
| `GET` | `/api/v1/perfis/me` | Retorna dados do usuário logado | **Token** |
| `GET` | `/api/v1/perfis/me/historico` | Histórico de transações do usuário | **Token** |

---

**Desenvolvido por Mateus [Seu Sobrenome]**

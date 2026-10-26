# E-commerce API

API REST para gerenciamento de um catálogo de produtos e processamento de pedidos, desenvolvida com Java e Spring Boot.

O projeto foi desenvolvido como parte do meu portfólio, com foco em boas práticas de desenvolvimento backend, modelagem de domínio, validação de dados, testes automatizados, documentação de API, migração de banco de dados, controle transacional e containerização.

## Tecnologias

- Java 21
- Spring Boot 4.1.1
- Spring Data JPA
- PostgreSQL 17
- Flyway
- Bean Validation
- OpenAPI / Swagger UI
- JUnit 5
- Mockito
- Testcontainers
- JaCoCo
- SpotBugs
- OWASP Dependency-Check
- Gradle
- Docker
- Docker Compose
- GitHub Actions

## Funcionalidades

- Criar e listar categorias
- Criar produtos vinculados a categorias
- Listar produtos com paginação e ordenação
- Buscar produto por ID
- Filtrar produtos por categoria e status de atividade
- Consultar estoque de produtos
- Adicionar ou reduzir estoque
- Atualizar informações de produtos
- Criar pedidos com múltiplos itens
- Listar pedidos com paginação
- Buscar pedido por ID
- Confirmar pedidos
- Cancelar pedidos
- Validar dados de entrada
- Impedir pedidos com produtos inativos ou estoque insuficiente
- Restaurar o estoque ao cancelar pedidos elegíveis
- Tratar exceções globalmente
- Gerenciar transações durante operações de negócio
- Versionar o esquema do banco de dados com Flyway

## Status dos pedidos

- `PENDING`
- `CONFIRMED`
- `CANCELLED`

## Operações de estoque

- `ADD` — adiciona unidades ao estoque
- `REDUCE` — reduz unidades do estoque, respeitando as regras de negócio

## Endpoints

### Categorias

| Método | Endpoint             | Descrição           |
| ------ | -------------------- | ------------------- |
| POST   | `/api/v1/categories` | Cria uma categoria  |
| GET    | `/api/v1/categories` | Lista as categorias |

### Produtos

| Método | Endpoint                      | Descrição                        |
| ------ | ----------------------------- | -------------------------------- |
| POST   | `/api/v1/products`            | Cria um produto                  |
| GET    | `/api/v1/products`            | Lista os produtos com paginação  |
| GET    | `/api/v1/products/{id}`       | Busca um produto por ID          |
| PUT    | `/api/v1/products/{id}`       | Atualiza um produto              |
| GET    | `/api/v1/products/{id}/stock` | Consulta o estoque de um produto |
| PATCH  | `/api/v1/products/{id}/stock` | Atualiza o estoque de um produto |

A listagem de produtos permite utilizar os seguintes parâmetros de consulta:

- `page`: número da página
- `size`: quantidade de registros por página
- `sort`: campo e direção da ordenação
- `categoryId`: filtra produtos por categoria
- `active`: filtra produtos pelo status de atividade

Exemplo:

```http
GET /api/v1/products?page=0&size=10&sort=price,asc&active=true
```

### Pedidos

| Método | Endpoint                      | Descrição                      |
| ------ | ----------------------------- | ------------------------------ |
| POST   | `/api/v1/orders`              | Cria um pedido                 |
| GET    | `/api/v1/orders`              | Lista os pedidos com paginação |
| GET    | `/api/v1/orders/{id}`         | Busca um pedido por ID         |
| PATCH  | `/api/v1/orders/{id}/confirm` | Confirma um pedido             |
| PATCH  | `/api/v1/orders/{id}/cancel`  | Cancela um pedido              |

## Executando localmente

### Pré-requisitos

- Java 21
- Docker
- Docker Compose

Clone o repositório:

```bash
git clone https://github.com/LuketeDev/ecommerce.git
cd ecommerce
```

### Configuração do ambiente

Crie o arquivo `.env` a partir do exemplo:

```bash
cp .env.example .env
```

Configure as variáveis necessárias no arquivo `.env`, de acordo com o `compose.yaml`.

Utilize credenciais apropriadas para o ambiente local. O arquivo `.env` não deve ser versionado.

### Executando com Docker Compose

Para construir a imagem da API e iniciar a aplicação e o PostgreSQL:

```bash
docker compose up --build -d
```

Confira o estado dos serviços:

```bash
docker compose ps
```

Consulte os logs da aplicação:

```bash
docker compose logs -f api
```

Para consultar os logs do banco de dados:

```bash
docker compose logs -f postgres
```

O serviço da API deve aguardar a disponibilidade do PostgreSQL. As migrações do banco são executadas automaticamente pelo Flyway durante a inicialização da aplicação.

A API estará disponível em:

`http://localhost:8080`

Para interromper os serviços:

```bash
docker compose down
```

Os dados do PostgreSQL são armazenados em um volume Docker nomeado. Não utilize `docker compose down -v` se quiser preservar os dados locais.

### Executando sem Docker para a aplicação

Se preferir executar a aplicação diretamente na máquina, primeiro inicie o PostgreSQL pelo Docker Compose:

```bash
docker compose up -d postgres
```

Depois, execute a aplicação.

Linux ou macOS:

```bash
./gradlew bootRun
```

Windows:

```bat
gradlew.bat bootRun
```

Nesse modo, as variáveis de ambiente necessárias à conexão devem estar disponíveis para o processo Java. Confira a configuração de datasource em `application.yaml`.

## Documentação

A documentação interativa da API está disponível pelo Swagger UI:

`http://localhost:8080/swagger-ui.html`

A especificação OpenAPI pode ser acessada em:

`http://localhost:8080/v3/api-docs`

## Testes

Para executar os testes automatizados:

```bash
./gradlew test
```

Os testes abrangem diferentes camadas da aplicação, incluindo controllers, serviços, repositórios, regras de negócio e integração com o banco de dados.

Os testes de integração que utilizam Testcontainers precisam de um ambiente Docker disponível.

O relatório de execução dos testes fica em:

`build/reports/tests/test/index.html`

### Cobertura de testes

Para gerar o relatório JaCoCo:

```bash
./gradlew jacocoTestReport
```

Relatórios:

- HTML: `build/reports/jacoco/test/html/index.html`
- XML: `build/reports/jacoco/test/jacocoTestReport.xml`

### Análise estática

Para executar o SpotBugs:

```bash
./gradlew spotbugsMain
```

O relatório HTML é gerado em `build/reports/spotbugs/main.html`, conforme a configuração do Gradle.

### Análise de vulnerabilidades

O OWASP Dependency-Check analisa as dependências do projeto em busca de vulnerabilidades conhecidas:

```bash
./gradlew dependencyCheckAnalyze
```

O comando utiliza os dados de vulnerabilidades do NVD. Para evitar limitações de acesso, configure `NVD_API_KEY` no ambiente local ou como um secret do GitHub Actions.

### Build

Para compilar e gerar o artefato executável:

```bash
./gradlew bootJar
```

## Integração contínua

O projeto utiliza GitHub Actions para executar verificações automatizadas em pushes e pull requests.

O pipeline inclui:

- Execução dos testes
- Geração do relatório de cobertura
- Análise estática com SpotBugs
- Análise de dependências com OWASP Dependency-Check
- Geração do artefato executável

A chave de acesso ao NVD deve ser configurada em `Settings → Secrets and variables → Actions` no repositório, utilizando o nome `NVD_API_KEY`.

## Banco de dados

O PostgreSQL é utilizado para persistência dos dados. O Flyway gerencia as migrações versionadas, enquanto o Hibernate valida o esquema do banco durante a inicialização da aplicação.

O projeto utiliza `ddl-auto: validate`, evitando que o Hibernate altere automaticamente a estrutura do banco de dados.

As alterações estruturais devem ser implementadas por novas migrações Flyway, preservando o histórico de mudanças no esquema.

## Estrutura do projeto

A aplicação é organizada em camadas para separar responsabilidades. A estrutura exata pode variar conforme a implementação:

```text
src/
├── main/
│   ├── java/
│   │   └── com/lukete/ecommerce/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── repository/
│   │       ├── domain/
│   │       ├── dto/
│   │       ├── exception/
│   │       └── ...
│   └── resources/
│       ├── db/migration/
│       └── application.yaml
└── test/
    └── java/
```

## Escopo e limitações

O projeto concentra-se no catálogo, no estoque e no processamento de pedidos.

Não fazem parte do escopo atual, salvo se implementados posteriormente:

- Integração com gateways de pagamento
- Processamento real de pagamentos
- Cálculo de frete
- Cupons e descontos promocionais
- Frontend
- Autenticação e autorização de clientes
- Integração com serviços externos de logística

A API é um projeto de portfólio e não deve ser considerada pronta para operação comercial sem uma revisão adicional de segurança, observabilidade, desempenho e requisitos operacionais.

## Objetivos de aprendizado

- Desenvolvimento de APIs REST com Java e Spring Boot
- Aplicação de princípios de orientação a objetos e separação de responsabilidades
- Implementação de regras de negócio e controle transacional
- Persistência de dados com Spring Data JPA
- Paginação, ordenação e filtros dinâmicos
- Testes unitários e de integração
- Documentação de APIs com OpenAPI
- Gerenciamento de migrações com Flyway
- Execução de infraestrutura com Docker Compose
- Análise estática e de vulnerabilidades
- Automação de verificações com GitHub Actions
- Organização de um projeto backend para portfólio profissional

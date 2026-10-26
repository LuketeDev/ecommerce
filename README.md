# E-commerce API

API REST para gerenciamento de um catálogo de produtos e processamento de pedidos, desenvolvida com Java e Spring Boot.

O projeto foi desenvolvido como parte do meu portfólio, com foco em boas práticas de desenvolvimento backend, modelagem de domínio, validação de dados, testes automatizados, documentação de API, migração de banco de dados e containerização.

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
- Gradle
- Docker Compose

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

`GET /api/v1/products?page=0&size=10&sort=price,asc&active=true`

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

Crie o arquivo `.env` a partir do exemplo disponibilizado:

```bash
cp .env.example .env
```

Configure as variáveis de ambiente no arquivo `.env`

Utilize uma senha adequada para o seu ambiente. O arquivo `.env` não deve ser versionado.

### Iniciando o banco de dados

Suba o PostgreSQL utilizando o Docker Compose:

```bash
docker compose up -d
```

Aguarde o banco de dados ficar disponível.

### Executando a aplicação

No Linux ou macOS:

```bash
./gradlew bootRun
```

No Windows:

```bat
gradlew.bat bootRun
```

A API estará disponível em:

`http://localhost:8080`

As migrações do banco de dados são executadas automaticamente pelo Flyway durante a inicialização da aplicação.

## Documentação

A documentação interativa da API está disponível através do Swagger UI:

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

Para gerar o relatório de testes, execute o comando acima e consulte os resultados em:

`build/reports/tests/test/index.html`

## Banco de dados

O PostgreSQL é utilizado para persistência dos dados. O Flyway gerencia as migrações versionadas, enquanto o Hibernate valida o esquema do banco durante a inicialização da aplicação.

O projeto utiliza `ddl-auto: validate`, evitando que o Hibernate altere automaticamente a estrutura do banco de dados.

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
- Organização de um projeto backend para portfólio profissional

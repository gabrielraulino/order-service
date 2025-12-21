# Order Service

Serviço responsável por **gerenciamento de pedidos**. Cria pedidos a partir de checkouts, gerencia status e processa cancelamentos.

## 📋 Visão Geral

O Order Service é responsável por:

- ✅ Criação de pedidos a partir de CheckoutEvent
- ✅ Listagem e busca de pedidos
- ✅ Cancelamento de pedidos
- ✅ Gerenciamento de status de pedidos
- ✅ Publicação de eventos RabbitMQ (UpdateStockEvent, OrderCancelledEvent)
- ✅ Processamento assíncrono de eventos
- ✅ Recuperação automática de falhas (cancelamento automático)

## 🚀 Executando

### Com Docker Compose

```bash
docker-compose up -d order-service
```

### Localmente

```bash
cd order
mvn spring-boot:run
```

**Dependências**: Service Discovery (Eureka), banco de dados PostgreSQL, RabbitMQ, Product Service e User Service devem estar rodando.

## 🌐 Acesso

- **Base URL**: http://localhost:8084
- **Porta padrão**: 8084
- **Swagger UI** (via Gateway): http://localhost:8080/order/swagger-ui.html

## 📡 Endpoints

### GET /orders/user

Retorna os pedidos do usuário atual.

**Autenticação**: USER ou ADMIN

**Response:**
```json
[
  {
    "id": 1,
    "userId": 1,
    "status": "PENDING",
    "paymentMethod": "CREDIT_CARD",
    "items": [
      {
        "productId": 1,
        "quantity": 2,
        "unitPrice": 3000.00,
        "totalPrice": 6000.00
      }
    ],
    "totalQuantity": 2,
    "totalPrice": 6000.00,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
]
```

### GET /orders/{id}

Busca um pedido por ID.

**Autenticação**: ADMIN

### GET /orders

Lista todos os pedidos com paginação.

**Autenticação**: ADMIN

**Query Parameters:**
- `page`: Número da página (padrão: 0)
- `size`: Tamanho da página (padrão: 20)
- `sort`: Campo para ordenação (padrão: id)

### POST /orders/{id}/cancel

Cancela um pedido.

**Autenticação**: USER ou ADMIN (usuários só podem cancelar próprios pedidos)

**Response:**
```json
{
  "id": 1,
  "userId": 1,
  "status": "CANCELLED",
  "paymentMethod": "CREDIT_CARD",
  "items": [...],
  "totalQuantity": 2,
  "totalPrice": 6000.00,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:05:00"
}
```

**Regras**:
- Apenas pedidos com status PENDING ou PROCESSING podem ser cancelados
- Publica `OrderCancelledEvent` para restaurar estoque

## 🔄 Eventos RabbitMQ

### CheckoutEvent (Queue: `cart.checkout`) - Consumidor

Recebido quando um checkout é realizado no Cart Service.

**Payload:**
```json
{
  "cartId": 1,
  "userId": 1,
  "paymentMethod": "CREDIT_CARD",
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ]
}
```

**Ação**:
1. Cria pedido com status PENDING
2. Publica `UpdateStockEvent` para atualizar estoque
3. Publica `OrderCreatedEvent` para iniciar processamento

### StockUpdateFailedEvent (Queue: `order.stock-update-failed`) - Consumidor

Recebido quando Product Service falha ao atualizar estoque.

**Payload:**
```json
{
  "orderId": 1,
  "userId": 1,
  "productQuantities": {
    "1": 2
  },
  "errorMessage": "Insufficient stock"
}
```

**Ação**: Cancela automaticamente o pedido

### Eventos Publicados

#### UpdateStockEvent (Queue: `product.update-stock`)

Publicado após criar pedido para atualizar estoque.

#### OrderCancelledEvent (Queue: `order.cancelled`)

Publicado quando pedido é cancelado para restaurar estoque.

#### OrderCreatedEvent (Queue: `order.created`)

Publicado após criar pedido para iniciar processamento.

## ⚙️ Configuração

### application.properties

```properties
spring.application.name=order
server.port=8084

# Database
spring.datasource.url=jdbc:postgresql://localhost:5435/ms-order
spring.datasource.username=postgres
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.jpa.hibernate.ddl-auto=update

# RabbitMQ
spring.rabbitmq.addresses=${SPRING_RABBITMQ_ADDRESSES}

# Queue names
broker.queue.order.checkout.name=cart.checkout
broker.queue.order.cancelled.name=order.cancelled
broker.queue.order.stock-update-failed.name=order.stock-update-failed
broker.queue.order.created.name=order.created

# Eureka
eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka/
```

### Variáveis de Ambiente

- `SERVER_PORT`: Porta do serviço (padrão: 8084)
- `SPRING_DATASOURCE_URL`: URL do banco de dados PostgreSQL
- `SPRING_DATASOURCE_USERNAME`: Usuário do banco
- `SPRING_DATASOURCE_PASSWORD`: Senha do banco
- `SPRING_RABBITMQ_ADDRESSES`: URL completa do RabbitMQ (obrigatório)
- `EUREKA_SERVER_URL`: URL do Eureka Server

## 🗄️ Banco de Dados

### Tabela: orders

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | ID único do pedido |
| user_id | BIGINT | ID do usuário |
| status | VARCHAR | Status (PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED) |
| payment_method | VARCHAR | Método de pagamento |
| created_at | TIMESTAMP | Data de criação |
| updated_at | TIMESTAMP | Data de atualização |

### Tabela: order_items

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | BIGSERIAL | ID único do item |
| order_id | BIGINT | ID do pedido (FK) |
| product_id | BIGINT | ID do produto |
| quantity | INTEGER | Quantidade |

## 📁 Estrutura do Projeto

```
order/
├── src/main/java/com/ms/order/
│   ├── OrderApplication.java
│   ├── controller/
│   │   └── OrderController.java        # Endpoints REST
│   ├── service/
│   │   ├── OrderService.java           # Lógica de negócio
│   │   └── OrderProcessingService.java # Processamento de pedidos
│   ├── repository/
│   │   └── OrderRepository.java        # JPA Repository
│   ├── model/
│   │   ├── Order.java                  # Entidade JPA
│   │   ├── OrderItem.java              # Entidade JPA
│   │   └── OrderStatus.java            # Enum de status
│   ├── dto/
│   │   ├── OrderDTO.java               # DTO de resposta
│   │   ├── OrderItemDTO.java           # DTO de item
│   │   ├── CreateOrderDTO.java         # DTO de criação
│   │   ├── CheckoutEvent.java          # Evento de checkout
│   │   ├── UpdateStockEvent.java       # Evento de atualização de estoque
│   │   ├── OrderCancelledEvent.java    # Evento de cancelamento
│   │   └── StockUpdateFailedEvent.java # Evento de falha
│   ├── consumer/
│   │   └── OrderConsumer.java          # Consumidores RabbitMQ
│   ├── producer/
│   │   └── OrderProducer.java          # Publicadores RabbitMQ
│   ├── client/
│   │   ├── UserService.java            # Feign Client para User Service
│   │   └── ProductService.java         # Feign Client para Product Service
│   ├── config/
│   │   └── SecurityConfig.java         # Configuração de segurança
│   ├── auth/
│   │   ├── GatewayAuthenticationFilter.java
│   │   └── CurrentUserService.java
│   └── exception/
│       ├── ResourceNotFoundException.java
│       └── InvalidOperationException.java
└── src/main/resources/
    └── application.properties
```

## 🔑 Componentes Principais

### OrderService

Lógica de negócio principal:

- `createOrder()`: Cria novo pedido
- `findById()`, `findByUserId()`, `getAllOrders()`: Busca pedidos
- `cancelOrder()`: Cancela pedido e publica evento
- `handleStockUpdateFailure()`: Cancela pedido automaticamente em caso de falha

### OrderConsumer

Consumidor RabbitMQ:

- `handleCheckoutEvent()`: Processa checkout e cria pedido
- Consome `StockUpdateFailedEvent` para cancelamento automático

### OrderProducer

Publicador RabbitMQ:

- `publishUpdateStockEvent()`: Publica evento para atualizar estoque
- `publishOrderCancelledEvent()`: Publica evento de cancelamento
- `publishOrderCreatedEvent()`: Publica evento de criação

### OrderProcessingService

Serviço de processamento de pedidos:

- Processa workflow de pedidos (PENDING → PROCESSING → SHIPPED → DELIVERED)

## 🔗 Comunicação com Outros Serviços

### User Service (Síncrono - Feign)

- Validação de usuário ao criar pedido

### Product Service (Síncrono - Feign)

- Busca informações de produtos para calcular totais

### Product Service (Assíncrono - RabbitMQ)

- Publica `UpdateStockEvent` para atualizar estoque
- Publica `OrderCancelledEvent` para restaurar estoque
- Recebe `StockUpdateFailedEvent` para cancelamento automático

## 🛠️ Tecnologias

- **Spring Boot 3.5.8**
- **Spring Data JPA** - Persistência
- **PostgreSQL** - Banco de dados
- **RabbitMQ** - Message broker (eventos assíncronos)
- **Spring AMQP** - Integração com RabbitMQ
- **Spring Cloud OpenFeign** - Comunicação síncrona
- **Spring Security** - Autenticação e autorização
- **Spring Cloud Netflix Eureka Client** - Service Discovery

## 📝 Exemplos de Uso

### Listar Pedidos do Usuário

```bash
curl -X GET http://localhost:8080/order/orders/user \
  -H "Authorization: Bearer <token>"
```

### Cancelar Pedido

```bash
curl -X POST http://localhost:8080/order/orders/1/cancel \
  -H "Authorization: Bearer <token>"
```

### Listar Todos os Pedidos (Admin)

```bash
curl -X GET "http://localhost:8080/order/orders?page=0&size=20" \
  -H "Authorization: Bearer <admin-token>"
```

## 🔒 Segurança

- Usuários só podem ver/cancelar próprios pedidos
- Admins podem ver/cancelar todos os pedidos
- Validação de status antes de cancelar

## 📊 Status dos Pedidos

- **PENDING**: Pedido criado, aguardando processamento
- **PROCESSING**: Pedido em processamento
- **SHIPPED**: Pedido enviado
- **DELIVERED**: Pedido entregue
- **CANCELLED**: Pedido cancelado

---

**📦 Serviço de gerenciamento de pedidos**


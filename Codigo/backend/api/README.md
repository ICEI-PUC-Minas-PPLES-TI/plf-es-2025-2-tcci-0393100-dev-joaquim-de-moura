# MobU — Backend API

API REST e WebSocket do sistema MobU, desenvolvida com **NestJS**, **Prisma ORM** e **PostgreSQL**.

## Tecnologias

- [NestJS](https://nestjs.com/) — framework Node.js modular
- [Prisma](https://www.prisma.io/) — ORM com migrações versionadas
- [PostgreSQL 16](https://www.postgresql.org/) — banco de dados relacional
- [JWT](https://jwt.io/) — autenticação stateless
- [WebSocket (ws)](https://github.com/websockets/ws) — comunicação em tempo real
- [Firebase Admin SDK](https://firebase.google.com/) — notificações push (FCM)
- [Google Maps Directions API](https://developers.google.com/maps) — cálculo de rotas

---

## Pré-requisitos

- Node.js 20+
- Docker e Docker Compose
- Chave de API do Google Maps (servidor)
- Projeto Firebase com Cloud Messaging habilitado

---

## Configuração do ambiente

```bash
cp .env.example .env
```

Preencha as variáveis no `.env`:

| Variável | Descrição |
|---|---|
| `DATABASE_URL` | URL de conexão com o PostgreSQL |
| `JWT_SECRET` | Segredo para assinatura dos tokens JWT |
| `GOOGLE_MAPS_API_KEY` | Chave da API do Google Maps (servidor) |
| `CORS_ORIGIN` | Origens permitidas pelo CORS (ex: `http://localhost:3001`) |
| `SMTP_HOST` / `SMTP_USER` / `SMTP_PASS` | Configuração de e-mail para verificação |

Adicione também o arquivo `firebase-service-account.json` na raiz desta pasta com as credenciais do Firebase Admin SDK.

---

## Executando

```bash
# Sobe o banco de dados (da pasta Codigo/)
docker compose up -d

# Instala dependências
npm install

# Aplica as migrações no banco
npx prisma migrate deploy

# Popula dados iniciais (admin + tarifa padrão)
npx prisma db seed

# Modo desenvolvimento (hot reload)
npm run start:dev

# Modo produção
npm run build
npm run start:prod
```

A API ficará disponível em `http://localhost:3000`.

---

## Testes

```bash
# Testes unitários (TU1–TU9)
npm run test

# Testes de integração (TI1–TI6)
npm run test:integration

# Testes de aceitação (TA1–TA15)
npm run test:acceptance

# Cobertura
npm run test:cov
```

### Cobertura de testes

| Suite | Identificadores | Arquivo |
|---|---|---|
| Unitários | TU1–TU4 | `src/auth/auth.service.spec.ts` |
| Unitários | TU5 | `src/driver/driver.service.spec.ts` |
| Unitários | TU6–TU9 | `src/ride/ride.service.spec.ts` |
| Integração | TI1–TI6 | `test/integration/` |
| Aceitação | TA1–TA15 | `test/acceptance/` |
| Limitações conhecidas | KL-01–KL-05 | `src/known-limitations.spec.ts` |

---

## Módulos

| Módulo | Responsabilidade |
|---|---|
| `auth` | Registro, login, verificação de telefone/e-mail, recuperação de senha |
| `ride` | Criação, estado, pagamento e segurança das corridas |
| `driver` | Perfil, status online, localização, histórico e financeiro do motorista |
| `admin` | Aprovação de motoristas, tarifas, regiões, relatórios e painel ao vivo |
| `payment` | Confirmação de pagamento e atualização de status |
| `review` | Avaliações de motoristas por passageiros |
| `chat` | Mensagens entre passageiro e motorista durante a corrida |
| `support` | Chamados de suporte |
| `realtime` | WebSocket com autenticação JWT e assinaturas por corrida |
| `notification` | Push notifications via Firebase Cloud Messaging |
| `maps` | Integração com Google Directions API |

---

## Endpoints principais

| Método | Rota | Descrição |
|---|---|---|
| POST | `/auth/register` | Cadastro de passageiro |
| POST | `/auth/register-driver` | Cadastro de motorista |
| POST | `/auth/login` | Login (retorna JWT) |
| GET | `/auth/me` | Perfil do usuário autenticado |
| POST | `/rides/estimate` | Estimativa de tarifa e rota |
| POST | `/rides` | Solicitar corrida |
| GET | `/rides/:id` | Detalhes da corrida |
| POST | `/driver/rides/:id/accept` | Motorista aceita corrida |
| PATCH | `/driver/rides/:id/finish` | Motorista finaliza corrida |
| GET | `/admin/live` | Operação ao vivo (corridas e motoristas) |
| GET | `/admin/reports` | Relatórios operacionais |

WebSocket disponível em `ws://localhost:3000/ws?token=<JWT>`.

---

## Banco de dados

As migrações ficam em `prisma/migrations/`. Para criar uma nova migração após alterar o schema:

```bash
npx prisma migrate dev --name descricao_da_mudanca
```

Para visualizar o banco graficamente:

```bash
npx prisma studio
```

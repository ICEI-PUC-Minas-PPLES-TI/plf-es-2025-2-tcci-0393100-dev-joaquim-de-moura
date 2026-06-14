# Código — MobU

Este diretório contém o código-fonte dos três componentes do sistema MobU.

## Estrutura

```
Codigo/
├── backend/
│   └── api/            API REST + WebSocket (NestJS + Prisma + PostgreSQL)
├── dashboard/
│   └── admin/          Painel administrativo web (Next.js)
├── MobULite/           Aplicativo Android (Kotlin + Jetpack Compose)
└── docker-compose.yml  Banco de dados PostgreSQL 16 via Docker
```

## Inicialização rápida

```bash
# 1. Sobe o banco de dados
docker compose up -d

# 2. Backend (porta 3000)
cd backend/api && cp .env.example .env && npm install && npx prisma migrate deploy && npm run start:dev

# 3. Dashboard (porta 3001)
cd dashboard/admin && npm install && npm run dev
```

O app Android é executado pelo Android Studio a partir da pasta `MobULite/`.

Consulte os READMEs individuais de cada componente para instruções detalhadas.

[![Open in Codespaces](https://classroom.github.com/assets/launch-codespace-2972f46106e565e64193e422d61a12cf1da4916b45550586e14ef0a7c637dd04.svg)](https://classroom.github.com/open-in-codespaces?assignment_repo_id=20559840)

# MobU — Plataforma de Transporte Urbano sob Demanda

O **MobU** é uma plataforma completa de transporte urbano desenvolvida como Trabalho de Conclusão de Curso em Engenharia de Software pela PUC Minas. O sistema conecta passageiros, motoristas e administradores em um fluxo integrado de solicitação, execução e pagamento de corridas, projetado para atender cidades de pequeno e médio porte.

## Alunos integrantes da equipe

- Joaquim de Moura Thomaz Neto

## Professores responsáveis

- Cleiton Silva Tavares
- Danilo de Quadros Maia Filho
- Leonardo Vilela Cardoso
- Raphael Ramos Dias Costa

---

## Visão Geral da Arquitetura

O sistema é composto por três componentes principais:

| Componente | Tecnologia | Descrição |
|---|---|---|
| **Backend API** | NestJS + Prisma + PostgreSQL | API REST + WebSocket para todos os fluxos do sistema |
| **App Mobile** | Android (Kotlin + Jetpack Compose) | Aplicativo para passageiros e motoristas |
| **Dashboard Admin** | Next.js (TypeScript) | Painel web para administração da plataforma |

---

## Pré-requisitos

- [Node.js](https://nodejs.org/) 20+
- [Docker](https://www.docker.com/) e Docker Compose
- [Android Studio](https://developer.android.com/studio) (para o app mobile)
- Conta no [Google Cloud](https://console.cloud.google.com/) com Maps API habilitada
- Conta no [Firebase](https://firebase.google.com/) com Cloud Messaging habilitado

---

## Como executar o projeto

### 1. Banco de dados (Docker)

```bash
cd Codigo
docker compose up -d
```

O PostgreSQL ficará disponível em `localhost:5432`.

### 2. Backend API

```bash
cd Codigo/backend/api
cp .env.example .env
# Preencha as variáveis no .env (DATABASE_URL, JWT_SECRET, GOOGLE_MAPS_API_KEY, etc.)

npm install
npx prisma migrate deploy
npx prisma db seed        # cria usuário admin e configuração de tarifa inicial
npm run start:dev
```

A API estará disponível em `http://localhost:3000`.

### 3. Dashboard Admin

```bash
cd Codigo/dashboard/admin
npm install
npm run dev
```

O painel estará disponível em `http://localhost:3001`.

Login padrão (após seed): telefone `31999000000` / senha `Senha@123`.

### 4. App Android

1. Abra a pasta `Codigo/MobULite` no Android Studio.
2. Copie `local.properties.example` para `local.properties` e preencha a chave do Google Maps.
3. Execute em um emulador ou dispositivo físico.
4. Configure o endereço da API em `RetrofitClient.kt` apontando para o IP da máquina que executa o backend.

---

## Estrutura do repositório

```
.
├── Artefatos/          Documentos e diagramas produzidos ao longo do TCC
├── Codigo/             Código-fonte dos três componentes do sistema
│   ├── backend/api/    API NestJS
│   ├── dashboard/      Painel administrativo Next.js
│   └── MobULite/       Aplicativo Android
├── Divulgacao/         Apresentações e vídeos de entrega
└── Documentacao/       Documentação de projeto (PDF)
```

---

## Funcionalidades principais

**Passageiro**
- Cadastro com CPF, data de nascimento e contato de emergência
- Estimativa de tarifa e rota antes de solicitar a corrida
- Acompanhamento do motorista em tempo real no mapa
- Pagamento via PIX off-line ou dinheiro
- Histórico de corridas e avaliação de motoristas
- Chat com o motorista durante a corrida

**Motorista**
- Cadastro com CNH e dados do veículo (aprovação pelo admin)
- Recebimento de corridas por notificação push e WebSocket
- Marcação de etapas: a caminho → chegou → em corrida → finalizado
- Histórico financeiro com ciclos de cobrança semanais
- Solicitação de pagamento à plataforma

**Administrador**
- Aprovação e rejeição de cadastros de motoristas
- Monitoramento ao vivo de corridas e motoristas online
- Relatórios financeiros e operacionais
- Configuração de tarifas e regiões de operação
- Gestão de cupons de desconto e chamados de suporte

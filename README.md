# MobU — Plataforma de Transporte Urbano sob Demanda

<p align="center">
  <img src="Codigo/MobULite/Logo1.png" alt="Logo MobU" width="180"/>
</p>

O **MobU** é uma plataforma completa de transporte urbano desenvolvida como Trabalho de Conclusão de Curso em Engenharia de Software pela PUC Minas. O sistema conecta passageiros, motoristas e administradores em um fluxo integrado de solicitação, execução e pagamento de corridas, projetado para atender cidades de pequeno e médio porte.

## Alunos integrantes da equipe

- Joaquim de Moura Thomaz Neto

## Professores responsáveis

- Cleiton Silva Tavares
- Danilo de Quadros Maia Filho
- Leonardo Vilela Cardoso
- Raphael Ramos Dias Costa
- Ramon Lacerda Marques

---

## Índice

1. [Arquitetura do Sistema](#arquitetura-do-sistema)
2. [Tecnologias Utilizadas](#tecnologias-utilizadas)
3. [Telas da Aplicação](#telas-da-aplicação)
4. [Pré-requisitos](#pré-requisitos)
5. [Dependências Externas](#dependências-externas-google-maps-e-firebase)
6. [Como Executar o Projeto](#como-executar-o-projeto)
7. [Variáveis de Ambiente](#variáveis-de-ambiente)
8. [Problemas Comuns](#problemas-comuns-troubleshooting)
9. [Estrutura do Repositório](#estrutura-do-repositório)
10. [Funcionalidades](#funcionalidades-principais)
11. [Documentação Adicional](#documentação-adicional)

---

## Arquitetura do Sistema

O MobU adota uma **arquitetura cliente-servidor** composta por três clientes e um backend central, seguindo o padrão em camadas *UI → API (regras de negócio) → Data*:

```
┌─────────────────┐   ┌─────────────────┐   ┌──────────────────┐
│   App Android    │   │  Dashboard Web   │   │  Serviços externos│
│ (Passageiro e    │   │ (Administrador)  │   │  Google Maps API  │
│  Motorista)      │   │    Next.js       │   │  Firebase FCM     │
└────────┬────────┘   └────────┬────────┘   └────────┬─────────┘
         │  HTTP/REST + WebSocket │                    │
         └───────────┬───────────┘                    │
                     ▼                                │
           ┌──────────────────┐                       │
           │    MobU API       │◄──────────────────────┘
           │  NestJS (porta    │
           │  3000)            │
           │  Controllers →    │
           │  Services → Data  │
           └────────┬─────────┘
                    │ Prisma ORM
                    ▼
           ┌──────────────────┐
           │  PostgreSQL 16    │
           │  (Docker, 5432)   │
           └──────────────────┘
```

- **API (NestJS)**: expõe endpoints REST e um gateway WebSocket para eventos em tempo real (posição do motorista, status da corrida, chat). Organizada em módulos: `auth`, `ride`, `driver`, `payment`, `chat`, `rating`, `review`, `notification`, `realtime`, `maps`, `admin`, `support` e `config`.
- **Banco de dados (PostgreSQL + Prisma)**: 16 entidades, com migrações versionadas e seed inicial.
- **App Android (Kotlin + Jetpack Compose)**: um único aplicativo com fluxos de passageiro e motorista.
- **Dashboard (Next.js)**: painel administrativo com mapa ao vivo, aprovações, tarifas, relatórios e suporte.

### Diagramas

| Diagrama | Arquivo |
|---|---|
| Casos de Uso | [`Artefatos/TCC2/Figura01_DiagramaCasosDeUso.png`](Artefatos/TCC2/Figura01_DiagramaCasosDeUso.png) |
| Entidade-Relacionamento (DER) | [`Artefatos/TCC2/Figura67_DER_DiagramaEntidadeRelacionamento.png`](Artefatos/TCC2/Figura67_DER_DiagramaEntidadeRelacionamento.png) |
| Classes (Controllers, Services, Models, Repositories, DTOs) | [`Artefatos/TCC2/`](Artefatos/TCC2/) — Figuras 12 a 17 |
| Sequência e Comunicação (corrida, pagamento, chat, aprovação...) | [`Artefatos/TCC2/`](Artefatos/TCC2/) — Figuras 02 a 34 |

A documentação completa do projeto (modelos, testes e cronograma) está em [`Documentacao/`](Documentacao/).

---

## Tecnologias Utilizadas

| Camada | Tecnologia | Justificativa |
|---|---|---|
| Backend | [NestJS](https://nestjs.com/) (Node.js + TypeScript) | Estrutura modular e injeção de dependências, facilitando testes e manutenção |
| ORM | [Prisma](https://www.prisma.io/) | Migrações versionadas e tipos TypeScript gerados a partir do schema |
| Banco de dados | PostgreSQL 16 (via Docker) | Conformidade ACID, essencial para registros financeiros |
| App mobile | Android nativo — Kotlin + [Jetpack Compose](https://developer.android.com/jetpack/compose) | Melhor integração com Google Maps SDK e FCM; desempenho em tempo real |
| Dashboard | [Next.js](https://nextjs.org/) (React + TypeScript) | SSR + ecossistema React para painéis com dados dinâmicos |
| Tempo real | WebSocket (Socket.IO) | Posição do motorista, status da corrida e chat |
| Notificações | Firebase Cloud Messaging (FCM) | Push notifications no Android |
| Mapas e rotas | Google Maps / Directions API | Cálculo de rotas, distância e tempo para precificação |
| Autenticação | JWT + BCrypt | Tokens de sessão e hash seguro de senhas |
| Testes | Jest (unitários, integração, e2e e aceitação) | Suítes separadas em `backend/api/test/` |
| Infraestrutura | Docker / Docker Compose | Paridade entre ambientes de desenvolvimento e produção |

---

## Telas da Aplicação

Todas as telas estão disponíveis em alta resolução em [`Artefatos/TCC2/`](Artefatos/TCC2/) (Figuras 43 a 66).

| Passageiro | Motorista | Administrador |
|---|---|---|
| ![Home do Passageiro](Artefatos/TCC2/Figura62_Interface_Passageiro_Home.png) | ![Painel do Motorista](Artefatos/TCC2/Figura55_Interface_Motorista_PainelHome.png) | ![Dashboard Admin](Artefatos/TCC2/Figura46_Interface_Admin_DashboardPrincipal.png) |
| Solicitação com estimativa de tarifa | Painel com ganhos e turno online | Dashboard com indicadores |
| ![Acompanhamento](Artefatos/TCC2/Figura64_Interface_Passageiro_AcompanhamentoCorrida.png) | ![Nova Corrida](Artefatos/TCC2/Figura56_Interface_Motorista_NovaCorrida.png) | ![Operação ao Vivo](Artefatos/TCC2/Figura47_Interface_Admin_OperacaoAoVivo.png) |
| Acompanhamento em tempo real | Recebimento de corrida | Mapa de operação ao vivo |

---

## Pré-requisitos

| Ferramenta | Versão | Uso |
|---|---|---|
| [Node.js](https://nodejs.org/) | 20+ | Backend e dashboard |
| [Docker](https://www.docker.com/) + Docker Compose | — | Banco de dados PostgreSQL |
| [Android Studio](https://developer.android.com/studio) | — | Compilar e executar o app mobile |
| Conta [Google Cloud](https://console.cloud.google.com/) | — | Chaves da Maps API |
| Conta [Firebase](https://firebase.google.com/) | — | Cloud Messaging (push) |

---

## Dependências Externas (Google Maps e Firebase)

### Google Maps

O sistema usa **duas chaves** distintas:

1. **Chave de servidor** (backend, variável `GOOGLE_MAPS_API_KEY` no `.env`): habilite **Directions API** e **Geocoding API** no Google Cloud Console. Sem restrição de aplicativo (ou restrita por IP).
2. **Chave Android** (app, em `local.properties`): habilite **Maps SDK for Android**. Restrinja pela assinatura do app (SHA-1) em produção.

> Sem a chave de servidor, a estimativa de corrida falha com `GOOGLE_MAPS_API_KEY não configurada no .env`.

### Firebase (notificações push)

1. Crie um projeto no [console do Firebase](https://console.firebase.google.com/) e adicione um app Android.
2. Baixe o `google-services.json` e coloque em `Codigo/MobULite/app/`.
3. Gere uma **service account** (Configurações do projeto → Contas de serviço → Gerar nova chave privada) e configure no backend:
   - **Opção 1**: salve o JSON como `firebase-service-account.json` na raiz de `Codigo/backend/api/`; ou
   - **Opção 2**: cole o JSON na variável `FIREBASE_SERVICE_ACCOUNT_JSON` do `.env`.
4. Preencha `FIREBASE_PROJECT_ID` no `.env`.

> Sem o Firebase configurado o sistema funciona normalmente, porém sem notificações push (os eventos em tempo real via WebSocket continuam funcionando).

---

## Como Executar o Projeto

### 1. Banco de dados (Docker)

```bash
cd Codigo
docker compose up -d
```

O PostgreSQL 16 ficará disponível em `localhost:5432`. Confirme com `docker ps`.

### 2. Backend (API NestJS)

**a) Crie o arquivo `.env`:**

```bash
cd Codigo/backend/api
cp .env.example .env
```

Edite o `.env` e preencha ao menos as variáveis obrigatórias (veja a [tabela completa](#variáveis-de-ambiente)):

```env
DATABASE_URL="postgresql://USER:PASSWORD@127.0.0.1:5432/mobu?schema=public"
JWT_SECRET="um-segredo-forte-com-pelo-menos-32-caracteres"
GOOGLE_MAPS_API_KEY="sua-chave-de-servidor-do-google-maps"
```

**b) Instale as dependências e configure o Prisma:**

```bash
npm install                  # instala dependências (também executa prisma generate)
npx prisma generate          # gera o cliente Prisma a partir do schema (se necessário)
npx prisma migrate deploy    # aplica as migrações, criando as tabelas no banco
npx prisma db seed           # popula o banco: usuário admin + configuração de tarifa
```

O que o **seed** cria:
- Usuário **administrador**: telefone `31999000000` / senha `Senha@123` (⚠️ altere em produção);
- **PricingConfig** ativa (tarifa base, R$/km, R$/min, taxa da plataforma) — sem ela a estimativa de corrida falha;
- Região de operação inicial.

**c) Inicie a API:**

```bash
npm run start:dev
```

A API estará em `http://localhost:3000`. Testes (opcional):

```bash
npm test              # unitários
npm run test:e2e      # ponta a ponta
```

### 3. Dashboard Admin (Next.js)

```bash
cd Codigo/dashboard/admin
cp .env.local.example .env.local   # NEXT_PUBLIC_API_URL="http://127.0.0.1:3000"
npm install
npm run dev
```

Painel em `http://localhost:3001`. Login padrão (após seed): telefone `31999000000` / senha `Senha@123`.

### 4. App Android

1. Abra a pasta `Codigo/MobULite` no Android Studio.
2. Copie `local.properties.example` para `local.properties` e preencha:
   ```properties
   sdk.dir=/caminho/para/Android/sdk
   GOOGLE_MAPS_API_KEY=sua-chave-android-do-google-maps
   MOBU_API_BASE_URL=http://10.0.2.2:3000/
   ```
   - **Emulador**: use `http://10.0.2.2:3000/` (alias do localhost da máquina);
   - **Dispositivo físico**: use o IP da máquina na rede local (ex.: `http://192.168.0.10:3000/`) — celular e computador no mesmo Wi-Fi.
3. Coloque o `google-services.json` do Firebase em `app/` (para push).
4. Execute (▶) em um emulador ou dispositivo com Android 8.0 (API 26) ou superior.

---

## Variáveis de Ambiente

### Backend (`Codigo/backend/api/.env`)

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DATABASE_URL` | ✅ | String de conexão do PostgreSQL |
| `JWT_SECRET` | ✅ | Segredo (32+ caracteres) para assinar tokens JWT |
| `GOOGLE_MAPS_API_KEY` | ✅ | Chave de **servidor** do Google Maps (Directions/Geocoding) |
| `CORS_ORIGIN` | opcional | Origens permitidas (default: `http://localhost:3001`) |
| `HOST` / `PORT` | opcional | Endereço e porta da API (default: `0.0.0.0:3000`) |
| `SMTP_HOST/PORT/USER/PASS/FROM` | opcional | Envio de e-mails (sem isto, e-mails são apenas logados) |
| `VERIFICATION_OTP_PEPPER` | opcional | Pepper dos códigos OTP (fallback: `JWT_SECRET`) |
| `TWILIO_ACCOUNT_SID/AUTH_TOKEN/FROM_NUMBER` | opcional | Envio de SMS (sem isto, SMS é logado no console) |
| `FIREBASE_PROJECT_ID` / `FIREBASE_SERVICE_ACCOUNT_JSON` | opcional | Notificações push via FCM |

### Dashboard (`Codigo/dashboard/admin/.env.local`)

| Variável | Descrição |
|---|---|
| `NEXT_PUBLIC_API_URL` | URL da API (ex.: `http://127.0.0.1:3000`) |

### App Android (`Codigo/MobULite/local.properties`)

| Variável | Descrição |
|---|---|
| `sdk.dir` | Caminho do Android SDK |
| `GOOGLE_MAPS_API_KEY` | Chave **Android** do Google Maps (Maps SDK) |
| `MOBU_API_BASE_URL` | URL da API (`http://10.0.2.2:3000/` no emulador) |

---

## Problemas Comuns (Troubleshooting)

| Problema | Causa provável / solução |
|---|---|
| `docker compose up` falha | Docker Desktop não está aberto. Inicie-o e tente novamente. |
| `Can't reach database server` | Container do banco parado (`docker ps`) ou `DATABASE_URL` incorreta no `.env`. |
| `prisma migrate deploy` falha | Banco ainda subindo — aguarde alguns segundos após o `docker compose up`. |
| Erro `GOOGLE_MAPS_API_KEY não configurada no .env` | Preencha a chave no `.env` e reinicie a API. |
| Estimativa retorna `Não existe PricingConfig ativo` | Rode `npx prisma db seed` ou crie uma configuração de tarifa pelo painel. |
| `Directions error: REQUEST_DENIED` | Chave do Maps inválida ou APIs (Directions/Geocoding) não habilitadas no Google Cloud. |
| Corrida recusada por região | O endereço está fora da região de operação cadastrada — ajuste no painel (Regiões). |
| Login no painel falha | Seed não executado ou API fora do ar. |
| App não conecta à API | `MOBU_API_BASE_URL` errada para o cenário (emulador × dispositivo físico) ou firewall bloqueando a porta 3000. |
| Push não chega no app | `google-services.json` ausente no app ou service account do Firebase não configurada no backend. |
| Porta 3000/3001 em uso | Outro processo ocupa a porta — encerre-o ou altere `PORT`/porta do Next. |

---

## Estrutura do Repositório

```
.
├── Artefatos/          Documentos e diagramas produzidos ao longo do TCC
│   └── TCC2/           Figuras finais: diagramas UML, DER e telas da aplicação
├── Codigo/             Código-fonte dos três componentes do sistema
│   ├── backend/api/    API NestJS + Prisma (src/, prisma/, test/)
│   ├── dashboard/      Painel administrativo Next.js
│   ├── MobULite/       Aplicativo Android (Kotlin + Jetpack Compose)
│   └── docker-compose.yml  PostgreSQL 16 via Docker
├── Divulgacao/         Apresentações e vídeos de entrega
├── Documentacao/       Documentação de projeto (PDF)
└── docs/
    └── manuais/        Manual do usuário, manual administrativo, guia de instalação e FAQ
```

---

## Funcionalidades Principais

**Passageiro**
- Cadastro com CPF, data de nascimento e contato de emergência
- Estimativa de tarifa e rota antes de solicitar a corrida
- Acompanhamento do motorista em tempo real no mapa (WebSocket)
- Pagamento via PIX off-line ou dinheiro; cupons de desconto
- Histórico de corridas, recibos e avaliação de motoristas (1–5 estrelas)
- Chat com o motorista durante a corrida

**Motorista**
- Cadastro com CNH (número, categoria, EAR) e dados do veículo — sujeito à aprovação do admin
- Recebimento de corridas por notificação push e WebSocket, com aceite/recusa
- Marcação de etapas: a caminho → chegou → em corrida → finalizado
- Histórico financeiro com ciclos de cobrança semanais
- Solicitação de pagamento (acerto) à plataforma
- Avaliação do passageiro e abertura de chamados de suporte

**Administrador**
- Aprovação e rejeição de cadastros de motoristas
- Monitoramento ao vivo de corridas e motoristas online
- Relatórios financeiros e operacionais
- Configuração de tarifas (base, km, minuto, surge, taxa) e regiões de operação
- Gestão de cupons de desconto, liquidações financeiras e chamados de suporte
- Bloqueio/desbloqueio de usuários

---

## Documentação Adicional

| Documento | Localização |
|---|---|
| Manual do usuário (passageiro e motorista) | [`docs/manuais/manual-do-usuario.md`](docs/manuais/manual-do-usuario.md) |
| Manual administrativo | [`docs/manuais/manual-administrativo.md`](docs/manuais/manual-administrativo.md) |
| Guia de instalação detalhado | [`docs/manuais/guia-de-instalacao.md`](docs/manuais/guia-de-instalacao.md) |
| FAQ (perguntas frequentes) | [`docs/manuais/faq.md`](docs/manuais/faq.md) |
| Documentação de projeto completa | [`Documentacao/`](Documentacao/) |
| READMEs específicos de cada componente | `Codigo/backend/api/`, `Codigo/dashboard/admin/`, `Codigo/MobULite/` |

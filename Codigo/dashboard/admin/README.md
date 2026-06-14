# MobU — Dashboard Administrativo

Painel web de administração do sistema MobU, desenvolvido com **Next.js** e **TypeScript**.

## Tecnologias

- [Next.js 16](https://nextjs.org/) — framework React
- [Leaflet](https://leafletjs.com/) — mapas interativos ao vivo
- [jsPDF + AutoTable](https://github.com/parallax/jsPDF) — exportação de relatórios em PDF
- [JWT](https://jwt.io/) — autenticação via token

---

## Pré-requisitos

- Node.js 20+
- Backend MobU em execução (porta 3000)

---

## Configuração

Por padrão, o dashboard conecta ao backend em `http://localhost:3000`. Para apontar para outro endereço, crie um arquivo `.env.local`:

```
NEXT_PUBLIC_API_URL=http://SEU_BACKEND:3000
```

---

## Executando

```bash
npm install
npm run dev
```

O painel estará disponível em `http://localhost:3001`.

**Login padrão** (após executar o seed do backend):
- Telefone: `31999000000`
- Senha: `Senha@123`

---

## Funcionalidades

| Seção | Descrição |
|---|---|
| **Operação ao vivo** | Mapa com motoristas online e corridas ativas em tempo real via WebSocket |
| **Motoristas** | Listagem, aprovação e rejeição de cadastros com visualização de CNH |
| **Passageiros** | Listagem com histórico de corridas e chamados |
| **Corridas** | Listagem com filtros por status e método de pagamento |
| **Financeiro** | Resumo de receita, saldo por motorista, liquidações e solicitações de pagamento |
| **Tarifas** | Criação e ativação de configurações de precificação por região |
| **Regiões** | Definição de áreas de operação com raio em metros |
| **Cupons** | Criação e gerenciamento de cupons de desconto |
| **Suporte** | Atendimento a chamados abertos por passageiros e motoristas |
| **Relatórios** | Estatísticas operacionais com filtro de período e exportação em PDF |
| **Configurações** | Ajuste de parâmetros do sistema (limite de débito, chave PIX da plataforma) |

---

## Build de produção

```bash
npm run build
npm run start
```

# Manual Administrativo — MobU

Este manual destina-se aos **administradores** da plataforma MobU, que operam o **painel web** (dashboard) pelo navegador. Ele foi escrito de forma didática: cada seção explica o que a função faz, quando usá-la e o passo a passo.

---

## 1. O que é o painel administrativo?

É um site (não é aplicativo de celular) onde o administrador enxerga e controla toda a operação do MobU: cadastros de motoristas, corridas acontecendo agora, valores, tarifas, cupons e chamados de suporte. Ele funciona em qualquer navegador moderno (Chrome, Firefox, Edge, Safari), de preferência em um computador.

## 2. Acessando o painel

1. Abra o navegador e acesse o endereço do painel (em ambiente de desenvolvimento: `http://localhost:3001`).
2. Entre com o **telefone** e a **senha** de administrador.
   - Após a instalação inicial (seed do banco), existe um administrador padrão: telefone `31999000000` / senha `Senha@123`.
   - ⚠️ **Troque essa senha imediatamente em produção** — ela é pública na documentação.
3. Após o login você verá o **painel principal**, com um resumo da operação e o menu com as seções descritas abaixo.

---

## 3. Aprovação de motoristas (tarefa mais importante)

**Por que existe:** qualquer pessoa pode se cadastrar como motorista pelo aplicativo, mas ninguém transporta passageiros sem que um administrador confira os documentos. Todo novo motorista entra com status **pendente**.

**Como fazer a análise:**

1. No menu, abra a seção **Motoristas**. Os cadastros pendentes ficam destacados.
2. Clique em um motorista para ver os dados enviados:
   - nome, telefone e CPF;
   - **CNH**: número, categoria, validade, indicação de **EAR** (Exerce Atividade Remunerada) e a **foto do documento**;
   - **veículo**: modelo, cor, placa, ano e capacidade;
   - **chave PIX** para recebimento.
3. Confira com atenção:
   - a foto da CNH está legível e dentro da validade?
   - a categoria permite a atividade? Possui EAR?
   - os dados do veículo estão completos e coerentes?
4. Decida:
   - **Aprovar** — o motorista é notificado no aplicativo e já pode ficar online e receber corridas;
   - **Rejeitar** — informe o **motivo** (ex.: "foto da CNH ilegível"); o motorista verá a justificativa e poderá corrigir o cadastro e reenviar.

---

## 4. Monitoramento ao vivo

**Por que existe:** permite enxergar a operação em tempo real, como uma "central de controle".

Na seção do **mapa ao vivo** você vê:

- **Motoristas online**: onde cada um está agora (a posição se move no mapa);
- **Corridas em andamento**: cada corrida ativa, com a etapa atual (a caminho, chegou, em corrida).

**Quando usar:** para acompanhar horários de pico, verificar se há motoristas cobrindo a cidade e investigar situações como uma corrida parada há muito tempo no mesmo lugar.

---

## 5. Gestão de usuários

- Na seção de **usuários**, você lista e pesquisa **passageiros** e **motoristas** cadastrados.
- Ao abrir um usuário, vê os dados de cadastro e o histórico de corridas.
- É possível **bloquear/desativar** uma conta em caso de violação das regras (ex.: comportamento inadequado relatado em chamados). Uma conta bloqueada não consegue mais usar a plataforma.

---

## 6. Tarifas e regiões de operação

### 6.1 Tarifas (quanto custa uma corrida)

O preço estimado de cada corrida é calculado automaticamente a partir de 4 valores que **você** configura:

| Item | O que é | Exemplo |
|---|---|---|
| **Bandeirada** | Valor fixo cobrado em toda corrida, só por começar | R$ 5,00 |
| **Preço por km** | Valor cobrado por quilômetro rodado | R$ 2,00/km |
| **Preço por minuto** | Valor cobrado por minuto de viagem | R$ 0,50/min |
| **Taxa da plataforma** | Percentual da corrida que fica para a plataforma (o restante é do motorista) | 20% |

**Como alterar:** abra a seção de **tarifas**, edite os valores e salve. Os novos valores passam a valer para as **próximas** estimativas de corrida (corridas já solicitadas não mudam).

> **Exemplo prático:** com os valores acima, uma corrida de 4 km e 10 minutos custa: R$ 5,00 + (4 × R$ 2,00) + (10 × R$ 0,50) = **R$ 18,00**. Desse total, R$ 3,60 (20%) ficam para a plataforma e R$ 14,40 para o motorista.

### 6.2 Regiões de operação (onde a plataforma atua)

- Defina no mapa as **áreas de atuação** (por exemplo, um círculo com centro na cidade e um raio em metros).
- Solicitações de corrida **fora** das regiões ativas são recusadas automaticamente pelo sistema — o passageiro recebe um aviso.
- Você pode cadastrar mais de uma região e ativar/desativar cada uma conforme a expansão do serviço.

---

## 7. Relatórios

A seção de **relatórios** reúne os números da operação para apoiar decisões:

- **Corridas**: total por período, concluídas × canceladas;
- **Financeiro**: faturamento total, valores da plataforma (taxas) e valores dos motoristas;
- **Motoristas**: desempenho individual, corridas realizadas, ganhos e **avaliação média** (estrelas recebidas dos passageiros).

**Sugestão de rotina:** consulte semanalmente para acompanhar o crescimento e ajustar tarifas/regiões conforme a demanda.

---

## 8. Financeiro dos motoristas (ciclos e acertos)

**Como funciona o modelo:** o passageiro paga o motorista diretamente (PIX ou dinheiro). A plataforma, portanto, precisa **acertar depois** com cada motorista a taxa das corridas. Para organizar isso:

- As corridas de cada motorista são agrupadas em **ciclos semanais** (de domingo a sábado), com o total ganho e a taxa devida.
- O motorista pode enviar pelo aplicativo uma **solicitação de pagamento/acerto**.
- No painel, você analisa cada solicitação: **aprova** (registrando o acerto, com método e observações) ou **rejeita** (informando o motivo). O motorista acompanha o status pelo app.
- Todos os acertos ficam registrados no histórico (quem pagou, quando, quanto e referente a quê).

---

## 9. Cupons de desconto

**Para que servem:** ações promocionais (ex.: atrair novos passageiros com "PRIMEIRA10").

**Como criar um cupom:**

1. Abra a seção de **cupons** e clique em criar.
2. Preencha:
   - **Código** — o texto que o passageiro digitará (ex.: `PROMO10`);
   - **Desconto** — em **percentual** (ex.: 10%) *ou* em **valor fixo** (ex.: R$ 5,00);
   - **Limite de usos** — quantas vezes o cupom pode ser usado no total;
   - **Validade** — data em que o cupom expira (opcional).
3. Salve. O cupom já pode ser usado pelos passageiros na tela de solicitação de corrida.

Você pode **desativar** um cupom a qualquer momento — ele deixa de ser aceito imediatamente. O painel mostra quantas vezes cada cupom já foi usado.

---

## 10. Suporte (chamados)

- Passageiros e motoristas abrem **chamados** pelo aplicativo relatando problemas (pagamento, comportamento, dúvidas).
- Na seção **Suporte**, os chamados aparecem em lista, dos mais recentes aos mais antigos.
- Abra um chamado para ler o relato, **responda** pelo próprio painel e **encerre** quando resolvido. O usuário vê a resposta no aplicativo.

**Boa prática:** responda em até 24–48h; chamados sem resposta geram desconfiança na plataforma.

---

## 11. Checklist do administrador

**Diariamente**
- [ ] Verificar cadastros de motoristas pendentes
- [ ] Verificar chamados de suporte abertos
- [ ] Dar uma olhada no mapa ao vivo nos horários de pico

**Semanalmente**
- [ ] Revisar os relatórios (corridas, faturamento, avaliações)
- [ ] Processar solicitações de pagamento dos motoristas
- [ ] Avaliar se tarifas e regiões precisam de ajuste

**Sempre**
- [ ] Nunca compartilhar a senha de administrador
- [ ] Trocar a senha padrão após a instalação

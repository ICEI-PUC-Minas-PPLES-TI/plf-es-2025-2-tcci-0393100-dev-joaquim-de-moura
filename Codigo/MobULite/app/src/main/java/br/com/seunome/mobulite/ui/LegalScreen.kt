package br.com.seunome.mobulite.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.seunome.mobulite.ui.theme.AppLilacSoft
import br.com.seunome.mobulite.ui.theme.Slate400
import br.com.seunome.mobulite.ui.theme.Slate700
import br.com.seunome.mobulite.ui.theme.Slate900

enum class LegalType { TERMS, PRIVACY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(type: LegalType, onBack: () -> Unit) {
    val title = if (type == LegalType.TERMS) "Termos de Uso" else "Política de Privacidade"
    val sections = if (type == LegalType.TERMS) termsSections() else privacySections()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppLilacSoft
                )
            )
        },
        containerColor = AppLilacSoft
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    "Última atualização: 24/05/2026",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
            }
            items(sections) { section ->
                LegalSection(heading = section.first, body = section.second)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun LegalSection(heading: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            heading,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Slate900
        )
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = Slate700,
            lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.6
        )
    }
}

// ─── Conteúdo — Termos de Uso ─────────────────────────────────────────────────

private fun termsSections(): List<Pair<String, String>> = listOf(

    "1. Identificação" to
        "O aplicativo MobU é operado por [NOME DA EMPRESA], pessoa jurídica de direito privado, " +
        "inscrita no CNPJ sob o nº [CNPJ: ___.___.___/____-__], com sede em [ENDEREÇO: ___], " +
        "doravante denominada simplesmente \"Empresa\".",

    "2. Aceitação dos Termos" to
        "Ao baixar, instalar ou utilizar o aplicativo MobU, o usuário declara ter lido, compreendido " +
        "e aceito integralmente os presentes Termos de Uso. Caso não concorde com qualquer disposição " +
        "aqui prevista, deverá cessar imediatamente o uso do aplicativo.",

    "3. Descrição do Serviço" to
        "O MobU é uma plataforma de intermediação de transporte urbano que conecta passageiros a " +
        "motoristas parceiros independentes. A Empresa não presta serviço de transporte diretamente, " +
        "atuando exclusivamente como intermediadora tecnológica.",

    "4. Cadastro e Conta" to
        "Para utilizar o aplicativo, o usuário deve realizar cadastro informando dados verdadeiros, " +
        "completos e atualizados. O usuário é responsável pela confidencialidade de suas credenciais " +
        "de acesso e por todas as atividades realizadas em sua conta. É vedado o compartilhamento " +
        "de conta entre terceiros.",

    "5. Obrigações do Passageiro" to
        "O passageiro compromete-se a: (a) informar corretamente o local de origem e destino; " +
        "(b) aguardar o motorista no endereço indicado; (c) tratar o motorista com respeito e " +
        "urbanidade; (d) não danificar o veículo do motorista parceiro; (e) efetuar o pagamento " +
        "pelo meio selecionado no aplicativo.",

    "6. Obrigações do Motorista Parceiro" to
        "O motorista parceiro compromete-se a: (a) manter documentação pessoal e do veículo " +
        "regularizada; (b) realizar as corridas aceitas com pontualidade e segurança; " +
        "(c) tratar o passageiro com respeito; (d) manter o veículo limpo e em boas condições; " +
        "(e) seguir o itinerário solicitado pelo passageiro.",

    "7. Pagamentos e Tarifas" to
        "As tarifas são calculadas com base em distância, tempo e configuração de preço vigente. " +
        "A Empresa reserva-se o direito de alterar as tarifas mediante aviso prévio. " +
        "Pagamentos via PIX ou dinheiro em espécie são de responsabilidade das partes envolvidas " +
        "na corrida, cabendo à Empresa apenas a intermediação.",

    "8. Cancelamentos" to
        "O passageiro pode cancelar a corrida antes da chegada do motorista. Cancelamentos " +
        "reiterados ou abusivos podem resultar em suspensão temporária ou permanente da conta. " +
        "O motorista parceiro também pode recusar ou cancelar corridas em situações justificadas.",

    "9. Avaliações" to
        "Ao final de cada corrida, passageiros e motoristas podem avaliar a experiência. " +
        "As avaliações devem ser honestas e baseadas na experiência real. É vedada a manipulação " +
        "de avaliações por qualquer meio.",

    "10. Conduta Proibida" to
        "É expressamente proibido: (a) utilizar o aplicativo para fins ilegais; " +
        "(b) assediar, ameaçar ou discriminar outros usuários; (c) fornecer informações falsas " +
        "no cadastro; (d) realizar corridas fora do aplicativo combinadas dentro dele; " +
        "(e) usar mecanismos automatizados para acessar o sistema.",

    "11. Limitação de Responsabilidade" to
        "A Empresa não se responsabiliza por danos decorrentes de: (a) conduta de motoristas " +
        "parceiros ou passageiros; (b) atrasos causados por tráfego, condições climáticas ou " +
        "eventos de força maior; (c) falhas de conexão à internet; (d) uso indevido do aplicativo " +
        "por terceiros.",

    "12. Propriedade Intelectual" to
        "Todo o conteúdo do aplicativo MobU — incluindo marca, logotipo, código-fonte, design e " +
        "funcionalidades — é de propriedade exclusiva da Empresa e protegido pela legislação de " +
        "propriedade intelectual. É vedada qualquer reprodução sem autorização prévia e por escrito.",

    "13. Suspensão e Encerramento de Conta" to
        "A Empresa poderá suspender ou encerrar a conta do usuário que violar estes Termos, " +
        "sem prejuízo de outras medidas cabíveis. O usuário também pode solicitar o encerramento " +
        "de sua conta a qualquer momento pelo aplicativo ou por meio do suporte.",

    "14. Alterações nos Termos" to
        "A Empresa pode modificar estes Termos a qualquer momento. As alterações serão comunicadas " +
        "pelo aplicativo. O uso continuado após a comunicação implica aceitação das novas condições.",

    "15. Legislação Aplicável" to
        "Estes Termos são regidos pelas leis da República Federativa do Brasil. Fica eleito o foro " +
        "da comarca de [CIDADE/ESTADO: ___] para dirimir quaisquer controvérsias decorrentes deste " +
        "instrumento.",

    "16. Contato" to
        "Dúvidas, sugestões ou reclamações podem ser enviadas para: [E-MAIL DE CONTATO: ___]."
)

// ─── Conteúdo — Política de Privacidade ──────────────────────────────────────

private fun privacySections(): List<Pair<String, String>> = listOf(

    "1. Controladora dos Dados" to
        "A controladora dos dados pessoais tratados neste aplicativo é [NOME DA EMPRESA], " +
        "inscrita no CNPJ nº [CNPJ: ___.___.___/____-__], com sede em [ENDEREÇO: ___]. " +
        "Contato do encarregado de dados (DPO): [E-MAIL DO DPO: ___].",

    "2. Bases Legais (LGPD)" to
        "O tratamento de dados pessoais pelo MobU fundamenta-se nas seguintes bases legais " +
        "previstas na Lei nº 13.709/2018 (LGPD): (a) execução de contrato, quando necessário " +
        "para a prestação do serviço; (b) legítimo interesse, para melhoria da experiência e " +
        "segurança; (c) consentimento, para comunicações de marketing e funcionalidades opcionais; " +
        "(d) cumprimento de obrigação legal, quando exigido por lei.",

    "3. Dados Coletados" to
        "Coletamos as seguintes categorias de dados pessoais:\n\n" +
        "• Identificação: nome completo, CPF, número de telefone, endereço de e-mail.\n" +
        "• Localização: coordenadas GPS durante o uso ativo do aplicativo.\n" +
        "• Financeiros: método de pagamento selecionado, chave Pix (motoristas).\n" +
        "• Documentos (motoristas): CNH, foto do veículo, foto de perfil.\n" +
        "• Uso do app: histórico de corridas, avaliações, interações com o suporte.\n" +
        "• Dispositivo: modelo do aparelho, sistema operacional, token de notificação push.",

    "4. Finalidade do Tratamento" to
        "Os dados são utilizados para: (a) criar e gerenciar contas de usuário; " +
        "(b) intermediar a conexão entre passageiros e motoristas; " +
        "(c) calcular tarifas e processar pagamentos; " +
        "(d) enviar notificações relacionadas ao serviço; " +
        "(e) melhorar a segurança da plataforma; " +
        "(f) cumprir obrigações legais e regulatórias; " +
        "(g) resolver disputas e prestação de suporte.",

    "5. Compartilhamento de Dados" to
        "Os dados pessoais podem ser compartilhados com:\n\n" +
        "• Motoristas parceiros: nome e foto do passageiro são exibidos durante a corrida.\n" +
        "• Passageiros: nome e foto do motorista, modelo e placa do veículo.\n" +
        "• Provedores de serviço: infraestrutura de nuvem, envio de SMS/e-mail e notificações push, " +
        "todos contratualmente obrigados a proteger os dados.\n" +
        "• Autoridades públicas: quando exigido por lei ou ordem judicial.\n\n" +
        "Não vendemos dados pessoais a terceiros.",

    "6. Localização" to
        "O aplicativo coleta dados de localização em tempo real apenas quando em uso ativo, " +
        "com a finalidade exclusiva de intermediar o serviço de transporte. Motoristas têm " +
        "localização coletada também em segundo plano durante o turno ativo, para permitir " +
        "o despacho de corridas. O usuário pode revogar a permissão de localização nas " +
        "configurações do dispositivo, o que impossibilitará o uso do serviço.",

    "7. Retenção dos Dados" to
        "Os dados pessoais são mantidos pelo tempo necessário para as finalidades descritas " +
        "ou conforme exigido por lei. Após o encerramento da conta, os dados são anonimizados " +
        "ou eliminados em até [PRAZO: ___] dias, salvo obrigação legal de retenção.",

    "8. Segurança" to
        "Adotamos medidas técnicas e organizacionais adequadas para proteger os dados pessoais " +
        "contra acesso não autorizado, perda, alteração ou divulgação indevida, incluindo " +
        "criptografia em trânsito (HTTPS/TLS), autenticação por token JWT e controle de acesso " +
        "baseado em perfis.",

    "9. Direitos do Titular" to
        "Nos termos da LGPD, o titular dos dados tem direito a:\n\n" +
        "• Confirmar a existência de tratamento;\n" +
        "• Acessar seus dados;\n" +
        "• Corrigir dados incompletos ou desatualizados;\n" +
        "• Solicitar anonimização, bloqueio ou eliminação;\n" +
        "• Solicitar portabilidade;\n" +
        "• Revogar consentimento;\n" +
        "• Opor-se a tratamento em desacordo com a lei.\n\n" +
        "Para exercer seus direitos, entre em contato pelo e-mail: [E-MAIL DE CONTATO: ___].",

    "10. Cookies e Tecnologias Similares" to
        "O aplicativo móvel não utiliza cookies de navegador. Utilizamos identificadores de " +
        "dispositivo e tokens de sessão estritamente necessários para o funcionamento do serviço.",

    "11. Transferência Internacional" to
        "Alguns de nossos provedores de infraestrutura podem armazenar dados em servidores " +
        "localizados fora do Brasil. Nesses casos, garantimos que as transferências ocorrem " +
        "em conformidade com a LGPD, com cláusulas contratuais adequadas.",

    "12. Menores de Idade" to
        "O aplicativo MobU não é destinado a menores de 18 anos. Não coletamos intencionalmente " +
        "dados de menores. Caso identifiquemos tal situação, os dados serão eliminados imediatamente.",

    "13. Alterações nesta Política" to
        "Esta Política pode ser atualizada periodicamente. Comunicaremos alterações relevantes " +
        "por meio do aplicativo. O uso continuado após a notificação implica aceitação da versão " +
        "atualizada.",

    "14. Contato e DPO" to
        "Para exercer seus direitos ou esclarecer dúvidas sobre privacidade:\n\n" +
        "E-mail: [E-MAIL DE CONTATO: ___]\n" +
        "Endereço: [ENDEREÇO: ___]\n" +
        "Encarregado de Dados (DPO): [NOME DO DPO: ___]"
)

/**
 * @file Limitações Conhecidas — Known Limitations Test Suite
 *
 * Testes marcados com `it.failing()` documentam lacunas reais da implementação.
 * O Jest os considera APROVADOS enquanto o bug existir, e os marca como
 * QUEBRADOS automaticamente quando a correção for aplicada — funcionando
 * como alertas de regressão invertidos.
 *
 * Essa prática é adotada por plataformas de mobilidade de grande escala
 * (Uber, 99, Lyft) para rastrear débitos técnicos críticos sem bloquear o
 * pipeline de entrega contínua.
 *
 * Lacunas documentadas neste arquivo:
 *
 *   [KL-01] Condição de corrida no uso simultâneo de cupom (TOCTOU)
 *           ride.service.ts — verificação e incremento não atômicos
 *
 *   [KL-02] Pagamento confirmado em corrida ainda em andamento
 *           ride.service.ts — falta verificação de ride.status antes de confirmar pagamento
 *
 *   [KL-03] Confirmação de pagamento não idempotente
 *           ride.service.ts — segunda confirmação sobrescreve timestamp original
 *
 *   [KL-04] Mensagem de chat permitida em corrida encerrada/cancelada
 *           chat.service.ts — assertParticipant() não valida status da corrida
 *
 *   [KL-05] Erro P2002 de avaliação duplicada simultânea não traduzido pelo serviço
 *           review.service.ts — PrismaClientKnownRequestError propaga sem encapsulamento
 */
import { Test, TestingModule } from '@nestjs/testing';
import { BadRequestException } from '@nestjs/common';
import { Prisma, PaymentStatus, RideStatus } from '@prisma/client';
import { ConfigService } from '@nestjs/config';

import { PrismaService } from './prisma/prisma.service';
import { RealtimeService } from './realtime/realtime.service';
import { NotificationService } from './notification/notification.service';
import { GoogleDirectionsService } from './maps/google-directions.service';
import { RideService } from './ride/ride.service';
import { ReviewService } from './review/review.service';
import { ChatService } from './chat/chat.service';

// ─── Fixtures ────────────────────────────────────────────────────────────────

const PRICING = {
  id: 'pricing-1',
  isActive: true,
  baseFareCents: 500,
  perKmCents: 200,
  perMinuteCents: 50,
  minimumFareCents: 800,
  bookingFeeCents: 0,
  surgeMultiplier: 1,
  currency: 'BRL',
  createdAt: new Date(),
};

const RIDE_CREATED = {
  id: 'ride-1',
  passengerId: 'pass-1',
  driverId: null,
  status: RideStatus.PENDING_DRIVER,
  originLat: -19.7,
  originLng: -44.8,
  destLat: -19.75,
  destLng: -44.85,
  originAddress: 'Rua A, 100',
  destinationAddress: 'Rua B, 200',
  distanceMeters: 5000,
  durationSeconds: 600,
  estimatedFareCents: 1800,
  paymentMethod: 'CASH',
  paymentStatus: PaymentStatus.PENDING,
  paymentPixPayload: null,
  paymentTxId: null,
  paymentConfirmedAt: null,
  paymentReportedAt: null,
  paymentConfirmedBy: null,
  pricingConfigId: 'pricing-1',
  createdAt: new Date(),
};

const RIDE_FINISHED = {
  ...RIDE_CREATED,
  status: RideStatus.FINISHED,
  driverId: 'driver-1',
  paymentStatus: PaymentStatus.RECEIVED,
  paymentConfirmedAt: new Date('2025-01-01T10:00:00Z'),
  paymentConfirmedBy: 'driver-1',
};

/** Cupom ativo, dentro do limite de usos, sem expiração */
const PROMO_VALID = {
  id: 'promo-1',
  code: 'DESCONTO10',
  active: true,
  discountPercent: 10,
  discountCents: null as number | null,
  maxUses: 1,
  usedCount: 0,
  expiresAt: null as Date | null,
  createdAt: new Date(),
};

/** Dados mínimos para criar uma corrida */
const CREATE_RIDE_INPUT = {
  passengerId: 'pass-1',
  originLat: -19.7,
  originLng: -44.8,
  destLat: -19.75,
  destLng: -44.85,
  originAddress: 'Rua A, 100',
  destinationAddress: 'Rua B, 200',
  paymentMethod: 'CASH' as const,
};

// ══════════════════════════════════════════════════════════════════════════════
// RideService — Limitações Conhecidas
// ══════════════════════════════════════════════════════════════════════════════
describe('RideService — Limitações Conhecidas', () => {
  let service: RideService;
  let prisma: jest.Mocked<PrismaService>;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        RideService,
        {
          provide: PrismaService,
          useValue: {
            ride: {
              create: jest.fn(),
              findMany: jest.fn(),
              findFirst: jest.fn(),
              findUnique: jest.fn(),
              update: jest.fn(),
              aggregate: jest.fn(),
            },
            driverProfile: { findMany: jest.fn() },
            pricingConfig: { findFirst: jest.fn() },
            operationRegion: { findMany: jest.fn() },
            promoCode: { findFirst: jest.fn(), update: jest.fn() },
            review: { aggregate: jest.fn() },
          },
        },
        {
          provide: ConfigService,
          useValue: { get: jest.fn().mockReturnValue('FAKE_GOOGLE_KEY') },
        },
        {
          provide: GoogleDirectionsService,
          useValue: {
            getRoute: jest.fn().mockResolvedValue({
              distanceMeters: 5000,
              durationSeconds: 600,
              encodedPolyline: 'abc',
            }),
          },
        },
        {
          provide: RealtimeService,
          useValue: {
            emitToDrivers: jest.fn(),
            emitAdmin: jest.fn(),
            emitRide: jest.fn(),
            emitToUser: jest.fn(),
          },
        },
        {
          provide: NotificationService,
          useValue: { sendToUser: jest.fn(), sendToUsers: jest.fn() },
        },
      ],
    }).compile();

    service = module.get<RideService>(RideService);
    prisma = module.get(PrismaService);
  });

  // ──────────────────────────────────────────────────────────────────────────
  // [KL-01] TOCTOU — Cupom Simultâneo
  // ──────────────────────────────────────────────────────────────────────────
  describe('[KL-01] Condição de corrida no uso simultâneo de cupom (TOCTOU)', () => {
    /**
     * CONTEXTO
     * --------
     * O fluxo atual de create() executa duas operações NÃO atômicas:
     *   1. promoCode.findFirst()  → lê usedCount
     *   2. promoCode.update()     → incrementa usedCount
     *
     * Em requisições simultâneas, ambas as leituras ocorrem antes de qualquer
     * escrita. Ambas veem usedCount=0 < maxUses=1 e ambas aplicam o desconto.
     *
     * IMPACTO REAL
     * ------------
     * Em produção, isso representa perda de receita direta: um cupom de uso
     * único pode ser aplicado em múltiplas corridas simultâneas.
     * Equivalente ao problema documentado pela engenharia do Uber em
     * promoções de alta demanda (pico de Ano Novo, eventos esportivos).
     *
     * CORREÇÃO ESPERADA
     * -----------------
     * Envolver verificação + incremento em uma transação atômica, ou usar
     * UPDATE com cláusula WHERE condicional (UPDATE ... WHERE usedCount < maxUses)
     * que falha silenciosamente se o limite já foi atingido.
     */
    it.failing(
      '[KL-01] cupom com maxUses=1 não deve ser aplicado em duas requisições simultâneas',
      async () => {
        // Ambas as requisições lerão este mesmo estado "velho" do cupom
        // antes que qualquer uma delas grave o incremento.
        (prisma.operationRegion.findMany as jest.Mock).mockResolvedValue([]);
        (prisma.pricingConfig.findFirst as jest.Mock).mockResolvedValue(PRICING);
        (prisma.ride.create as jest.Mock).mockResolvedValue(RIDE_CREATED);
        (prisma.driverProfile.findMany as jest.Mock).mockResolvedValue([]);
        (prisma.promoCode.findFirst as jest.Mock).mockResolvedValue(PROMO_VALID);
        (prisma.promoCode.update as jest.Mock).mockResolvedValue({ ...PROMO_VALID, usedCount: 1 });

        // Duas corridas de passageiros diferentes com o mesmo cupom de uso único
        await Promise.all([
          service.create({ ...CREATE_RIDE_INPUT, passengerId: 'pass-1', promoCode: 'DESCONTO10' }),
          service.create({ ...CREATE_RIDE_INPUT, passengerId: 'pass-2', promoCode: 'DESCONTO10' }),
        ]);

        // Comportamento esperado: apenas uma corrida deve ter o cupom aplicado.
        // Comportamento atual: promoCode.update é chamado duas vezes (duas aplicações).
        expect(prisma.promoCode.update).toHaveBeenCalledTimes(1);
      },
    );
  });

  // ──────────────────────────────────────────────────────────────────────────
  // [KL-02] Pagamento Confirmado em Corrida Não Finalizada
  // ──────────────────────────────────────────────────────────────────────────
  describe('[KL-02] Pagamento pode ser confirmado antes da corrida finalizar', () => {
    /**
     * CONTEXTO
     * --------
     * updatePaymentStatus() valida apenas:
     *   - Se o usuário é motorista ou admin
     *   - Se a corrida cancelada não recebe outro status de pagamento
     *
     * Não há verificação de ride.status === FINISHED antes de confirmar.
     * Um motorista pode marcar o pagamento como recebido em uma corrida que
     * ainda está em andamento (IN_PROGRESS) ou nem foi aceita (PENDING_DRIVER).
     *
     * IMPACTO REAL
     * ------------
     * Dados financeiros inconsistentes: um ride pode ter paymentStatus=RECEIVED
     * com status=IN_PROGRESS. Relatórios de faturamento, dashboards de admin e
     * liquidações (driverSettlement) calculariam receita de corridas não concluídas.
     * Em plataformas de grande escala, isso causa reconciliação financeira incorreta.
     *
     * CORREÇÃO ESPERADA
     * -----------------
     * Em updatePaymentStatus(), antes de atualizar, verificar:
     *   if (ride.status !== RideStatus.FINISHED)
     *     throw new BadRequestException('Pagamento só pode ser confirmado em corridas finalizadas');
     */
    it.failing(
      '[KL-02] confirmPayment deve ser rejeitado quando corrida ainda está em andamento',
      async () => {
        const rideEmAndamento = {
          ...RIDE_CREATED,
          status: RideStatus.IN_PROGRESS,
          driverId: 'driver-1',
          paymentStatus: PaymentStatus.PENDING,
        };

        (prisma.ride.findUnique as jest.Mock).mockResolvedValue(rideEmAndamento);
        (prisma.ride.update as jest.Mock).mockResolvedValue({
          ...rideEmAndamento,
          paymentStatus: PaymentStatus.RECEIVED,
          paymentConfirmedAt: new Date(),
        });

        // Comportamento esperado: rejeitar confirmação em corrida ainda em andamento.
        // Comportamento atual: pagamento confirmado com sucesso em qualquer status.
        await expect(
          service.confirmPayment('ride-1', { userId: 'driver-1', role: 'DRIVER' }),
        ).rejects.toThrow(BadRequestException);
      },
    );
  });

  // ──────────────────────────────────────────────────────────────────────────
  // [KL-03] Idempotência — Confirmação de Pagamento
  // ──────────────────────────────────────────────────────────────────────────
  describe('[KL-03] Confirmação de pagamento não idempotente', () => {
    /**
     * CONTEXTO
     * --------
     * confirmPayment() delega para updatePaymentStatus(), que atualiza
     * paymentConfirmedAt com `new Date()` a cada chamada, sem verificar
     * se o pagamento já foi confirmado anteriormente.
     *
     * IMPACTO REAL
     * ------------
     * Uma segunda chamada — seja por bug no cliente, retry de rede ou ação
     * maliciosa — sobrescreve o timestamp original de confirmação.
     * Em disputas de pagamento, o registro auditável de "quando o motorista
     * confirmou" é destruído.
     *
     * Plataformas como Stripe e PagSeguro rejeitam operações de pagamento
     * duplicadas com HTTP 409, protegendo a integridade do log financeiro.
     *
     * CORREÇÃO ESPERADA
     * -----------------
     * Em updatePaymentStatus(), verificar ride.paymentStatus antes de gravar:
     *   if (ride.paymentStatus === PaymentStatus.RECEIVED)
     *     throw new BadRequestException('Pagamento já confirmado');
     */
    it.failing(
      '[KL-03] segunda confirmação de pagamento deve lançar BadRequestException',
      async () => {
        // Corrida já confirmada (paymentStatus: RECEIVED)
        (prisma.ride.findUnique as jest.Mock).mockResolvedValue(RIDE_FINISHED);
        (prisma.ride.update as jest.Mock).mockResolvedValue({
          ...RIDE_FINISHED,
          paymentConfirmedAt: new Date(),
        });

        // Comportamento esperado: rejeitar com erro claro.
        // Comportamento atual: re-confirma silenciosamente com novo timestamp.
        await expect(
          service.confirmPayment('ride-1', { userId: 'driver-1', role: 'DRIVER' }),
        ).rejects.toThrow(BadRequestException);
      },
    );
  });
});

// ══════════════════════════════════════════════════════════════════════════════
// ChatService — Limitações Conhecidas
// ══════════════════════════════════════════════════════════════════════════════
describe('ChatService — Limitações Conhecidas', () => {
  let service: ChatService;
  let prisma: jest.Mocked<PrismaService>;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ChatService,
        {
          provide: PrismaService,
          useValue: {
            ride: { findUnique: jest.fn() },
            chatMessage: {
              create: jest.fn().mockResolvedValue({ id: 'msg-1', content: 'ok', sentAt: new Date() }),
              findMany: jest.fn(),
              updateMany: jest.fn(),
            },
          },
        },
      ],
    }).compile();

    service = module.get<ChatService>(ChatService);
    prisma = module.get(PrismaService);
  });

  // ──────────────────────────────────────────────────────────────────────────
  // [KL-04] Chat em Corrida Encerrada
  // ──────────────────────────────────────────────────────────────────────────
  describe('[KL-04] Mensagem de chat permitida em corrida encerrada', () => {
    /**
     * CONTEXTO
     * --------
     * assertParticipant() valida apenas se o usuário é passageiro ou motorista
     * da corrida (ride.passengerId ou ride.driverId), mas não verifica o
     * status atual da corrida.
     *
     * IMPACTO REAL
     * ------------
     * Após cancelamento ou finalização, passageiro e motorista continuam
     * podendo enviar mensagens — gerando notificações push desnecessárias,
     * histórico de chat poluído e possível vetor de assédio pós-corrida.
     *
     * Apps como Uber e 99 bloqueiam o canal de chat assim que a corrida
     * é finalizada ou cancelada, exibindo apenas o histórico como leitura.
     *
     * CORREÇÃO ESPERADA
     * -----------------
     * Em assertParticipant(), após verificar participação, checar:
     *   if (!ACTIVE_RIDE_STATUSES.includes(ride.status))
     *     throw new ForbiddenException('Chat encerrado para esta corrida');
     */
    it.failing(
      '[KL-04] mensagem não deve ser enviada para corrida com status CANCELED',
      async () => {
        (prisma.ride.findUnique as jest.Mock).mockResolvedValue({
          passengerId: 'pass-1',
          driverId: 'driver-1',
          status: RideStatus.CANCELED,
        });

        // Comportamento esperado: rejeitar envio após cancelamento.
        // Comportamento atual: mensagem criada normalmente.
        await expect(
          service.sendMessage('pass-1', 'ride-1', 'Chegou?', 'PASSENGER'),
        ).rejects.toThrow();
      },
    );

    it.failing(
      '[KL-04] mensagem não deve ser enviada para corrida com status FINISHED',
      async () => {
        (prisma.ride.findUnique as jest.Mock).mockResolvedValue({
          passengerId: 'pass-1',
          driverId: 'driver-1',
          status: RideStatus.FINISHED,
        });

        // Comportamento esperado: rejeitar envio após finalização.
        // Comportamento atual: mensagem criada normalmente.
        await expect(
          service.sendMessage('pass-1', 'ride-1', 'Obrigado pela corrida!', 'PASSENGER'),
        ).rejects.toThrow();
      },
    );
  });
});

// ══════════════════════════════════════════════════════════════════════════════
// ReviewService — Limitações Conhecidas
// ══════════════════════════════════════════════════════════════════════════════
describe('ReviewService — Limitações Conhecidas', () => {
  let service: ReviewService;
  let prisma: jest.Mocked<PrismaService>;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ReviewService,
        {
          provide: PrismaService,
          useValue: {
            ride: { findUnique: jest.fn() },
            review: {
              findUnique: jest.fn(),
              create: jest.fn(),
              findMany: jest.fn(),
              aggregate: jest.fn(),
            },
            driverProfile: { findFirst: jest.fn() },
          },
        },
      ],
    }).compile();

    service = module.get<ReviewService>(ReviewService);
    prisma = module.get(PrismaService);
  });

  // ──────────────────────────────────────────────────────────────────────────
  // [KL-05] P2002 de Avaliação Duplicada
  // ──────────────────────────────────────────────────────────────────────────
  describe('[KL-05] Erro P2002 de avaliação duplicada não encapsulado pelo serviço', () => {
    /**
     * CONTEXTO
     * --------
     * createForRide() realiza uma verificação de duplicata seguida de criação
     * em operações separadas (não atômicas):
     *   1. review.findUnique()  → verifica existência
     *   2. review.create()      → insere registro
     *
     * Em requisições simultâneas, ambas passam pelo check (retorna null),
     * mas a segunda tentativa de inserção recebe P2002 do banco de dados
     * (violação de unique constraint em rideId).
     *
     * Esse erro NÃO é capturado dentro do ReviewService — propaga como
     * PrismaClientKnownRequestError em vez de um BadRequestException legível.
     *
     * O PrismaExceptionFilter converte P2002 em HTTP 409 na camada HTTP,
     * mas qualquer código que consuma o serviço diretamente (outros módulos,
     * testes de integração, BFF) recebe um erro não estruturado e opaco.
     *
     * IMPACTO REAL
     * ------------
     * Logs de erro desnecessários com stack trace de banco de dados;
     * mensagem de erro expõe detalhes internos de schema ao cliente.
     *
     * CORREÇÃO ESPERADA
     * -----------------
     * Capturar P2002 no bloco try/catch de createForRide() e relançar como:
     *   throw new BadRequestException('Essa corrida já foi avaliada');
     */
    it.failing(
      '[KL-05] P2002 de avaliação duplicada deve ser convertido em BadRequestException pelo serviço',
      async () => {
        const rideFinalizada = {
          id: 'ride-1',
          passengerId: 'pass-1',
          driverId: 'driver-1',
          status: RideStatus.FINISHED,
        };

        (prisma.ride.findUnique as jest.Mock).mockResolvedValue(rideFinalizada);
        // Simula condição de corrida: check passou (null encontrado),
        // mas a inserção falha porque outra requisição simultânea já gravou.
        (prisma.review.findUnique as jest.Mock).mockResolvedValue(null);
        (prisma.review.create as jest.Mock).mockRejectedValue(
          new Prisma.PrismaClientKnownRequestError('Unique constraint failed on the fields: (`rideId`)', {
            code: 'P2002',
            clientVersion: '5.0.0',
            meta: { target: ['rideId'] },
          }),
        );

        // Comportamento esperado: BadRequestException com mensagem amigável.
        // Comportamento atual: PrismaClientKnownRequestError propaga sem tratamento.
        await expect(
          service.createForRide('pass-1', 'ride-1', { rating: 5, comment: 'Ótimo motorista!' }),
        ).rejects.toThrow(BadRequestException);
      },
    );
  });
});

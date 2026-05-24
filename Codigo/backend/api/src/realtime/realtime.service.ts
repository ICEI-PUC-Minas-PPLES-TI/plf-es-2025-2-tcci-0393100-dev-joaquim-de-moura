import { Injectable, Logger } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { UserRole } from '@prisma/client';
import type { IncomingMessage, Server as HttpServer } from 'http';
import { WebSocket, WebSocketServer } from 'ws';
import { PrismaService } from '../prisma/prisma.service';

type ClientContext = {
  userId: string;
  role: UserRole;
  subscriptions: Set<string>;
};

type RealtimeMessage = {
  type: string;
  payload?: unknown;
};

@Injectable()
export class RealtimeService {
  private readonly logger = new Logger(RealtimeService.name);
  private server?: WebSocketServer;
  private readonly clients = new Map<WebSocket, ClientContext>();

  constructor(
    private readonly jwt: JwtService,
    private readonly prisma: PrismaService,
  ) {}

  attach(httpServer: HttpServer) {
    if (this.server) return;

    this.server = new WebSocketServer({
      server: httpServer,
      path: '/ws',
    });

    this.server.on('connection', (socket, request) => {
      this.handleConnection(socket, request).catch((error) => {
        this.logger.warn(`Falha ao autenticar websocket: ${String(error)}`);
        socket.close(1011, 'internal_error');
      });
    });

    this.logger.log('Realtime WebSocket disponível em /ws');
  }

  emitToUser(userId: string | null | undefined, type: string, payload: unknown) {
    if (!userId) return;
    this.emitWhere((client) => client.userId === userId, type, payload);
  }

  emitToRole(role: UserRole, type: string, payload: unknown) {
    this.emitWhere((client) => client.role === role, type, payload);
  }

  emitToDrivers(driverUserIds: string[], type: string, payload: unknown) {
    const targets = new Set(driverUserIds);
    this.emitWhere(
      (client) => client.role === UserRole.DRIVER && targets.has(client.userId),
      type,
      payload,
    );
  }

  emitRide(rideId: string, type: string, payload: unknown) {
    this.emitWhere(
      (client) => client.subscriptions.has(rideId),
      type,
      payload,
    );
  }

  /** Corridas: cada assinante pode receber payload diferente (ex.: telefone mascarado). */
  emitRidePerViewer(
    rideId: string,
    type: string,
    buildPayload: (viewer: ClientContext) => unknown,
  ) {
    for (const [socket, client] of this.clients.entries()) {
      if (socket.readyState !== WebSocket.OPEN) continue;
      if (!client.subscriptions.has(rideId)) continue;
      this.send(socket, type, buildPayload(client));
    }
  }

  emitAdmin(type: string, payload: unknown) {
    this.emitToRole(UserRole.ADMIN, type, payload);
  }

  private handleMessage(socket: WebSocket, raw: string) {
    const client = this.clients.get(socket);
    if (!client) return;

    let message: RealtimeMessage;
    try {
      message = JSON.parse(raw) as RealtimeMessage;
    } catch {
      this.send(socket, 'error', { message: 'Mensagem inválida' });
      return;
    }

    if (message.type === 'subscribe_ride') {
      const rideId =
        typeof message.payload === 'object' && message.payload !== null
          ? (message.payload as { rideId?: unknown }).rideId
          : null;

      if (typeof rideId === 'string' && rideId.trim()) {
        client.subscriptions.add(rideId);
        this.send(socket, 'ride_subscribed', { rideId });
      }
      return;
    }

    if (message.type === 'unsubscribe_ride') {
      const rideId =
        typeof message.payload === 'object' && message.payload !== null
          ? (message.payload as { rideId?: unknown }).rideId
          : null;

      if (typeof rideId === 'string') {
        client.subscriptions.delete(rideId);
      }
      return;
    }

    if (message.type === 'ping') {
      this.send(socket, 'pong', { at: new Date().toISOString() });
    }
  }

  private emitWhere(
    predicate: (client: ClientContext) => boolean,
    type: string,
    payload: unknown,
  ) {
    for (const [socket, client] of this.clients.entries()) {
      if (socket.readyState !== WebSocket.OPEN) continue;
      if (!predicate(client)) continue;
      this.send(socket, type, payload);
    }
  }

  private send(socket: WebSocket, type: string, payload: unknown) {
    if (socket.readyState !== WebSocket.OPEN) return;
    socket.send(JSON.stringify({ type, payload }));
  }

  private async handleConnection(socket: WebSocket, request: IncomingMessage) {
    const context = await this.authenticate(request);

    if (!context) {
      socket.close(1008, 'unauthorized');
      return;
    }

    this.clients.set(socket, context);
    this.send(socket, 'connected', {
      userId: context.userId,
      role: context.role,
    });

    socket.on('message', (raw) => this.handleMessage(socket, raw.toString()));
    socket.on('close', () => this.clients.delete(socket));
    socket.on('error', () => this.clients.delete(socket));
  }

  private async authenticate(request: IncomingMessage): Promise<ClientContext | null> {
    try {
      const url = new URL(request.url ?? '', 'http://localhost');
      const rawToken =
        url.searchParams.get('token') ??
        request.headers.authorization?.replace(/^Bearer\s+/i, '');

      if (!rawToken) return null;

      const payload = this.jwt.verify<{ sub: string; role: UserRole }>(rawToken);

      if (!payload.sub || !payload.role) return null;

      const user = await this.prisma.user.findUnique({
        where: { id: payload.sub },
        select: { id: true, role: true, blocked: true },
      });

      if (!user || user.blocked) return null;

      return {
        userId: user.id,
        role: user.role,
        subscriptions: new Set(),
      };
    } catch {
      return null;
    }
  }
}

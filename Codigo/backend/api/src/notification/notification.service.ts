import { Injectable, Logger, OnModuleInit } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import * as admin from 'firebase-admin';
import { existsSync, readFileSync } from 'fs';
import { join } from 'path';

@Injectable()
export class NotificationService implements OnModuleInit {
  private readonly logger = new Logger(NotificationService.name);
  private initialized = false;

  constructor(private prisma: PrismaService) {}

  onModuleInit() {
    if (admin.apps.length > 0) {
      this.initialized = true;
      return;
    }

    try {
      let credential: admin.credential.Credential | undefined;

      // Opção 1: variável de ambiente com o JSON completo
      const jsonEnv = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
      if (jsonEnv) {
        const serviceAccount = JSON.parse(jsonEnv);
        credential = admin.credential.cert(serviceAccount);
      }

      // Opção 2: arquivo local firebase-service-account.json
      if (!credential) {
        const filePath = join(process.cwd(), 'firebase-service-account.json');
        if (existsSync(filePath)) {
          const serviceAccount = JSON.parse(readFileSync(filePath, 'utf8'));
          credential = admin.credential.cert(serviceAccount);
        }
      }

      if (!credential) {
        this.logger.warn('FCM não configurado — defina FIREBASE_SERVICE_ACCOUNT_JSON ou firebase-service-account.json');
        return;
      }

      admin.initializeApp({ credential });
      this.initialized = true;
      this.logger.log('Firebase Admin inicializado com sucesso');
    } catch (err) {
      this.logger.error('Erro ao inicializar Firebase Admin', err);
    }
  }

  async sendToUser(
    userId: string,
    title: string,
    body: string,
    data?: Record<string, string>,
  ): Promise<void> {
    if (!this.initialized) return;

    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { fcmToken: true },
    });

    if (!user?.fcmToken) return;

    await this.sendToToken(user.fcmToken, title, body, data);
  }

  async sendToUsers(
    userIds: string[],
    title: string,
    body: string,
    data?: Record<string, string>,
  ): Promise<void> {
    if (!this.initialized || userIds.length === 0) return;

    const users = await this.prisma.user.findMany({
      where: { id: { in: userIds }, fcmToken: { not: null } },
      select: { fcmToken: true },
    });

    const tokens = users.map((u) => u.fcmToken!).filter(Boolean);
    if (tokens.length === 0) return;

    await Promise.allSettled(
      tokens.map((token) => this.sendToToken(token, title, body, data)),
    );
  }

  private async sendToToken(
    token: string,
    title: string,
    body: string,
    data?: Record<string, string>,
  ): Promise<void> {
    try {
      await admin.messaging().send({
        token,
        notification: { title, body },
        android: { priority: 'high', notification: { sound: 'default', channelId: 'mobu_notifications' } },
        apns: { payload: { aps: { sound: 'default' } } },
        data,
      });
    } catch (err: any) {
      // Token inválido/expirado: limpa do banco
      if (
        err?.errorInfo?.code === 'messaging/registration-token-not-registered' ||
        err?.errorInfo?.code === 'messaging/invalid-registration-token'
      ) {
        await this.prisma.user.updateMany({
          where: { fcmToken: token },
          data: { fcmToken: null },
        });
      } else {
        this.logger.warn(`Falha ao enviar notificação: ${err?.message}`);
      }
    }
  }
}

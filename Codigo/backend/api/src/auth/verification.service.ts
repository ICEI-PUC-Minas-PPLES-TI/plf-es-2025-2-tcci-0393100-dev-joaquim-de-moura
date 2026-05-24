import {
  BadRequestException,
  Injectable,
  Logger,
  HttpException,
  HttpStatus,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as bcrypt from 'bcrypt';
import * as crypto from 'crypto';
import { VerificationChannel, UserRole } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

const CODE_TTL_MS = 10 * 60 * 1000;
const MAX_ATTEMPTS = 5;
const PASSWORD_RESET_DESTINATION_PREFIX = 'password-reset:';
const PASSWORD_RESET_WINDOW_MS = 15 * 60 * 1000;
const PASSWORD_RESET_MAX_REQUESTS = 3;
const PASSWORD_RESET_RESEND_COOLDOWN_MS = 60 * 1000;

type ResetRequestWindow = {
  count: number;
  resetAt: number;
  lastSentAt: number;
};

@Injectable()
export class VerificationService {
  private readonly logger = new Logger(VerificationService.name);
  private readonly passwordResetRequests = new Map<string, ResetRequestWindow>();

  constructor(
    private prisma: PrismaService,
    private config: ConfigService,
  ) {}

  private pepper(): string {
    return (
      this.config.get<string>('VERIFICATION_OTP_PEPPER') ??
      this.config.get<string>('JWT_SECRET') ??
      'mobu-dev-pepper'
    );
  }

  private hashCode(userId: string, code: string): string {
    return crypto
      .createHmac('sha256', this.pepper())
      .update(`${userId}:${code}`)
      .digest('hex');
  }

  private timingSafeEqual(a: string, b: string): boolean {
    const ba = Buffer.from(a);
    const bb = Buffer.from(b);
    if (ba.length !== bb.length) return false;
    return crypto.timingSafeEqual(ba, bb);
  }

  private generateSixDigitCode(): string {
    const n = crypto.randomInt(0, 1_000_000);
    return n.toString().padStart(6, '0');
  }

  private normalizePhone(phone: string): string {
    const digits = (phone ?? '').replace(/\D/g, '');
    if (digits.length < 10 || digits.length > 15) {
      throw new BadRequestException('Informe um telefone válido');
    }
    return digits;
  }

  private maskPhone(phone: string): string {
    if (phone.length <= 4) return '****';
    return `${'*'.repeat(phone.length - 4)}${phone.slice(-4)}`;
  }

  private passwordResetDestination(phone: string): string {
    return `${PASSWORD_RESET_DESTINATION_PREFIX}${phone}`;
  }

  private enforcePasswordResetThrottle(phone: string) {
    const now = Date.now();
    const current = this.passwordResetRequests.get(phone);

    if (!current || current.resetAt <= now) {
      this.passwordResetRequests.set(phone, {
        count: 1,
        resetAt: now + PASSWORD_RESET_WINDOW_MS,
        lastSentAt: now,
      });
      return;
    }

    if (now - current.lastSentAt < PASSWORD_RESET_RESEND_COOLDOWN_MS) {
      throw new HttpException(
        'Aguarde um minuto antes de solicitar outro código.',
        HttpStatus.TOO_MANY_REQUESTS,
      );
    }

    if (current.count >= PASSWORD_RESET_MAX_REQUESTS) {
      throw new HttpException(
        'Muitas solicitações. Tente novamente em alguns minutos.',
        HttpStatus.TOO_MANY_REQUESTS,
      );
    }

    current.count += 1;
    current.lastSentAt = now;
  }

  private ensureStrongPassword(password: string) {
    if (password.length < 8 || !/[A-Za-z]/.test(password) || !/\d/.test(password)) {
      throw new BadRequestException('Use uma senha com pelo menos 8 caracteres, letras e números.');
    }
  }

  private genericPasswordResetAck(phone: string) {
    return {
      ok: true,
      message: 'Se existir uma conta com esse telefone, enviaremos um código de recuperação.',
      channel: 'SMS',
      destination: this.maskPhone(phone),
    };
  }

  async requestPhoneOtp(userId: string) {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user) throw new BadRequestException('Usuário não encontrado');
    if (user.phoneVerifiedAt) {
      return { ok: true, message: 'Telefone já verificado' };
    }

    await this.prisma.verificationChallenge.deleteMany({
      where: {
        userId,
        channel: VerificationChannel.SMS,
        destination: user.phone,
        consumedAt: null,
      },
    });

    const code = this.generateSixDigitCode();
    const expiresAt = new Date(Date.now() + CODE_TTL_MS);

    await this.prisma.verificationChallenge.create({
      data: {
        userId,
        channel: VerificationChannel.SMS,
        destination: user.phone,
        codeHash: this.hashCode(userId, code),
        expiresAt,
      },
    });

    await this.deliverSms(user.phone, `MobU: seu código de verificação é ${code}. Válido por 10 minutos.`);

    return {
      ok: true,
      message: 'Código enviado por SMS',
      devHint: this.config.get<string>('TWILIO_ACCOUNT_SID') ? undefined : code,
    };
  }

  async confirmPhoneOtp(userId: string, code: string) {
    const digits = code.replace(/\D/g, '').slice(0, 6);
    if (digits.length !== 6) throw new BadRequestException('Informe o código de 6 dígitos');

    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { phone: true },
    });
    if (!user) throw new BadRequestException('Usuário não encontrado');

    const challenge = await this.prisma.verificationChallenge.findFirst({
      where: {
        userId,
        channel: VerificationChannel.SMS,
        destination: user.phone,
        consumedAt: null,
      },
      orderBy: { createdAt: 'desc' },
    });

    if (!challenge || challenge.expiresAt < new Date()) {
      throw new BadRequestException('Código expirado ou inexistente. Solicite um novo.');
    }

    if (challenge.attempts >= MAX_ATTEMPTS) {
      throw new HttpException(
        'Muitas tentativas. Solicite um novo código.',
        HttpStatus.TOO_MANY_REQUESTS,
      );
    }

    await this.prisma.verificationChallenge.update({
      where: { id: challenge.id },
      data: { attempts: { increment: 1 } },
    });

    const expected = this.hashCode(userId, digits);
    if (!this.timingSafeEqual(challenge.codeHash, expected)) {
      throw new BadRequestException('Código inválido');
    }

    await this.prisma.$transaction([
      this.prisma.verificationChallenge.update({
        where: { id: challenge.id },
        data: { consumedAt: new Date() },
      }),
      this.prisma.user.update({
        where: { id: userId },
        data: { phoneVerifiedAt: new Date() },
      }),
    ]);

    return { ok: true, message: 'Telefone verificado' };
  }

  async requestPasswordResetOtp(phoneRaw: string) {
    const phone = this.normalizePhone(phoneRaw);
    const ack = this.genericPasswordResetAck(phone);

    this.enforcePasswordResetThrottle(phone);

    const user = await this.prisma.user.findUnique({
      where: { phone },
      select: { id: true, phone: true, blocked: true },
    });

    if (!user || user.blocked) {
      return ack;
    }

    const destination = this.passwordResetDestination(phone);

    await this.prisma.verificationChallenge.deleteMany({
      where: {
        userId: user.id,
        channel: VerificationChannel.SMS,
        destination,
        consumedAt: null,
      },
    });

    const code = this.generateSixDigitCode();
    const expiresAt = new Date(Date.now() + CODE_TTL_MS);

    await this.prisma.verificationChallenge.create({
      data: {
        userId: user.id,
        channel: VerificationChannel.SMS,
        destination,
        codeHash: this.hashCode(user.id, code),
        expiresAt,
      },
    });

    await this.deliverSms(
      user.phone,
      `MobU: seu código de recuperação de senha é ${code}. Válido por 10 minutos.`,
    );

    return {
      ...ack,
      expiresAt: expiresAt.toISOString(),
      devHint: this.config.get<string>('TWILIO_ACCOUNT_SID') ? undefined : code,
    };
  }

  async confirmPasswordReset(phoneRaw: string, code: string, newPassword: string) {
    const phone = this.normalizePhone(phoneRaw);
    const digits = code.replace(/\D/g, '').slice(0, 6);

    if (digits.length !== 6) throw new BadRequestException('Informe o código de 6 dígitos');
    this.ensureStrongPassword(newPassword);

    const user = await this.prisma.user.findUnique({
      where: { phone },
      select: { id: true, blocked: true },
    });

    if (!user || user.blocked) {
      throw new BadRequestException('Código inválido ou expirado. Solicite um novo.');
    }

    const challenge = await this.prisma.verificationChallenge.findFirst({
      where: {
        userId: user.id,
        channel: VerificationChannel.SMS,
        destination: this.passwordResetDestination(phone),
        consumedAt: null,
      },
      orderBy: { createdAt: 'desc' },
    });

    if (!challenge || challenge.expiresAt < new Date()) {
      throw new BadRequestException('Código inválido ou expirado. Solicite um novo.');
    }

    if (challenge.attempts >= MAX_ATTEMPTS) {
      throw new HttpException(
        'Muitas tentativas. Solicite um novo código.',
        HttpStatus.TOO_MANY_REQUESTS,
      );
    }

    await this.prisma.verificationChallenge.update({
      where: { id: challenge.id },
      data: { attempts: { increment: 1 } },
    });

    const expected = this.hashCode(user.id, digits);
    if (!this.timingSafeEqual(challenge.codeHash, expected)) {
      throw new BadRequestException('Código inválido ou expirado. Solicite um novo.');
    }

    const passwordHash = await bcrypt.hash(newPassword, 10);

    await this.prisma.$transaction([
      this.prisma.verificationChallenge.update({
        where: { id: challenge.id },
        data: { consumedAt: new Date() },
      }),
      this.prisma.user.update({
        where: { id: user.id },
        data: { passwordHash },
      }),
    ]);

    return { ok: true, message: 'Senha redefinida com sucesso' };
  }

  async setEmail(userId: string, email: string) {
    const normalized = email.trim().toLowerCase();
    const taken = await this.prisma.user.findFirst({
      where: {
        email: normalized,
        NOT: { id: userId },
      },
    });
    if (taken) throw new BadRequestException('E-mail já utilizado');

    await this.prisma.user.update({
      where: { id: userId },
      data: {
        email: normalized,
        emailVerifiedAt: null,
      },
    });

    return { ok: true, message: 'E-mail atualizado. Confirme com o código enviado.' };
  }

  async requestEmailOtp(userId: string) {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user?.email) throw new BadRequestException('Cadastre um e-mail antes');
    if (user.emailVerifiedAt) {
      return { ok: true, message: 'E-mail já verificado' };
    }

    await this.prisma.verificationChallenge.deleteMany({
      where: {
        userId,
        channel: VerificationChannel.EMAIL,
        consumedAt: null,
      },
    });

    const code = this.generateSixDigitCode();
    const expiresAt = new Date(Date.now() + CODE_TTL_MS);

    await this.prisma.verificationChallenge.create({
      data: {
        userId,
        channel: VerificationChannel.EMAIL,
        destination: user.email,
        codeHash: this.hashCode(userId, code),
        expiresAt,
      },
    });

    await this.deliverEmail(
      user.email,
      'MobU — verificação de e-mail',
      `Seu código MobU: ${code} (válido por 10 minutos)`,
    );

    return {
      ok: true,
      message: 'Código enviado por e-mail',
      devHint: code,
    };
  }

  async confirmEmailOtp(userId: string, code: string) {
    const digits = code.replace(/\D/g, '').slice(0, 6);
    if (digits.length !== 6) throw new BadRequestException('Informe o código de 6 dígitos');

    const challenge = await this.prisma.verificationChallenge.findFirst({
      where: {
        userId,
        channel: VerificationChannel.EMAIL,
        consumedAt: null,
      },
      orderBy: { createdAt: 'desc' },
    });

    if (!challenge || challenge.expiresAt < new Date()) {
      throw new BadRequestException('Código expirado ou inexistente.');
    }

    if (challenge.attempts >= MAX_ATTEMPTS) {
      throw new HttpException('Muitas tentativas.', HttpStatus.TOO_MANY_REQUESTS);
    }

    await this.prisma.verificationChallenge.update({
      where: { id: challenge.id },
      data: { attempts: { increment: 1 } },
    });

    const expected = this.hashCode(userId, digits);
    if (!this.timingSafeEqual(challenge.codeHash, expected)) {
      throw new BadRequestException('Código inválido');
    }

    await this.prisma.$transaction([
      this.prisma.verificationChallenge.update({
        where: { id: challenge.id },
        data: { consumedAt: new Date() },
      }),
      this.prisma.user.update({
        where: { id: userId },
        data: { emailVerifiedAt: new Date() },
      }),
    ]);

    return { ok: true, message: 'E-mail verificado' };
  }

  private async deliverSms(to: string, body: string) {
    const sid = this.config.get<string>('TWILIO_ACCOUNT_SID');
    const token = this.config.get<string>('TWILIO_AUTH_TOKEN');
    const from = this.config.get<string>('TWILIO_FROM_NUMBER');

    if (!sid || !token || !from) {
      this.logger.warn(`[SMS DEV] para ${to}: ${body}`);
      return;
    }

    const auth = Buffer.from(`${sid}:${token}`).toString('base64');
    const params = new URLSearchParams({
      To: to.startsWith('+') ? to : `+${to}`,
      From: from,
      Body: body,
    });

    const res = await fetch(`https://api.twilio.com/2010-04-01/Accounts/${sid}/Messages.json`, {
      method: 'POST',
      headers: {
        Authorization: `Basic ${auth}`,
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: params.toString(),
    });

    if (!res.ok) {
      const text = await res.text();
      this.logger.error(`Twilio falhou: ${res.status} ${text}`);
      throw new BadRequestException('Não foi possível enviar SMS agora');
    }
  }

  private async deliverEmail(to: string, subject: string, text: string) {
    const apiKey = this.config.get<string>('SENDGRID_API_KEY');
    const from = this.config.get<string>('SENDGRID_FROM_EMAIL');

    if (!apiKey || !from) {
      this.logger.warn(`[EMAIL DEV] para ${to} | ${subject} | ${text}`);
      return;
    }

    const res = await fetch('https://api.sendgrid.com/v3/mail/send', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${apiKey}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        personalizations: [{ to: [{ email: to }] }],
        from: { email: from },
        subject,
        content: [{ type: 'text/plain', value: text }],
      }),
    });

    if (!res.ok) {
      const errText = await res.text();
      this.logger.error(`SendGrid falhou: ${res.status} ${errText}`);
      throw new BadRequestException('Não foi possível enviar e-mail agora');
    }
  }

  async ensurePassengerPhoneVerified(userId: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { role: true, phoneVerifiedAt: true },
    });
    if (!user) throw new BadRequestException('Usuário não encontrado');
    if (user.role !== UserRole.PASSENGER) return;
    if (!user.phoneVerifiedAt) {
      throw new BadRequestException(
        'Confirme seu telefone por SMS antes de solicitar corridas (Perfil → Verificação).',
      );
    }
  }
}

import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  Logger,
  UnauthorizedException,
} from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { ConfigService } from '@nestjs/config';
import * as bcrypt from 'bcrypt';
import { LoginDto } from './dto/login.dto';
import { RegisterDto } from './dto/register.dto';
import { DriverApprovalStatus, UserRole } from '@prisma/client';
import { JwtService, JwtSignOptions } from '@nestjs/jwt';
import { RegisterDriverDto } from './dto/register-driver.dto';
import { VerificationService } from './verification.service';

@Injectable()
export class AuthService {
  private readonly logger = new Logger(AuthService.name);

  constructor(
    private prisma: PrismaService,
    private jwt: JwtService,
    private config: ConfigService,
    private verification: VerificationService,
  ) {}

  private signToken(payload: { sub: string; role: UserRole }) {
    const options: JwtSignOptions = { expiresIn: 60 * 60 * 24 * 7 };
    return this.jwt.sign(payload, options);
  }

  private onlyDigits(value?: string | null) {
    return (value ?? '').replace(/\D/g, '');
  }

  private cleanOptionalText(value?: string | null) {
    const clean = value?.trim();
    return clean ? clean : null;
  }

  private isValidCpf(cpf: string) {
    if (!/^\d{11}$/.test(cpf)) return false;
    if (/^(\d)\1{10}$/.test(cpf)) return false;

    const calcDigit = (baseLength: number) => {
      let sum = 0;
      for (let i = 0; i < baseLength; i++) {
        sum += Number(cpf[i]) * (baseLength + 1 - i);
      }
      const rest = (sum * 10) % 11;
      return rest === 10 ? 0 : rest;
    };

    return calcDigit(9) === Number(cpf[9]) && calcDigit(10) === Number(cpf[10]);
  }

  private parsePassengerBirthDate(value?: string) {
    if (!value) throw new BadRequestException('Informe a data de nascimento');

    const birthDate = new Date(value);
    if (Number.isNaN(birthDate.getTime())) {
      throw new BadRequestException('Data de nascimento inválida');
    }

    const today = new Date();
    const age = today.getFullYear() - birthDate.getFullYear();
    const hadBirthday =
      today.getMonth() > birthDate.getMonth() ||
      (today.getMonth() === birthDate.getMonth() && today.getDate() >= birthDate.getDate());
    const realAge = hadBirthday ? age : age - 1;

    if (birthDate > today || realAge < 13 || realAge > 120) {
      throw new BadRequestException('Informe uma data de nascimento válida');
    }

    return birthDate;
  }

  private assertPassengerRegistration(dto: RegisterDto) {
    const name = dto.name?.trim();
    const email = dto.email?.trim().toLowerCase();
    const cpf = this.onlyDigits(dto.cpf);
    const emergencyContactName = this.cleanOptionalText(dto.emergencyContactName);
    const emergencyContactPhone = this.onlyDigits(dto.emergencyContactPhone) || null;

    if (!name || name.length < 3) {
      throw new BadRequestException('Informe seu nome completo');
    }
    if (!email) {
      throw new BadRequestException('Informe um e-mail válido');
    }
    if (!cpf || !this.isValidCpf(cpf)) {
      throw new BadRequestException('Informe um CPF válido');
    }
    if (!dto.acceptedTerms || !dto.acceptedPrivacy) {
      throw new BadRequestException('Aceite os termos de uso e a política de privacidade');
    }

    return {
      name,
      email,
      cpf,
      birthDate: this.parsePassengerBirthDate(dto.birthDate),
      emergencyContactName,
      emergencyContactPhone,
      accessibilityNotes: this.cleanOptionalText(dto.accessibilityNotes),
    };
  }

  async register(dto: RegisterDto) {
    const role = UserRole.PASSENGER;
    const phone = this.onlyDigits(dto.phone);
    if (phone.length < 10 || phone.length > 15) {
      throw new BadRequestException('Informe um telefone válido');
    }

    const passengerData = this.assertPassengerRegistration(dto);

    const exists = await this.prisma.user.findFirst({
      where: {
        OR: [
          { phone },
          ...(passengerData?.email ? [{ email: passengerData.email }] : []),
        ],
      },
    });
    if (exists?.phone === phone) throw new BadRequestException('Telefone já cadastrado');
    if (passengerData?.email && exists?.email === passengerData.email) {
      throw new BadRequestException('E-mail já cadastrado');
    }

    if (passengerData) {
      const cpfExists = await this.prisma.passengerProfile.findUnique({
        where: { cpf: passengerData.cpf },
      });
      if (cpfExists) throw new BadRequestException('CPF já cadastrado');
    }

    const passwordHash = await bcrypt.hash(dto.password, 10);

    const user = await this.prisma.$transaction(async (tx) => {
      const createdUser = await tx.user.create({
        data: {
          phone,
          name: passengerData?.name ?? dto.name,
          email: passengerData?.email ?? dto.email?.trim().toLowerCase() ?? null,
          role,
          passwordHash,
        },
        select: {
          id: true,
          phone: true,
          name: true,
          role: true,
          createdAt: true,
        },
      });

      if (createdUser.role === UserRole.PASSENGER) {
        const acceptedAt = new Date();
        await tx.passengerProfile.create({
          data: {
            userId: createdUser.id,
            cpf: passengerData.cpf,
            birthDate: passengerData.birthDate,
            emergencyContactName: passengerData.emergencyContactName,
            emergencyContactPhone: passengerData.emergencyContactPhone,
            accessibilityNotes: passengerData.accessibilityNotes,
            termsAcceptedAt: acceptedAt,
            privacyAcceptedAt: acceptedAt,
          },
        });
      }

      return createdUser;
    });

    const accessToken = this.signToken({ sub: user.id, role: user.role });

    // Dispara e-mail de verificação em background (não bloqueia o registro)
    const savedEmail = passengerData?.email ?? dto.email?.trim().toLowerCase();
    if (savedEmail) {
      this.verification.requestEmailOtp(user.id).catch((err: unknown) => {
        const msg = err instanceof Error ? err.message : String(err);
        this.logger.warn(`Falha ao enviar e-mail de verificação para ${user.id}: ${msg}`);
      });
    }

    return { user, accessToken };
  }

  async login(dto: LoginDto) {
    const user = await this.prisma.user.findUnique({
      where: { phone: dto.phone },
    });

    if (!user) throw new UnauthorizedException('Credenciais inválidas');

    const ok = await bcrypt.compare(dto.password, user.passwordHash);
    if (!ok) throw new UnauthorizedException('Credenciais inválidas');

    if (user.blocked) {
      throw new ForbiddenException('Conta bloqueada. Entre em contato com o suporte.');
    }

    if (user.role === UserRole.DRIVER) {
      const profile = await this.prisma.driverProfile.findUnique({
        where: { userId: user.id },
      });

      if (!profile) {
        await this.prisma.driverProfile.create({
          data: { userId: user.id, online: false, available: false },
        });
      }
    }

    const accessToken = this.signToken({ sub: user.id, role: user.role });
    return {
      user: { id: user.id, phone: user.phone, name: user.name, role: user.role },
      accessToken,
    };
  }

  async me(userId: string) {
    return this.prisma.user.findUnique({
      where: { id: userId },
      select: {
        id: true,
        phone: true,
        name: true,
        email: true,
        role: true,
        profilePhotoUrl: true,
        phoneVerifiedAt: true,
        emailVerifiedAt: true,
        blocked: true,
        createdAt: true,
      },
    });
  }

  async updateProfile(userId: string, name: string) {
    return this.prisma.user.update({
      where: { id: userId },
      data: { name },
      select: { id: true, phone: true, name: true, role: true, profilePhotoUrl: true },
    });
  }

  async updatePhotoUrl(userId: string, photoUrl: string) {
    return this.prisma.user.update({
      where: { id: userId },
      data: { profilePhotoUrl: photoUrl },
      select: { id: true, phone: true, name: true, role: true, profilePhotoUrl: true },
    });
  }

  async changePassword(userId: string, currentPassword: string, newPassword: string) {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user) throw new BadRequestException('Usuário não encontrado');
    const ok = await bcrypt.compare(currentPassword, user.passwordHash);
    if (!ok) throw new BadRequestException('Senha atual incorreta');
    const passwordHash = await bcrypt.hash(newPassword, 10);
    await this.prisma.user.update({ where: { id: userId }, data: { passwordHash } });
    return { ok: true, message: 'Senha alterada com sucesso' };
  }

  async deleteAccount(userId: string) {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user) throw new BadRequestException('Usuário não encontrado');

    // Anonymise instead of hard-delete to preserve FK integrity on rides/reviews
    const tombstone = `deleted_${userId}`;
    await this.prisma.$transaction([
      this.prisma.passengerProfile.deleteMany({ where: { userId } }),
      this.prisma.user.update({
        where: { id: userId },
        data: {
          name: null,
          phone: tombstone,
          email: null,
          profilePhotoUrl: null,
          passwordHash: tombstone,
          blocked: true,
          blockedAt: new Date(),
        },
      }),
    ]);

    return { ok: true, message: 'Conta excluída com sucesso' };
  }

  async registerDriver(dto: RegisterDriverDto) {
    const exists = await this.prisma.user.findUnique({
      where: { phone: dto.phone },
    });
    if (exists) throw new BadRequestException('Telefone já cadastrado');

    const passwordHash = await bcrypt.hash(dto.password, 10);

    const user = await this.prisma.$transaction(async (tx) => {
      const createdUser = await tx.user.create({
        data: {
          phone: dto.phone,
          name: dto.name,
          role: UserRole.DRIVER,
          passwordHash,
        },
        select: {
          id: true,
          phone: true,
          name: true,
          role: true,
          createdAt: true,
        },
      });

      await tx.driverProfile.create({
        data: {
          userId: createdUser.id,
          online: false,
          available: false,
          cnhImageUrl: dto.cnhImageUrl,
          cnhNumber: dto.cnhNumber,
          cnhCategory: dto.cnhCategory,
          hasEar: dto.hasEar,
          pixQrPayload: dto.pixQrPayload ?? null,
          approvalStatus: DriverApprovalStatus.PENDING,
        },
      });

      return createdUser;
    });

    const accessToken = this.signToken({ sub: user.id, role: user.role });
    return {
      user,
      accessToken,
      message: 'Cadastro de motorista enviado para análise',
    };
  }

  async saveFcmToken(userId: string, token: string) {
    await this.prisma.user.update({
      where: { id: userId },
      data: { fcmToken: token },
    });
    return { ok: true };
  }
}

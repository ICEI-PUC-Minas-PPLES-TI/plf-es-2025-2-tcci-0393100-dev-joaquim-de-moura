import {
  BadRequestException,
  Injectable,
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

@Injectable()
export class AuthService {
  constructor(
    private prisma: PrismaService,
    private jwt: JwtService,
    private config: ConfigService,
  ) {}

  private signToken(payload: { sub: string; role: UserRole }) {
    const options: JwtSignOptions = { expiresIn: 60 * 60 * 24 * 7 };
    return this.jwt.sign(payload, options);
  }

  async register(dto: RegisterDto) {
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
          role: dto.role ?? UserRole.PASSENGER,
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

      if (createdUser.role === UserRole.DRIVER) {
        await tx.driverProfile.create({
          data: {
            userId: createdUser.id,
            online: false,
            available: false,
          },
        });
      }

      return createdUser;
    });

    const accessToken = this.signToken({ sub: user.id, role: user.role });

    return { user, accessToken };
  }

  async login(dto: LoginDto) {
  console.log('LOGIN DTO:', dto);

  const user = await this.prisma.user.findUnique({
    where: { phone: dto.phone },
  });

  console.log('USER ENCONTRADO:', user);

  if (!user) throw new UnauthorizedException('Credenciais inválidas');

  const ok = await bcrypt.compare(dto.password, user.passwordHash);
  console.log('SENHA OK?', ok);

  if (!ok) throw new UnauthorizedException('Credenciais inválidas');

  if (user.role === UserRole.DRIVER) {
    const profile = await this.prisma.driverProfile.findUnique({
      where: { userId: user.id },
    });

    if (!profile) {
      await this.prisma.driverProfile.create({
        data: {
          userId: user.id,
          online: false,
          available: false,
        },
      });
    }
  }

  const accessToken = this.signToken({ sub: user.id, role: user.role });

  return {
    user: {
      id: user.id,
      phone: user.phone,
      name: user.name,
      role: user.role,
    },
    accessToken,
  };
}

  async me(userId: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { id: true, phone: true, name: true, role: true, createdAt: true },
    });
    return user;
  }

  async registerDriver(dto: RegisterDriverDto) {
  const exists = await this.prisma.user.findUnique({
    where: { phone: dto.phone },
  });

  if (exists) {
    throw new BadRequestException('Telefone já cadastrado');
  }

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
}
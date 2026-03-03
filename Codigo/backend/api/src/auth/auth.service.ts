import { BadRequestException, Injectable, UnauthorizedException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { ConfigService } from '@nestjs/config';
import * as bcrypt from 'bcrypt';
import { LoginDto } from './dto/login.dto';
import { RegisterDto } from './dto/register.dto';
import { UserRole } from '@prisma/client';
import { JwtService, JwtSignOptions } from '@nestjs/jwt';

@Injectable()
export class AuthService {
  constructor(
    private prisma: PrismaService,
    private jwt: JwtService,
    private config: ConfigService,
  ) {}

  private signToken(payload: { sub: string; role: UserRole }) {
    // 7 dias em segundos
    const options: JwtSignOptions = { expiresIn: 60 * 60 * 24 * 7 };
    return this.jwt.sign(payload, options);
    }

  async register(dto: RegisterDto) {
    const exists = await this.prisma.user.findUnique({ where: { phone: dto.phone } });
    if (exists) throw new BadRequestException('Telefone já cadastrado');

    const passwordHash = await bcrypt.hash(dto.password, 10);

    const user = await this.prisma.user.create({
      data: {
        phone: dto.phone,
        name: dto.name,
        role: dto.role ?? UserRole.PASSENGER,
        passwordHash,
      },
      select: { id: true, phone: true, name: true, role: true, createdAt: true },
    });

    const accessToken = this.signToken({ sub: user.id, role: user.role });

    return { user, accessToken };
  }

  async login(dto: LoginDto) {
    const user = await this.prisma.user.findUnique({ where: { phone: dto.phone } });
    if (!user) throw new UnauthorizedException('Credenciais inválidas');

    const ok = await bcrypt.compare(dto.password, user.passwordHash);
    if (!ok) throw new UnauthorizedException('Credenciais inválidas');

    const accessToken = this.signToken({ sub: user.id, role: user.role });

    return {
      user: { id: user.id, phone: user.phone, name: user.name, role: user.role },
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
}
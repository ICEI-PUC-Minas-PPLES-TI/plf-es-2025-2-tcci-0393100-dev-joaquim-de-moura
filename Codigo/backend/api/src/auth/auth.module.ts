import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { ConfigModule, ConfigService } from '@nestjs/config';

import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { JwtStrategy } from './jwt.strategy';
import { VerificationService } from './verification.service';

import { PrismaModule } from '../prisma/prisma.module'; 

@Module({
  imports: [
    PrismaModule, 
    ConfigModule,
    PassportModule,
    JwtModule.registerAsync({
    imports: [ConfigModule],
    inject: [ConfigService],
    useFactory: (config: ConfigService) => ({
        secret: config.get<string>('JWT_SECRET')!, // garante que não é undefined
    }),
    }),
  ],
  controllers: [AuthController],
  providers: [AuthService, JwtStrategy, VerificationService],
  exports: [JwtModule, PassportModule],
})
export class AuthModule {}

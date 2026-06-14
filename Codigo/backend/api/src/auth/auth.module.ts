import { MiddlewareConsumer, Module, NestModule, RequestMethod } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { ConfigModule, ConfigService } from '@nestjs/config';

import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { JwtStrategy } from './jwt.strategy';
import { VerificationService } from './verification.service';
import { createRateLimitMiddleware } from '../common/middleware/rate-limit.middleware';

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
        secret: config.get<string>('JWT_SECRET')!,
      }),
    }),
  ],
  controllers: [AuthController],
  providers: [AuthService, JwtStrategy, VerificationService],
  exports: [JwtModule, PassportModule],
})
export class AuthModule implements NestModule {
  configure(consumer: MiddlewareConsumer) {
    const authRateLimit = createRateLimitMiddleware({
      windowMs: 15 * 60 * 1000,
      max: 10,
      message: 'Muitas tentativas. Aguarde 15 minutos antes de tentar novamente.',
    });
    consumer.apply(authRateLimit).forRoutes(
      { path: 'auth/login', method: RequestMethod.POST },
      { path: 'auth/register', method: RequestMethod.POST },
      { path: 'auth/register-driver', method: RequestMethod.POST },
      { path: 'auth/reset-password/request', method: RequestMethod.POST },
    );
  }
}

import {
  BadRequestException,
  Body,
  Controller,
  Delete,
  Get,
  Patch,
  Post,
  Req,
  UploadedFile,
  UseGuards,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { mkdirSync } from 'fs';
import { join } from 'path';
import { AuthService } from './auth.service';
import { VerificationService } from './verification.service';
import { RegisterDto } from './dto/register.dto';
import { RegisterDriverDto } from './dto/register-driver.dto';
import { LoginDto } from './dto/login.dto';
import { ConfirmVerificationDto } from './dto/confirm-verification.dto';
import { ConfirmPasswordResetDto } from './dto/confirm-password-reset.dto';
import { RequestPasswordResetDto } from './dto/request-password-reset.dto';
import { SetEmailDto } from './dto/set-email.dto';
import { JwtAuthGuard } from './jwt.guard';

@Controller('auth')
export class AuthController {
  constructor(
    private readonly authService: AuthService,
    private readonly verificationService: VerificationService,
  ) {}

  @Post('register')
  register(@Body() dto: RegisterDto) {
    return this.authService.register(dto);
  }

  @Post('register-driver')
  registerDriver(@Body() dto: RegisterDriverDto) {
    return this.authService.registerDriver(dto);
  }

  @Post('login')
  login(@Body() dto: LoginDto) {
    return this.authService.login(dto);
  }

  @UseGuards(JwtAuthGuard)
  @Get('me')
  me(@Req() req) {
    return this.authService.me(req.user.userId);
  }

  @UseGuards(JwtAuthGuard)
  @Patch('fcm-token')
  saveFcmToken(@Req() req, @Body('token') token: string) {
    return this.authService.saveFcmToken(req.user.userId, token);
  }

  @UseGuards(JwtAuthGuard)
  @Post('verification/phone/request')
  requestPhoneVerification(@Req() req) {
    return this.verificationService.requestPhoneOtp(req.user.userId);
  }

  @UseGuards(JwtAuthGuard)
  @Post('verification/phone/confirm')
  confirmPhoneVerification(@Req() req, @Body() dto: ConfirmVerificationDto) {
    return this.verificationService.confirmPhoneOtp(req.user.userId, dto.code);
  }

  @UseGuards(JwtAuthGuard)
  @Patch('email')
  setEmail(@Req() req, @Body() dto: SetEmailDto) {
    return this.verificationService.setEmail(req.user.userId, dto.email);
  }

  @UseGuards(JwtAuthGuard)
  @Post('verification/email/request')
  requestEmailVerification(@Req() req) {
    return this.verificationService.requestEmailOtp(req.user.userId);
  }

  @UseGuards(JwtAuthGuard)
  @Post('verification/email/confirm')
  confirmEmailVerification(@Req() req, @Body() dto: ConfirmVerificationDto) {
    return this.verificationService.confirmEmailOtp(req.user.userId, dto.code);
  }

  @Post('reset-password/request')
  requestPasswordReset(@Body() dto: RequestPasswordResetDto) {
    return this.verificationService.requestPasswordResetOtp(dto.phone);
  }

  @Post('reset-password/confirm')
  confirmPasswordReset(@Body() dto: ConfirmPasswordResetDto) {
    return this.verificationService.confirmPasswordReset(
      dto.phone,
      dto.code,
      dto.newPassword,
    );
  }

  @Post('reset-password')
  resetPassword(@Body() dto: ConfirmPasswordResetDto) {
    return this.verificationService.confirmPasswordReset(
      dto.phone,
      dto.code,
      dto.newPassword,
    );
  }

  @UseGuards(JwtAuthGuard)
  @Patch('profile')
  updateProfile(@Req() req, @Body() body: { name: string }) {
    return this.authService.updateProfile(req.user.userId, body.name);
  }

  @UseGuards(JwtAuthGuard)
  @Patch('change-password')
  changePassword(@Req() req, @Body() body: { currentPassword: string; newPassword: string }) {
    return this.authService.changePassword(req.user.userId, body.currentPassword, body.newPassword);
  }

  @UseGuards(JwtAuthGuard)
  @Delete('account')
  deleteAccount(@Req() req) {
    return this.authService.deleteAccount(req.user.userId);
  }

  @UseGuards(JwtAuthGuard)
  @Post('upload-photo')
  @UseInterceptors(
    FileInterceptor('photo', {
      storage: diskStorage({
        destination: (_req, _file, cb) => {
          const dir = join(process.cwd(), 'uploads', 'profile_photos');
          mkdirSync(dir, { recursive: true });
          cb(null, dir);
        },
        filename: (req, file, cb) => {
          const ext = extname(file.originalname).toLowerCase() || '.jpg';
          const safeExt = ['.jpg', '.jpeg', '.png', '.webp'].includes(ext) ? ext : '.jpg';
          cb(null, `${(req as any).user.userId}_${Date.now()}${safeExt}`);
        },
      }),
      limits: { fileSize: 5 * 1024 * 1024 },
      fileFilter: (_req, file, cb) => {
        if (file.mimetype.startsWith('image/')) cb(null, true);
        else cb(new BadRequestException('Apenas imagens são permitidas') as any, false);
      },
    }),
  )
  uploadPhoto(@Req() req, @UploadedFile() file: Express.Multer.File) {
    if (!file) throw new BadRequestException('Nenhum arquivo enviado');
    const photoUrl = `/uploads/profile_photos/${file.filename}`;
    return this.authService.updatePhotoUrl(req.user.userId, photoUrl);
  }
}

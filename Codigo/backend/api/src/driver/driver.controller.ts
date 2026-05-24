import {
  BadRequestException,
  Body,
  Controller,
  Get,
  Post,
  Patch,
  Param,
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
import { JwtAuthGuard } from '../auth/jwt.guard';
import { DriverService } from './driver.service';
import { UpdateDriverStatusDto } from './dto/update-driver-status.dto';
import { UpdateDriverProfileDto } from './dto/update-driver-profile.dto';

@UseGuards(JwtAuthGuard)
@Controller('driver')
export class DriverController {
  constructor(private readonly driverService: DriverService) {}

  @Get('me')
  getDriverMe(@Req() req: any) {
    return this.driverService.getDriverMe(req.user.userId);
  }

  @Post('status')
  updateStatus(@Req() req: any, @Body() dto: UpdateDriverStatusDto) {
    return this.driverService.updateStatus(req.user.userId, dto);
  }

  @Patch('profile')
  updateProfile(@Req() req: any, @Body() dto: UpdateDriverProfileDto) {
    return this.driverService.updateProfile(req.user.userId, dto);
  }

  @Get('rides/history')
  getRideHistory(@Req() req: any) {
    return this.driverService.getRideHistory(req.user.userId);
  }

  @Get('financial/balance')
  getBalance(@Req() req: any) {
    return this.driverService.getDriverBalance(req.user.userId);
  }

  @Post('financial/payment-request')
  createPaymentRequest(
    @Req() req: any,
    @Body('amountCents') amountCents: number,
    @Body('notes') notes: string,
  ) {
    return this.driverService.createPaymentRequest(req.user.userId, amountCents, notes ?? null);
  }

  @Get('financial/payment-requests')
  getPaymentRequests(@Req() req: any) {
    return this.driverService.getPaymentRequests(req.user.userId);
  }

  @Get('billing/cycles')
  getBillingCycles(@Req() req: any) {
    return this.driverService.getBillingCycles(req.user.userId);
  }

  @Patch('location')
  updateLocation(
    @Req() req: any,
    @Body() body: { lat: number; lng: number },
  ) {
    return this.driverService.updateLocation(req.user.userId, body.lat, body.lng);
  }

  @Get('available')
  getAvailableDrivers() {
    return this.driverService.getAvailableDrivers();
  }

  @Get('rides/pending')
  getPendingRides(@Req() req: any) {
    return this.driverService.getPendingRides(req.user.userId);
  }

  @Post('rides/:rideId/accept')
  acceptRide(@Req() req: any, @Param('rideId') rideId: string) {
    return this.driverService.acceptRide(req.user.userId, rideId);
  }

  @Post('rides/:rideId/reject')
  rejectRide(@Req() req: any, @Param('rideId') rideId: string) {
    return this.driverService.rejectRide(req.user.userId, rideId);
  }

  @Get('rides/current')
  getCurrentRide(@Req() req: any) {
    return this.driverService.getCurrentRide(req.user.userId);
  }

  @Patch('rides/:rideId/arriving')
  markArriving(@Req() req: any, @Param('rideId') rideId: string) {
    return this.driverService.markArriving(req.user.userId, rideId);
  }

  @Patch('rides/:rideId/arrived')
  markArrived(@Req() req: any, @Param('rideId') rideId: string) {
    return this.driverService.markArrived(req.user.userId, rideId);
  }

  @Patch('rides/:rideId/start')
  startRide(@Req() req: any, @Param('rideId') rideId: string) {
    return this.driverService.startRide(req.user.userId, rideId);
  }

  @Patch('rides/:rideId/finish')
  finishRide(@Req() req: any, @Param('rideId') rideId: string) {
    return this.driverService.finishRide(req.user.userId, rideId);
  }

  @Patch('rides/:rideId/cancel')
  cancelRide(@Req() req: any, @Param('rideId') rideId: string) {
    return this.driverService.cancelRide(req.user.userId, rideId);
  }

  @Post('financial/payment-requests/:id/receipt')
  @UseInterceptors(
    FileInterceptor('receipt', {
      storage: diskStorage({
        destination: (_req, _file, cb) => {
          const dir = join(process.cwd(), 'uploads', 'payment_receipts');
          mkdirSync(dir, { recursive: true });
          cb(null, dir);
        },
        filename: (req, file, cb) => {
          const ext = extname(file.originalname).toLowerCase() || '.jpg';
          const safeExt = ['.jpg', '.jpeg', '.png', '.webp', '.pdf'].includes(ext) ? ext : '.jpg';
          cb(null, `${(req as any).user.userId}_receipt_${Date.now()}${safeExt}`);
        },
      }),
      limits: { fileSize: 10 * 1024 * 1024 },
      fileFilter: (_req, file, cb) => {
        if (file.mimetype.startsWith('image/') || file.mimetype === 'application/pdf') cb(null, true);
        else cb(new BadRequestException('Apenas imagens ou PDF são permitidos') as any, false);
      },
    }),
  )
  uploadPaymentReceipt(
    @Req() req: any,
    @Param('id') id: string,
    @UploadedFile() file: Express.Multer.File,
  ) {
    if (!file) throw new BadRequestException('Nenhum arquivo enviado');
    const receiptUrl = `/uploads/payment_receipts/${file.filename}`;
    return this.driverService.uploadPaymentRequestReceipt(req.user.userId, id, receiptUrl);
  }

  @Post('upload-photo')
  @UseInterceptors(
    FileInterceptor('photo', {
      storage: diskStorage({
        destination: (_req, _file, cb) => {
          const dir = join(process.cwd(), 'uploads', 'driver_photos');
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
  uploadDriverPhoto(@Req() req: any, @UploadedFile() file: Express.Multer.File) {
    if (!file) throw new BadRequestException('Nenhum arquivo enviado');
    const photoUrl = `/uploads/driver_photos/${file.filename}`;
    return this.driverService.updateDriverPhotoUrl(req.user.userId, photoUrl);
  }

  @Post('upload-cnh')
  @UseInterceptors(
    FileInterceptor('photo', {
      storage: diskStorage({
        destination: (_req, _file, cb) => {
          const dir = join(process.cwd(), 'uploads', 'cnh_photos');
          mkdirSync(dir, { recursive: true });
          cb(null, dir);
        },
        filename: (req, file, cb) => {
          const ext = extname(file.originalname).toLowerCase() || '.jpg';
          const safeExt = ['.jpg', '.jpeg', '.png', '.webp'].includes(ext) ? ext : '.jpg';
          cb(null, `${(req as any).user.userId}_cnh_${Date.now()}${safeExt}`);
        },
      }),
      limits: { fileSize: 10 * 1024 * 1024 },
      fileFilter: (_req, file, cb) => {
        if (file.mimetype.startsWith('image/')) cb(null, true);
        else cb(new BadRequestException('Apenas imagens são permitidas') as any, false);
      },
    }),
  )
  uploadCnhPhoto(@Req() req: any, @UploadedFile() file: Express.Multer.File) {
    if (!file) throw new BadRequestException('Nenhum arquivo enviado');
    const cnhImageUrl = `/uploads/cnh_photos/${file.filename}`;
    return this.driverService.updateCnhImageUrl(req.user.userId, cnhImageUrl);
  }
}

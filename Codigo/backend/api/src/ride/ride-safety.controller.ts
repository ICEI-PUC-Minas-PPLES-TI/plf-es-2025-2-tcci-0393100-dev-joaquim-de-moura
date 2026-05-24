import { Body, Controller, Get, Param, Post, Req, UseGuards } from '@nestjs/common';
import { UserRole } from '@prisma/client';
import { JwtAuthGuard } from '../auth/jwt.guard';
import { RideAuditService } from './ride-audit.service';
import { SafetyAudioDto } from './dto/safety-audio.dto';
import { SafetyEmergencyDto } from './dto/safety-emergency.dto';
import { SafetyShareDto } from './dto/safety-share.dto';

type AuthedRequest = { user: { userId: string; role: UserRole } };

@UseGuards(JwtAuthGuard)
@Controller('rides')
export class RideSafetyController {
  constructor(private readonly audit: RideAuditService) {}

  @Post(':id/safety/share')
  async shareTrip(
    @Req() req: AuthedRequest,
    @Param('id') rideId: string,
    @Body() body: SafetyShareDto,
  ) {
    await this.audit.assertRideParticipant(rideId, req.user);
    await this.audit.append(rideId, req.user.userId, 'SHARE_TRIP', {
      channel: body.channel ?? 'unknown',
    });
    return {
      ok: true,
      message: 'Compartilhamento registrado para fins de disputa/suporte.',
    };
  }

  @Post(':id/safety/emergency')
  async emergency(
    @Req() req: AuthedRequest,
    @Param('id') rideId: string,
    @Body() body: SafetyEmergencyDto,
  ) {
    await this.audit.assertRideParticipant(rideId, req.user);
    await this.audit.append(rideId, req.user.userId, 'EMERGENCY_TAP', {
      note: body.note ?? null,
      lat: body.lat ?? null,
      lng: body.lng ?? null,
    });
    return {
      ok: true,
      message:
        'Emergência registrada. Em situação real, ligue 190 (Polícia), 192 (SAMU) ou 193 (Bombeiros), conforme o caso.',
      emergencyNumbers: ['190', '192', '193'],
    };
  }

  @Post(':id/safety/audio')
  async audio(
    @Req() req: AuthedRequest,
    @Param('id') rideId: string,
    @Body() body: SafetyAudioDto,
  ) {
    await this.audit.assertRideParticipant(rideId, req.user);
    const action =
      body.phase === 'start' ? 'AUDIO_RECORDING_START' : 'AUDIO_RECORDING_STOP';
    await this.audit.append(rideId, req.user.userId, action, {
      consentAccepted: body.consentAccepted ?? false,
      durationMs: body.durationMs ?? null,
      legalNotice:
        'Use gravação apenas onde permitido por lei; o áudio fica no dispositivo até você optar por enviar.',
    });
    return { ok: true };
  }

  @Get(':id/safety/audit')
  async listAudit(@Req() req: AuthedRequest, @Param('id') rideId: string) {
    return this.audit.listForRide(rideId, req.user);
  }
}

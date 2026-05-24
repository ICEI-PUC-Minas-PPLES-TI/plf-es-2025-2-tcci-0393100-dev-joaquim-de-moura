import {
  Body,
  Controller,
  Get,
  Param,
  Post,
  Req,
  UseGuards,
} from '@nestjs/common';
import { JwtAuthGuard } from '../auth/jwt.guard';
import { ChatService } from './chat.service';

@Controller('rides')
export class ChatController {
  constructor(private readonly chatService: ChatService) {}

  @UseGuards(JwtAuthGuard)
  @Post(':rideId/messages')
  sendMessage(
    @Req() req: any,
    @Param('rideId') rideId: string,
    @Body() body: { content: string },
  ) {
    const senderRole: 'PASSENGER' | 'DRIVER' =
      req.user.role === 'DRIVER' ? 'DRIVER' : 'PASSENGER';

    return this.chatService.sendMessage(
      req.user.userId,
      rideId,
      body.content,
      senderRole,
    );
  }

  @UseGuards(JwtAuthGuard)
  @Get(':rideId/messages')
  getMessages(@Req() req: any, @Param('rideId') rideId: string) {
    return this.chatService.getMessages(req.user.userId, rideId);
  }
}

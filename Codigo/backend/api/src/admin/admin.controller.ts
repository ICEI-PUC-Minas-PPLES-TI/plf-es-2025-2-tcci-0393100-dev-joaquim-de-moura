import { Body, Controller, Get, Param, Patch } from '@nestjs/common';
import { AdminService } from './admin.service';

@Controller('admin')
export class AdminController {
  constructor(private readonly adminService: AdminService) {}

  @Get('drivers/pending')
  listPendingDrivers() {
    return this.adminService.listPendingDrivers();
  }

  @Patch('drivers/:id/approve')
  approveDriver(@Param('id') id: string) {
    return this.adminService.approveDriver(id);
  }

  @Patch('drivers/:id/reject')
  rejectDriver(
    @Param('id') id: string,
    @Body('reason') reason: string,
  ) {
    return this.adminService.rejectDriver(id, reason);
  }
}
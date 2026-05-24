import { Body, Controller, Delete, Get, Param, Patch, Post, Query, Req, UseGuards } from '@nestjs/common';
import { AdminService } from './admin.service';
import { JwtAuthGuard } from '../auth/jwt.guard';
import { AdminGuard } from '../auth/admin.guard';
import { CreatePricingConfigDto } from './dto/create-pricing-config.dto';
import { ListRidesQueryDto } from './dto/list-rides-query.dto';
import { ListReportsQueryDto } from './dto/list-reports-query.dto';
import {
  CreateOperationRegionDto,
  UpdateOperationRegionDto,
} from './dto/operation-region.dto';

@UseGuards(JwtAuthGuard, AdminGuard)
@Controller('admin')
export class AdminController {
  constructor(private readonly adminService: AdminService) {}

  @Get('drivers/pending')
  listPendingDrivers() {
    return this.adminService.listPendingDrivers();
  }

  @Get('drivers')
  listDrivers() {
    return this.adminService.listDrivers();
  }

  @Get('drivers/all')
  listAllDrivers() {
    return this.adminService.listDrivers();
  }

  @Get('drivers/approved')
  listApprovedDrivers() {
    return this.adminService.listDrivers('APPROVED');
  }

  @Get('drivers/rejected')
  listRejectedDrivers() {
    return this.adminService.listDrivers('REJECTED');
  }

  @Get('rides')
  listRides(@Query() query: ListRidesQueryDto) {
    return this.adminService.listRides(query);
  }

  @Get('stats')
  stats() {
    return this.adminService.stats();
  }

  @Get('reports')
  reports(@Query() query: ListReportsQueryDto) {
    return this.adminService.reports(query);
  }

  @Get('passengers')
  listPassengers() {
    return this.adminService.listPassengers();
  }

  @Patch('users/:id/block')
  blockUser(@Param('id') id: string) {
    return this.adminService.blockUser(id);
  }

  @Patch('users/:id/unblock')
  unblockUser(@Param('id') id: string) {
    return this.adminService.unblockUser(id);
  }

  @Get('live')
  liveOperation() {
    return this.adminService.liveOperation();
  }

  @Get('pricing')
  listPricingConfigs() {
    return this.adminService.listPricingConfigs();
  }

  @Post('pricing')
  createPricingConfig(@Body() dto: CreatePricingConfigDto) {
    return this.adminService.createPricingConfig(dto);
  }

  @Patch('pricing/:id/activate')
  activatePricingConfig(@Param('id') id: string) {
    return this.adminService.activatePricingConfig(id);
  }

  @Patch('pricing/:id/deactivate')
  deactivatePricingConfig(@Param('id') id: string) {
    return this.adminService.deactivatePricingConfig(id);
  }

  @Delete('pricing/:id')
  deletePricingConfig(@Param('id') id: string) {
    return this.adminService.deletePricingConfig(id);
  }

  @Get('regions')
  listRegions() {
    return this.adminService.listRegions();
  }

  @Post('regions')
  createRegion(@Body() dto: CreateOperationRegionDto) {
    return this.adminService.createRegion(dto);
  }

  @Delete('regions/:id')
  deleteRegion(@Param('id') id: string) {
    return this.adminService.deleteRegion(id);
  }

  @Patch('regions/:id')
  updateRegion(
    @Param('id') id: string,
    @Body() dto: UpdateOperationRegionDto,
  ) {
    return this.adminService.updateRegion(id, dto);
  }

  @Get('financial')
  financialSummary() {
    return this.adminService.financialSummary();
  }

  @Post('financial/drivers/:driverId/settle')
  registerSettlement(
    @Req() req: any,
    @Param('driverId') driverId: string,
    @Body('amountCents') amountCents: number,
    @Body('notes') notes: string,
    @Body('method') method: string,
  ) {
    return this.adminService.registerSettlement(req.user.userId, driverId, amountCents, notes ?? null, method ?? 'PIX');
  }

  @Delete('financial/settlements/:id')
  deleteSettlement(@Param('id') id: string) {
    return this.adminService.deleteSettlement(id);
  }

  @Get('financial/drivers/:driverId/balance')
  driverBalance(@Param('driverId') driverId: string) {
    return this.adminService.driverBalance(driverId);
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

  @Get('financial/payment-requests')
  listPaymentRequests(@Query('status') status?: string) {
    const s = status as any;
    return this.adminService.listPaymentRequests(s || undefined);
  }

  @Patch('financial/payment-requests/:id/confirm')
  confirmPaymentRequest(@Req() req: any, @Param('id') id: string) {
    return this.adminService.confirmPaymentRequest(req.user.userId, id);
  }

  @Patch('financial/payment-requests/:id/reject')
  rejectPaymentRequest(
    @Req() req: any,
    @Param('id') id: string,
    @Body('reason') reason: string,
  ) {
    return this.adminService.rejectPaymentRequest(req.user.userId, id, reason);
  }

  @Get('config')
  getSystemConfig() {
    return this.adminService.getSystemConfig();
  }

  @Patch('config')
  updateSystemConfig(@Body('key') key: string, @Body('value') value: string) {
    return this.adminService.updateSystemConfig(key, value);
  }

  // ── Promo Codes ──────────────────────────────────────────────────────────────

  @Get('promo-codes')
  listPromoCodes() {
    return this.adminService.listPromoCodes();
  }

  @Post('promo-codes')
  createPromoCode(@Body() body: any) {
    return this.adminService.createPromoCode(body);
  }

  @Patch('promo-codes/:id/toggle')
  togglePromoCode(@Param('id') id: string) {
    return this.adminService.togglePromoCode(id);
  }

  @Delete('promo-codes/:id')
  deletePromoCode(@Param('id') id: string) {
    return this.adminService.deletePromoCode(id);
  }
}

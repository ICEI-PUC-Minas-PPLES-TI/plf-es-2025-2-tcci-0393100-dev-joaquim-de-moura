import { Test, TestingModule } from '@nestjs/testing';
import { PrismaService } from './prisma.service';

describe('PrismaService — Banco de Dados', () => {
  let service: PrismaService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [PrismaService],
    }).compile();

    service = module.get<PrismaService>(PrismaService);
  });

  it('deve ser instanciado corretamente', () => {
    expect(service).toBeDefined();
  });

  it('deve chamar $connect ao iniciar o módulo (onModuleInit)', async () => {
    const connectSpy = jest.spyOn(service, '$connect').mockResolvedValueOnce();
    await service.onModuleInit();
    expect(connectSpy).toHaveBeenCalledTimes(1);
  });

  it('deve chamar $disconnect ao destruir o módulo (onModuleDestroy)', async () => {
    const disconnectSpy = jest.spyOn(service, '$disconnect').mockResolvedValueOnce();
    await service.onModuleDestroy();
    expect(disconnectSpy).toHaveBeenCalledTimes(1);
  });

  it('deve propagar erro quando $connect falha — banco de dados indisponível', async () => {
    jest
      .spyOn(service, '$connect')
      .mockRejectedValueOnce(new Error("Can't reach database server"));

    await expect(service.onModuleInit()).rejects.toThrow(
      "Can't reach database server",
    );
  });

  it('deve propagar erro quando $disconnect falha', async () => {
    jest
      .spyOn(service, '$disconnect')
      .mockRejectedValueOnce(new Error('Disconnect failed'));

    await expect(service.onModuleDestroy()).rejects.toThrow('Disconnect failed');
  });
});

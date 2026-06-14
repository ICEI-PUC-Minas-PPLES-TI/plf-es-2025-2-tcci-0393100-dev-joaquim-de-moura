import { HttpStatus } from '@nestjs/common';
import { Prisma } from '@prisma/client';
import { PrismaExceptionFilter } from './prisma-exception.filter';

function makeResponseMock() {
  const json = jest.fn();
  const status = jest.fn().mockReturnValue({ json });
  return { status, json, _statusCalled: status };
}

function makeHost(responseMock: ReturnType<typeof makeResponseMock>) {
  return {
    switchToHttp: () => ({ getResponse: () => responseMock }),
  } as any;
}

function makePrismaError(code: string, meta?: Record<string, unknown>) {
  return new Prisma.PrismaClientKnownRequestError('test error', {
    code,
    clientVersion: '5.0.0',
    meta,
  });
}

describe('PrismaExceptionFilter — Erros de Banco de Dados', () => {
  let filter: PrismaExceptionFilter;

  beforeEach(() => {
    filter = new PrismaExceptionFilter();
  });

  describe('P2002 — violação de campo único', () => {
    it('deve retornar HTTP 409 Conflict', () => {
      const res = makeResponseMock();
      filter.catch(makePrismaError('P2002', { target: ['email'] }), makeHost(res));
      expect(res.status).toHaveBeenCalledWith(HttpStatus.CONFLICT);
    });

    it('deve mencionar o campo duplicado na mensagem', () => {
      const res = makeResponseMock();
      filter.catch(makePrismaError('P2002', { target: ['phone'] }), makeHost(res));
      const jsonArg = res.status.mock.results[0].value.json.mock.calls[0][0];
      expect(jsonArg.message).toContain('phone');
    });

    it('deve usar "campo" quando meta.target não estiver definido', () => {
      const res = makeResponseMock();
      filter.catch(makePrismaError('P2002'), makeHost(res));
      const jsonArg = res.status.mock.results[0].value.json.mock.calls[0][0];
      expect(jsonArg.message).toContain('campo');
    });
  });

  describe('P2003 — violação de chave estrangeira', () => {
    it('deve retornar HTTP 400 Bad Request', () => {
      const res = makeResponseMock();
      filter.catch(makePrismaError('P2003', { constraint: 'ride_fk' }), makeHost(res));
      expect(res.status).toHaveBeenCalledWith(HttpStatus.BAD_REQUEST);
    });

    it('deve retornar mensagem especial para constraint settledBy', () => {
      const res = makeResponseMock();
      filter.catch(makePrismaError('P2003', { constraint: 'settledBy_fk' }), makeHost(res));
      const jsonArg = res.status.mock.results[0].value.json.mock.calls[0][0];
      expect(jsonArg.message).toContain('login');
    });

    it('deve retornar mensagem genérica para outras foreign keys', () => {
      const res = makeResponseMock();
      filter.catch(makePrismaError('P2003', { constraint: 'other_fk' }), makeHost(res));
      const jsonArg = res.status.mock.results[0].value.json.mock.calls[0][0];
      expect(jsonArg.message).toContain('Referência inválida');
    });
  });

  describe('P2025 — registro não encontrado', () => {
    it('deve retornar HTTP 404 Not Found', () => {
      const res = makeResponseMock();
      filter.catch(makePrismaError('P2025'), makeHost(res));
      expect(res.status).toHaveBeenCalledWith(HttpStatus.NOT_FOUND);
    });

    it('deve retornar mensagem de "Registro não encontrado"', () => {
      const res = makeResponseMock();
      filter.catch(makePrismaError('P2025'), makeHost(res));
      const jsonArg = res.status.mock.results[0].value.json.mock.calls[0][0];
      expect(jsonArg.message).toContain('não encontrado');
    });
  });

  describe('Código desconhecido — queda inesperada do BD', () => {
    it('deve retornar HTTP 500 Internal Server Error', () => {
      const res = makeResponseMock();
      filter.catch(makePrismaError('P9999'), makeHost(res));
      expect(res.status).toHaveBeenCalledWith(HttpStatus.INTERNAL_SERVER_ERROR);
    });

    it('deve retornar mensagem genérica de erro interno', () => {
      const res = makeResponseMock();
      filter.catch(makePrismaError('P9999'), makeHost(res));
      const jsonArg = res.status.mock.results[0].value.json.mock.calls[0][0];
      expect(jsonArg.message).toContain('Erro interno');
    });
  });
});

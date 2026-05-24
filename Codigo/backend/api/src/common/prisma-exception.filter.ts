import { ArgumentsHost, Catch, ExceptionFilter, HttpStatus, Logger } from '@nestjs/common';
import { Prisma } from '@prisma/client';
import { Response } from 'express';

@Catch(Prisma.PrismaClientKnownRequestError)
export class PrismaExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(PrismaExceptionFilter.name);

  catch(exception: Prisma.PrismaClientKnownRequestError, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();

    this.logger.error(`Prisma ${exception.code}: ${exception.message}`);

    switch (exception.code) {
      case 'P2002': {
        const fields = (exception.meta?.target as string[])?.join(', ') ?? 'campo';
        response.status(HttpStatus.CONFLICT).json({
          statusCode: HttpStatus.CONFLICT,
          message: `Já existe um registro com o mesmo ${fields}.`,
          error: 'Conflict',
        });
        break;
      }
      case 'P2003': {
        const constraint = (exception.meta?.constraint as string) ?? '';
        const msg = constraint.includes('settledBy')
          ? 'Sessão inválida — faça login novamente no painel.'
          : 'Referência inválida: o registro relacionado não foi encontrado.';
        response.status(HttpStatus.BAD_REQUEST).json({
          statusCode: HttpStatus.BAD_REQUEST,
          message: msg,
          error: 'Bad Request',
        });
        break;
      }
      case 'P2025':
        response.status(HttpStatus.NOT_FOUND).json({
          statusCode: HttpStatus.NOT_FOUND,
          message: 'Registro não encontrado.',
          error: 'Not Found',
        });
        break;
      default:
        response.status(HttpStatus.INTERNAL_SERVER_ERROR).json({
          statusCode: HttpStatus.INTERNAL_SERVER_ERROR,
          message: 'Erro interno do servidor.',
          error: 'Internal Server Error',
        });
    }
  }
}

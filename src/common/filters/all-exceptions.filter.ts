import {
  ArgumentsHost,
  Catch,
  ExceptionFilter,
  HttpException,
  HttpStatus,
  Logger,
} from '@nestjs/common';
import { HttpAdapterHost } from '@nestjs/core';

@Catch()
export class AllExceptionsFilter implements ExceptionFilter {
  private readonly logger = new Logger(AllExceptionsFilter.name);

  constructor(private readonly adapterHost: HttpAdapterHost) {}

  catch(exception: unknown, host: ArgumentsHost) {
    const { httpAdapter } = this.adapterHost;
    const ctx = host.switchToHttp();
    const request = ctx.getRequest<Request>();

    const status =
      exception instanceof HttpException ? exception.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR;

    const responseBody =
      exception instanceof HttpException ? exception.getResponse() : { message: 'Internal server error' };

    const message =
      typeof responseBody === 'string'
        ? responseBody
        : (responseBody as any)?.message || 'Internal server error';

    this.logger.error(
      `Request ${request?.method || ''} ${request?.url || ''} failed`,
      exception instanceof Error ? exception.stack : JSON.stringify(responseBody)
    );

    httpAdapter.reply(
      ctx.getResponse(),
      {
        statusCode: status,
        message,
        path: request?.url,
        timestamp: new Date().toISOString(),
      },
      status
    );
  }
}

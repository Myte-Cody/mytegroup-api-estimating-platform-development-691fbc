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

    const logMessage = `Request ${request?.method || ''} ${request?.url || ''} failed (${status})`;
    const logDetail = exception instanceof Error ? exception.stack : JSON.stringify(responseBody);

    // 4xx responses are often expected (auth/validation) and shouldn't spam ERROR logs.
    if (status >= 500) {
      this.logger.error(logMessage, logDetail);
    } else {
      this.logger.warn(logMessage, logDetail);
    }

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

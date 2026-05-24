import type { NextFunction, Request, Response } from 'express';

type RateLimitOptions = {
  windowMs: number;
  max: number;
  message: string;
};

type HitCounter = {
  count: number;
  resetAt: number;
};

export function createRateLimitMiddleware(options: RateLimitOptions) {
  const hits = new Map<string, HitCounter>();

  return (req: Request, res: Response, next: NextFunction) => {
    const now = Date.now();
    const key = `${req.ip}:${req.originalUrl}`;
    const current = hits.get(key);

    if (!current || current.resetAt <= now) {
      hits.set(key, { count: 1, resetAt: now + options.windowMs });
      next();
      return;
    }

    current.count += 1;

    if (current.count > options.max) {
      const retryAfter = Math.ceil((current.resetAt - now) / 1000);
      res.setHeader('Retry-After', String(retryAfter));
      res.status(429).json({ message: options.message });
      return;
    }

    next();
  };
}

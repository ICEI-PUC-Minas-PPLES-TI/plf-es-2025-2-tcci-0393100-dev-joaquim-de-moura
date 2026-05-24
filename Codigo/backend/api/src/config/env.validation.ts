type Environment = Record<string, unknown>;

function asString(config: Environment, key: string): string | undefined {
  const value = config[key];
  return typeof value === 'string' ? value.trim() : undefined;
}

function required(config: Environment, key: string, errors: string[]) {
  const value = asString(config, key);
  if (!value) {
    errors.push(`${key} não configurado`);
  }
  return value;
}

export function validateEnv(config: Environment) {
  const errors: string[] = [];

  const databaseUrl = required(config, 'DATABASE_URL', errors);
  const jwtSecret = required(config, 'JWT_SECRET', errors);
  const googleMapsApiKey = required(config, 'GOOGLE_MAPS_API_KEY', errors);

  if (jwtSecret && jwtSecret.length < 32) {
    errors.push('JWT_SECRET deve ter pelo menos 32 caracteres');
  }

  const portRaw = asString(config, 'PORT');
  const port = portRaw ? Number(portRaw) : 3000;
  if (!Number.isInteger(port) || port <= 0 || port > 65535) {
    errors.push('PORT deve ser uma porta TCP válida');
  }

  const corsOrigin =
    asString(config, 'CORS_ORIGIN') ??
    'http://localhost:3001,http://127.0.0.1:3001';

  if (errors.length > 0) {
    throw new Error(`Configuração inválida: ${errors.join('; ')}`);
  }

  return {
    ...config,
    DATABASE_URL: databaseUrl,
    JWT_SECRET: jwtSecret,
    GOOGLE_MAPS_API_KEY: googleMapsApiKey,
    CORS_ORIGIN: corsOrigin,
    PORT: port,
  };
}

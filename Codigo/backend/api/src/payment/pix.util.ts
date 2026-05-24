type PixPayloadInput = {
  pixKey: string;
  amountCents: number;
  txId: string;
  merchantName?: string | null;
  merchantCity?: string | null;
};

function normalizePixText(value: string, maxLength: number) {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^A-Za-z0-9 .,&/-]/g, '')
    .trim()
    .toUpperCase()
    .slice(0, maxLength);
}

function emv(id: string, value: string) {
  return `${id}${String(value.length).padStart(2, '0')}${value}`;
}

function crc16Ccitt(payload: string) {
  let crc = 0xffff;

  for (let i = 0; i < payload.length; i++) {
    crc ^= payload.charCodeAt(i) << 8;
    for (let bit = 0; bit < 8; bit++) {
      crc = crc & 0x8000 ? (crc << 1) ^ 0x1021 : crc << 1;
      crc &= 0xffff;
    }
  }

  return crc.toString(16).toUpperCase().padStart(4, '0');
}

export function makePixTxId(rideId: string) {
  const compact = rideId.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
  return `MOBU${compact.slice(-21)}`.slice(0, 25);
}

export function buildPixPayload(input: PixPayloadInput) {
  const amount = (Math.max(0, input.amountCents) / 100).toFixed(2);
  const merchantName = normalizePixText(input.merchantName || 'MOBU MOTORISTA', 25) || 'MOBU';
  const merchantCity = normalizePixText(input.merchantCity || 'CONCEICAO', 15) || 'CONCEICAO';
  const txId = normalizePixText(input.txId, 25) || 'MOBU';

  const merchantAccountInfo =
    emv('00', 'br.gov.bcb.pix') +
    emv('01', input.pixKey.trim()) +
    emv('02', `MobU ${txId}`.slice(0, 72));

  const additionalData = emv('05', txId);
  const withoutCrc =
    emv('00', '01') +
    emv('26', merchantAccountInfo) +
    emv('52', '0000') +
    emv('53', '986') +
    emv('54', amount) +
    emv('58', 'BR') +
    emv('59', merchantName) +
    emv('60', merchantCity) +
    emv('62', additionalData) +
    '6304';

  return `${withoutCrc}${crc16Ccitt(withoutCrc)}`;
}


import { buildPixPayload, makePixTxId } from './pix.util';

describe('pix.util', () => {
  describe('makePixTxId', () => {
    it('deve gerar um txId com prefixo MOBU', () => {
      const txId = makePixTxId('abc123');
      expect(txId).toMatch(/^MOBU/);
    });

    it('deve gerar txId com no máximo 25 caracteres', () => {
      const txId = makePixTxId('um-id-muito-longo-para-testar-o-truncamento-correto');
      expect(txId.length).toBeLessThanOrEqual(25);
    });

    it('deve remover hífens do rideId', () => {
      const txId = makePixTxId('aaaa-bbbb-cccc');
      expect(txId).not.toContain('-');
    });
  });

  describe('buildPixPayload (TA7)', () => {
    const baseInput = {
      pixKey: 'motorista@email.com',
      amountCents: 1500,
      txId: 'MOBUTESTE001',
      merchantName: 'Carlos Motorista',
      merchantCity: 'Pará de Minas',
    };

    it('deve retornar uma string não vazia', () => {
      const payload = buildPixPayload(baseInput);
      expect(typeof payload).toBe('string');
      expect(payload.length).toBeGreaterThan(0);
    });

    it('deve conter a chave PIX do motorista no payload', () => {
      const payload = buildPixPayload(baseInput);
      expect(payload).toContain('motorista@email.com');
    });

    it('deve conter o valor formatado corretamente (15.00)', () => {
      const payload = buildPixPayload(baseInput);
      expect(payload).toContain('15.00');
    });

    it('deve terminar com CRC de 4 caracteres hexadecimais', () => {
      const payload = buildPixPayload(baseInput);
      expect(payload).toMatch(/[0-9A-F]{4}$/);
    });

    it('deve conter o identificador EMV do PIX brasileiro', () => {
      const payload = buildPixPayload(baseInput);
      expect(payload).toContain('br.gov.bcb.pix');
    });

    it('deve usar valor mínimo zero para amountCents negativo', () => {
      const payload = buildPixPayload({ ...baseInput, amountCents: -100 });
      expect(payload).toContain('0.00');
    });

    it('deve funcionar sem merchantName e merchantCity', () => {
      const payload = buildPixPayload({
        pixKey: 'chave@pix.com',
        amountCents: 800,
        txId: 'MOBU001',
      });
      expect(payload).toContain('br.gov.bcb.pix');
      expect(payload).toContain('8.00');
    });

    it('deve normalizar acentos no merchantCity', () => {
      const payload = buildPixPayload({ ...baseInput, merchantCity: 'São Paulo' });
      expect(payload).not.toMatch(/[áéíóúãõâêîôûç]/i);
    });
  });
});

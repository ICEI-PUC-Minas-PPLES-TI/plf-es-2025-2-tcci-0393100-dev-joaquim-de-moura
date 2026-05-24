/** Mascaramento para API (contato direto deve usar fluxo in-app / central no futuro). */
export function maskBrazilPhone(phone: string | null | undefined): string | null {
  if (phone == null || phone === '') return phone ?? null;
  const digits = phone.replace(/\D/g, '');
  if (digits.length < 4) return '****';
  const last4 = digits.slice(-4);
  return `(**) *****-${last4}`;
}

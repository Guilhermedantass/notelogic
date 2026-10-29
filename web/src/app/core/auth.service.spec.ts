import { expirou } from './auth.service';

/** Monta um JWT de mentira só com o conteúdo (a assinatura não importa para ler o exp). */
function token(conteudo: object): string {
  const base64 = btoa(JSON.stringify(conteudo)).replace(/=+$/, '');
  return `cabecalho.${base64}.assinatura`;
}

describe('expirou', () => {
  const agora = Date.parse('2026-11-02T12:00:00Z');

  it('lê a expiração do token', () => {
    expect(expirou(token({ exp: agora / 1000 + 3600 }), agora)).toBe(false);
    expect(expirou(token({ exp: agora / 1000 - 1 }), agora)).toBe(true);
  });

  it('trata lixo como expirado', () => {
    expect(expirou('nao-e-um-jwt', agora)).toBe(true);
  });
});

// Conversa com a App Store Connect: o token ES256. Sem nada do banco aqui.
// Espelha supabase/functions/apple-envio/apple.ts do PDV (drrodrigo/catologoonline).

// O painel do Supabase guarda o segredo numa linha só: o .p8 colado pode chegar com as quebras
// de linha trocadas por espaço. Remonta o PEM a partir do miolo em base64, venha como vier.
export function miolhoDoP8(p8: string): string {
  return p8
    .replace(/-----BEGIN PRIVATE KEY-----/, '')
    .replace(/-----END PRIVATE KEY-----/, '')
    .replace(/\\n/g, '')
    .replace(/\s+/g, '')
}

function b64url(bytes: Uint8Array | string): string {
  const b = typeof bytes === 'string' ? new TextEncoder().encode(bytes) : bytes
  let s = ''
  for (const x of b) s += String.fromCharCode(x)
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

export async function tokenDaApple(p8: string, keyId: string, issuer: string): Promise<string> {
  const der = Uint8Array.from(atob(miolhoDoP8(p8)), (c) => c.charCodeAt(0))
  const chave = await crypto.subtle.importKey('pkcs8', der, { name: 'ECDSA', namedCurve: 'P-256' }, false, ['sign'])
  const agora = Math.floor(Date.now() / 1000)
  const cab = b64url(JSON.stringify({ alg: 'ES256', kid: keyId.trim(), typ: 'JWT' }))
  const corpo = b64url(JSON.stringify({ iss: issuer.trim(), iat: agora, exp: agora + 1100, aud: 'appstoreconnect-v1' }))
  const assinatura = await crypto.subtle.sign({ name: 'ECDSA', hash: 'SHA-256' }, chave, new TextEncoder().encode(`${cab}.${corpo}`))
  return `${cab}.${corpo}.${b64url(new Uint8Array(assinatura))}`
}

export const API_APPLE = 'https://api.appstoreconnect.apple.com'

// guardioes-alertar — quando a pessoa aciona o SOS, o app chama aqui com o aparelho dela; buscamos
// os GUARDIÕES que aceitaram protegê-la (RPC guardioes_tokens_dos_guardioes) e disparamos a SIRENE
// (push tipo 'alerta', DATA message) em cada aparelho. Autentica com a chave do app (x-smsos-app).
//
// Publicar com verify_jwt: false (o app não tem login). Até a v4 este arquivo só existia no servidor; desde
// 24/11 mora aqui. A v5 respeita o guardião PAUSADO: o app (0.36.0+) manda `pausados` (os números, 11 dígitos,
// dos guardiões com a chave "Recebe os alertas" desligada) e a RPC de 2 argumentos deixa esses de fora
// (supabase/sql/2026-11-24-guardioes-tokens-sem-pausados.sql). App antigo, sem o campo: toca em todos, como antes.

import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const SUPABASE_URL = Deno.env.get('SUPABASE_URL') ?? ''
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
// Chaves aceitas (só o hash): Android e iPhone (br.com.guardioessos). 20/11: o iPhone entrou (OK do Rodrigo).
const APP_KEY_HASHES = [
  '2307d2a6e56b1f1003660cfe233737bf01d1027a8130481c19b2417e7ebb7ec1', // Android
  'e3004feb3be97c3f3e92cdc0b93096198efb9fb92156696a64b76173925e5b32', // iPhone
]
const SA_RAW = Deno.env.get('FCM_SERVICE_ACCOUNT') ?? ''

const cors = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'content-type, x-smsos-app',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
}
function json(corpo: unknown, status = 200) {
  return new Response(JSON.stringify(corpo), { status, headers: { ...cors, 'Content-Type': 'application/json' } })
}
async function sha256hex(s: string): Promise<string> {
  const b = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(s))
  return [...new Uint8Array(b)].map((x) => x.toString(16).padStart(2, '0')).join('')
}
function b64url(bytes: Uint8Array): string {
  let s = ''
  for (const b of bytes) s += String.fromCharCode(b)
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}
async function importarChave(pem: string): Promise<CryptoKey> {
  const b64 = pem.replace(/-----BEGIN PRIVATE KEY-----/, '').replace(/-----END PRIVATE KEY-----/, '').replace(/\s+/g, '')
  const der = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0))
  return crypto.subtle.importKey('pkcs8', der.buffer, { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }, false, ['sign'])
}
async function accessToken(sa: Record<string, string>): Promise<string> {
  const agora = Math.floor(Date.now() / 1000)
  const header = b64url(new TextEncoder().encode(JSON.stringify({ alg: 'RS256', typ: 'JWT' })))
  const claims = b64url(new TextEncoder().encode(JSON.stringify({
    iss: sa.client_email,
    scope: 'https://www.googleapis.com/auth/firebase.messaging',
    aud: sa.token_uri,
    iat: agora,
    exp: agora + 3600,
  })))
  const base = `${header}.${claims}`
  const chave = await importarChave(sa.private_key)
  const assinatura = new Uint8Array(await crypto.subtle.sign('RSASSA-PKCS1-v1_5', chave, new TextEncoder().encode(base)))
  const jwt = `${base}.${b64url(assinatura)}`
  const r = await fetch(sa.token_uri, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  })
  const corpo = await r.json().catch(() => ({}))
  if (!corpo.access_token) throw new Error('token_oauth_falhou: ' + JSON.stringify(corpo))
  return corpo.access_token
}
async function enviarUm(projectId: string, bearer: string, token: string, titulo: string, corpo: string) {
  const r = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${bearer}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      message: {
        token,
        data: { tipo: 'alerta', titulo, corpo },
        android: { priority: 'HIGH' },
      },
    }),
  })
  const resposta = await r.json().catch(() => ({}))
  return { ok: r.ok, status: r.status, resposta }
}

/** Os números pausados que o app mandou: só dígitos, os 11 finais, no máximo 50 (o resto é lixo e cai fora). */
export function lerPausados(valor: unknown): string[] {
  if (!Array.isArray(valor)) return []
  const numeros = valor
    .map((v) => String(v ?? '').replace(/\D/g, '').slice(-11))
    .filter((n) => n.length >= 10)
  return [...new Set(numeros)].slice(0, 50)
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: cors })
  if (req.method !== 'POST') return json({ erro: 'metodo_nao_permitido' }, 405)

  const chave = req.headers.get('x-smsos-app') ?? ''
  if (!chave || !APP_KEY_HASHES.includes(await sha256hex(chave))) return json({ erro: 'nao_autorizado' }, 401)

  const c = await req.json().catch(() => ({} as Record<string, unknown>))
  const dispositivo = String(c?.dispositivo ?? '')
  if (!dispositivo) return json({ erro: 'sem_dispositivo' }, 400)
  const titulo = String(c?.titulo ?? '⚠️ Alerta de SOS')
  const corpo = String(c?.corpo ?? 'Alguém que você protege acionou o SOS.')
  const pausados = lerPausados(c?.pausados)

  const supabase = createClient(SUPABASE_URL, SERVICE_ROLE)
  const { data, error } = await supabase.rpc('guardioes_tokens_dos_guardioes', {
    p_usuario_dispositivo: dispositivo,
    p_pausados: pausados,
  })
  if (error) return json({ erro: 'busca_guardioes', mensagem: error.message }, 500)
  const tokens = [...new Set(((data ?? []) as Array<Record<string, unknown>>).map((r) => String(r.token)).filter((t) => t && t.length > 20))]
  if (tokens.length === 0) return json({ ok: true, enviados: 0, total: 0, pausados: pausados.length, nota: 'nenhum guardião com app' })

  if (!SA_RAW) return json({ erro: 'sem_service_account' }, 500)
  let sa: Record<string, string>
  try { sa = JSON.parse(SA_RAW) } catch { return json({ erro: 'service_account_invalido' }, 500) }
  let bearer: string
  try { bearer = await accessToken(sa) } catch (e) { return json({ erro: 'oauth', mensagem: String(e) }, 502) }

  const resultados: unknown[] = []
  for (const t of tokens) {
    try { resultados.push({ token: t.slice(0, 12) + '…', ...(await enviarUm(sa.project_id, bearer, t, titulo, corpo)) }) }
    catch (e) { resultados.push({ token: t.slice(0, 12) + '…', ok: false, erro: String(e) }) }
  }
  const enviados = resultados.filter((r) => (r as Record<string, unknown>).ok).length
  return json({ ok: enviados > 0, enviados, total: tokens.length, pausados: pausados.length, resultados })
})

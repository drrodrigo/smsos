// Regras puras da porta guardioes-web (sem Deno, sem rede): dá para testar no Node.
//
// A página guardioessos.com.br/app NÃO carrega chave nenhuma. Quem prova quem é a pessoa é o
// número dela, confirmado por código de SMS; depois disso o servidor entrega uma "chave do
// aparelho" ASSINADA (HMAC) com o telefone dentro. Ninguém consegue fabricar essa chave sem o
// segredo do servidor, e o texto do SOS é sempre montado AQUI (nunca vem da página): quem
// tentar usar a porta para mandar outra coisa só consegue mandar o alerta com o próprio nome.

export type Perfil = 'seguranca' | 'saude'

// Os mesmos textos do app Android (core/MessageBuilder.kt).
export const TEXTO_SEGURANCA =
  'ALERTA! {nome} acionou o botao de emergencia e pode estar em perigo. Local: {mapa} - {hora}'
export const TEXTO_SAUDE =
  'ALERTA DE SAUDE! {nome} apertou o botao e pode estar passando mal. Local: {mapa} - {hora}'
export const TEXTO_TUDO_BEM =
  '{nome} avisou que esta tudo bem. Pode desconsiderar o alerta de emergencia. - {hora}'

export const MAX_CONTATOS = 10

/** Celular brasileiro com 11 dígitos (DDD + 9 + número), como o app guarda. Senão, null. */
export function normalizarTelefone(bruto: unknown): string | null {
  let d = String(bruto ?? '').replace(/\D/g, '')
  if ((d.length === 12 || d.length === 13) && d.startsWith('55')) d = d.slice(2)
  if (d.startsWith('0')) d = d.replace(/^0+/, '')
  // Faltou o 9 do celular: DDD + 8 dígitos começando por 6, 7, 8 ou 9.
  if (d.length === 10 && /[6-9]/.test(d[2])) d = d.slice(0, 2) + '9' + d.slice(2)
  if (d.length !== 11 || d[2] !== '9') return null
  if (d[0] === '0' || d[1] === '0') return null
  return d
}

/** O nome que vai no SMS: só letras, espaço, apóstrofo e hífen (sem ponto: nada de link), até 30 letras. */
export function limparNome(bruto: unknown): string {
  const s = String(bruto ?? '')
    .replace(/[^\p{L}\p{M} '-]/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 30)
    .trim()
  return s || 'Um contato seu'
}

export function semAcento(s: string): string {
  return s.normalize('NFD').replace(/\p{M}/gu, '')
}

/** "dd/MM HH:mm" no horário de Brasília. */
export function horaDeBrasilia(agora: Date): string {
  const partes = new Intl.DateTimeFormat('pt-BR', {
    timeZone: 'America/Sao_Paulo', day: '2-digit', month: '2-digit',
    hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
  }).formatToParts(agora)
  const p = (t: string) => partes.find((x) => x.type === t)?.value ?? '00'
  return `${p('day')}/${p('month')} ${p('hour')}:${p('minute')}`
}

export function linkDoMapa(lat: unknown, lng: unknown): string | null {
  const a = Number(lat)
  const o = Number(lng)
  if (lat === null || lng === null || lat === undefined || lng === undefined) return null
  if (!Number.isFinite(a) || !Number.isFinite(o)) return null
  if (Math.abs(a) > 90 || Math.abs(o) > 180) return null
  return `https://maps.google.com/?q=${a.toFixed(6)},${o.toFixed(6)}`
}

export function montarAlerta(
  perfil: unknown, nome: unknown, lat: unknown, lng: unknown, agora: Date,
): string {
  const modelo = perfil === 'saude' ? TEXTO_SAUDE : TEXTO_SEGURANCA
  return semAcento(
    modelo
      .replace('{nome}', limparNome(nome))
      .replace('{mapa}', linkDoMapa(lat, lng) ?? 'localizacao indisponivel')
      .replace('{hora}', horaDeBrasilia(agora)),
  )
}

export function montarTudoBem(nome: unknown, agora: Date): string {
  return semAcento(
    TEXTO_TUDO_BEM.replace('{nome}', limparNome(nome)).replace('{hora}', horaDeBrasilia(agora)),
  )
}

/** Os contatos válidos, sem repetir, no máximo 10. Devolve também os que foram recusados. */
export function separarContatos(lista: unknown): { validos: string[]; recusados: string[] } {
  const validos: string[] = []
  const recusados: string[] = []
  for (const item of Array.isArray(lista) ? lista : []) {
    const tel = normalizarTelefone(item)
    if (!tel) recusados.push(String(item ?? ''))
    else if (!validos.includes(tel)) validos.push(tel)
  }
  return { validos: validos.slice(0, MAX_CONTATOS), recusados }
}

// ---- A chave do aparelho (HMAC-SHA256) ----

function b64url(bytes: Uint8Array): string {
  let s = ''
  for (const b of bytes) s += String.fromCharCode(b)
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function deB64url(s: string): Uint8Array {
  const base = s.replace(/-/g, '+').replace(/_/g, '/')
  const cru = atob(base.padEnd(base.length + ((4 - (base.length % 4)) % 4), '='))
  return Uint8Array.from(cru, (c) => c.charCodeAt(0))
}

async function hmac(segredo: string, texto: string): Promise<Uint8Array> {
  const chave = await crypto.subtle.importKey(
    'raw', new TextEncoder().encode(segredo), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign'],
  )
  return new Uint8Array(await crypto.subtle.sign('HMAC', chave, new TextEncoder().encode('gw1.' + texto)))
}

function iguais(a: Uint8Array, b: Uint8Array): boolean {
  if (a.length !== b.length) return false
  let dif = 0
  for (let i = 0; i < a.length; i++) dif |= a[i] ^ b[i]
  return dif === 0
}

export async function assinarChave(segredo: string, telefone: string, agora: Date): Promise<string> {
  const carga = b64url(new TextEncoder().encode(JSON.stringify({ t: telefone, i: Math.floor(agora.getTime() / 1000) })))
  return `${carga}.${b64url(await hmac(segredo, carga))}`
}

/** O telefone dentro da chave, se a assinatura confere. Senão, null. */
export async function lerChave(segredo: string, chave: unknown): Promise<string | null> {
  if (!segredo || typeof chave !== 'string' || chave.length > 400) return null
  const [carga, assinatura, sobra] = chave.split('.')
  if (!carga || !assinatura || sobra !== undefined) return null
  try {
    if (!iguais(deB64url(assinatura), await hmac(segredo, carga))) return null
    const dados = JSON.parse(new TextDecoder().decode(deB64url(carga)))
    return normalizarTelefone(dados?.t)
  } catch {
    return null
  }
}

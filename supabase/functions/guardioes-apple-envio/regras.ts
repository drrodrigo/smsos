// A PORTA DA CHAVE DA APPLE PARA O GITHUB — GuardiõesSOS (iPhone).
//
// Espelha a porta do PDV Único (drrodrigo/catologoonline, supabase/functions/apple-envio), mas
// atende SÓ o GuardiõesSOS: outro repositório, outra rotina, outro pacote. Assim os dois produtos
// ficam separados e mexer num não quebra o outro.
//
// Quem pede a chave prova quem é com o token de identidade do GitHub (OIDC): um documento
// assinado pelo GitHub dizendo o repositório, a rotina e o ramo da execução. Aqui moram as
// conferências (sem nada de Deno, para dar pra testar fora).

export const EMISSOR_GITHUB = 'https://token.actions.githubusercontent.com'
export const CHAVES_DO_GITHUB = `${EMISSOR_GITHUB}/.well-known/jwks`
/** O "público" que a rotina pede ao GitHub: um token feito para outra coisa não serve aqui. */
export const PUBLICO = 'guardioes-apple'
export const REPOSITORIO = 'drrodrigo/smsos'
export const ROTINA = `${REPOSITORIO}/.github/workflows/guardioes-iphone.yml@`
/** O único app que esta porta atende: o mesmo pacote da Play. */
export const PACOTE = 'br.com.guardioessos'

export interface Jwk {
  kty: string
  kid?: string
  n?: string
  e?: string
}

export type Documento = Record<string, unknown>

export type Veredito = { ok: true; documento: Documento } | { ok: false; motivo: string }

export function deBase64Url(s: string): Uint8Array {
  const b = s.replace(/-/g, '+').replace(/_/g, '/')
  const resto = b.length % 4
  return Uint8Array.from(atob(resto ? b + '='.repeat(4 - resto) : b), (c) => c.charCodeAt(0))
}

/** O ramo principal, os ramos de trabalho (`feat/...`) e os do Claude (`claude/...`). Etiqueta e pull request, nunca. */
export function ramoPermitido(ref: string): boolean {
  return (
    ref === 'refs/heads/main' ||
    /^refs\/heads\/feat\/[A-Za-z0-9._/-]+$/.test(ref) ||
    /^refs\/heads\/claude\/[A-Za-z0-9._-]+$/.test(ref)
  )
}

/** O que o documento diz, conferido na ordem da spec. Devolve o primeiro motivo de recusa. */
export function conferirDocumento(d: Documento, agora: number): string | null {
  if (d.iss !== EMISSOR_GITHUB) return 'emissor'
  const publico = Array.isArray(d.aud) ? d.aud : [d.aud]
  if (!publico.includes(PUBLICO)) return 'publico'
  if (typeof d.exp !== 'number' || d.exp <= agora) return 'vencido'
  if (typeof d.nbf === 'number' && d.nbf > agora + 60) return 'ainda_nao_vale'
  if (d.repository !== REPOSITORIO) return 'repositorio'
  if (typeof d.workflow_ref !== 'string' || !d.workflow_ref.startsWith(ROTINA)) return 'rotina'
  if (typeof d.ref !== 'string' || !ramoPermitido(d.ref)) return 'ramo'
  // O ramo da rotina e o da execução são o mesmo: senão um ramo permitido roda a rotina de outro.
  if (d.workflow_ref !== `${ROTINA}${d.ref}`) return 'rotina'
  if (d.event_name !== 'push' && d.event_name !== 'workflow_dispatch') return 'evento'
  return null
}

/** A assinatura (RS256, com as chaves públicas do GitHub) e depois o documento. */
export async function verificarToken(token: string, chaves: Jwk[], agora: number): Promise<Veredito> {
  const partes = token.split('.')
  if (partes.length !== 3) return { ok: false, motivo: 'formato' }
  let cabecalho: { alg?: string; kid?: string }
  let documento: Documento
  try {
    cabecalho = JSON.parse(new TextDecoder().decode(deBase64Url(partes[0])))
    documento = JSON.parse(new TextDecoder().decode(deBase64Url(partes[1])))
  } catch {
    return { ok: false, motivo: 'formato' }
  }
  if (cabecalho.alg !== 'RS256') return { ok: false, motivo: 'algoritmo' }
  const jwk = chaves.find((k) => k.kty === 'RSA' && k.kid === cabecalho.kid && k.n && k.e)
  if (!jwk) return { ok: false, motivo: 'chave_desconhecida' }
  let confere = false
  try {
    const chave = await crypto.subtle.importKey(
      'jwk',
      { kty: 'RSA', n: jwk.n, e: jwk.e, ext: true },
      { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
      false,
      ['verify'],
    )
    confere = await crypto.subtle.verify(
      'RSASSA-PKCS1-v1_5',
      chave,
      deBase64Url(partes[2]),
      new TextEncoder().encode(`${partes[0]}.${partes[1]}`),
    )
  } catch {
    confere = false
  }
  if (!confere) return { ok: false, motivo: 'assinatura' }
  const motivo = conferirDocumento(documento, agora)
  return motivo ? { ok: false, motivo } : { ok: true, documento }
}

/**
 * O segredo do Supabase guarda o .p8 numa linha só (as quebras podem chegar trocadas por espaço).
 * O Xcode quer o arquivo como a Apple entregou: remonta o PEM a partir do miolo.
 */
export function p8ComoArquivo(p8: string): string {
  const miolo = p8
    .replace(/-----BEGIN PRIVATE KEY-----/, '')
    .replace(/-----END PRIVATE KEY-----/, '')
    .replace(/\\n/g, '')
    .replace(/\s+/g, '')
  const linhas = miolo.match(/.{1,64}/g) ?? []
  return `-----BEGIN PRIVATE KEY-----\n${linhas.join('\n')}\n-----END PRIVATE KEY-----\n`
}

import { API_APPLE, tokenDaApple } from './apple.ts'
import { CHAVES_DO_GITHUB, PACOTE, p8ComoArquivo, verificarToken, type Jwk } from './regras.ts'

// A PORTA DA CHAVE DA APPLE PARA A ROTINA DO IPHONE — GuardiõesSOS.
//
// Espelha a porta apple-envio do PDV Único, mas atende só o GuardiõesSOS (ver regras.ts).
// Quem chama é o GitHub (.github/workflows/guardioes-iphone.yml), com o token de identidade dele
// no cabeçalho `x-github-oidc`. Por isso a função vai ao ar com verify_jwt: false: não há login do
// Supabase no meio. A conferência é a de regras.ts (assinatura do GitHub, repositório, rotina,
// ramo, evento). A chave da Apple (APPLE_P8 / APPLE_KEY_ID / APPLE_ISSUER_ID, a MESMA do PDV, no
// mesmo projeto Supabase) nunca vai para o log.
//
//   {"acao":"preparar"} -> registra o pacote br.com.guardioessos se faltar, diz o código do time
//                          e se o app já existe na App Store Connect. Não devolve segredo.
//   {"acao":"chave"}    -> a chave, para o Xcode assinar e enviar.

const json = (d: unknown, status = 200) =>
  new Response(JSON.stringify(d), { status, headers: { 'Content-Type': 'application/json' } })

let guardadas: { em: number; chaves: Jwk[] } | null = null

async function chavesDoGitHub(forcar = false): Promise<Jwk[]> {
  if (!forcar && guardadas && Date.now() - guardadas.em < 10 * 60_000) return guardadas.chaves
  const r = await fetch(CHAVES_DO_GITHUB)
  if (!r.ok) throw new Error(`chaves do GitHub: ${r.status}`)
  const chaves = ((await r.json()).keys ?? []) as Jwk[]
  guardadas = { em: Date.now(), chaves }
  return chaves
}

type Recurso = { id: string; attributes?: Record<string, unknown> }

async function preparar(token: string) {
  const H = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
  const falha = async (passo: string, r: Response) =>
    json({ erro: 'apple', passo, status: r.status, texto: (await r.text()).slice(0, 400) }, 502)

  // Os pacotes da conta: o do GuardiõesSOS (registra se faltar) e o código do time, que é o prefixo
  // (seedId) dos pacotes. Todos os pacotes da conta devem dar o mesmo.
  let r = await fetch(`${API_APPLE}/v1/bundleIds?limit=200&fields[bundleIds]=identifier,seedId,platform`, { headers: H })
  if (!r.ok) return falha('bundleIds', r)
  const pacotes = ((await r.json()).data ?? []) as Recurso[]
  let nosso = pacotes.find((p) => p.attributes?.identifier === PACOTE)
  let criado = false
  if (!nosso) {
    r = await fetch(`${API_APPLE}/v1/bundleIds`, {
      method: 'POST',
      headers: H,
      body: JSON.stringify({ data: { type: 'bundleIds', attributes: { identifier: PACOTE, name: 'GuardioesSOS', platform: 'IOS' } } }),
    })
    if (!r.ok) return falha('criar bundleId', r)
    nosso = (await r.json()).data as Recurso
    criado = true
  }
  const times = [...new Set([...pacotes, nosso].map((p) => String(p.attributes?.seedId ?? '')).filter(Boolean))]

  r = await fetch(`${API_APPLE}/v1/apps?filter[bundleId]=${encodeURIComponent(PACOTE)}&fields[apps]=bundleId,name`, { headers: H })
  if (!r.ok) return falha('apps', r)
  const app = (((await r.json()).data ?? []) as Recurso[]).find((a) => a.attributes?.bundleId === PACOTE)

  return json({
    pacote: PACOTE,
    pacote_criado: criado,
    time: String(nosso.attributes?.seedId ?? ''),
    times_da_conta: times,
    app_existe: !!app,
    app_id: app?.id ?? null,
    app_nome: app?.attributes?.name ?? null,
  })
}

Deno.serve(async (req) => {
  if (req.method !== 'POST') return json({ erro: 'metodo' }, 405)
  const token = req.headers.get('x-github-oidc') ?? ''
  if (!token) return json({ erro: 'sem_token' }, 401)

  const agora = Math.floor(Date.now() / 1000)
  let veredito
  try {
    veredito = await verificarToken(token, await chavesDoGitHub(), agora)
    // O GitHub troca as chaves de vez em quando: uma desconhecida pede a lista de novo, uma vez.
    if (!veredito.ok && veredito.motivo === 'chave_desconhecida') {
      veredito = await verificarToken(token, await chavesDoGitHub(true), agora)
    }
  } catch (e) {
    return json({ erro: 'github_fora', detalhe: e instanceof Error ? e.message : String(e) }, 503)
  }
  if (!veredito.ok) {
    console.log('guardioes-apple-envio: recusado', veredito.motivo)
    return json({ erro: 'recusado', motivo: veredito.motivo }, 403)
  }

  const p8 = Deno.env.get('APPLE_P8') ?? ''
  const keyId = (Deno.env.get('APPLE_KEY_ID') ?? '').trim()
  const issuer = (Deno.env.get('APPLE_ISSUER_ID') ?? '').trim()
  if (!p8.trim() || !keyId || !issuer) return json({ erro: 'sem_chave_no_supabase' }, 500)

  const corpo = (await req.json().catch(() => ({}))) as { acao?: string }
  const quem = `${veredito.documento.workflow_ref} execução ${veredito.documento.run_id}`

  if (corpo.acao === 'preparar') {
    console.log('guardioes-apple-envio: preparar para', quem)
    return preparar(await tokenDaApple(p8, keyId, issuer))
  }
  if (corpo.acao === 'chave') {
    console.log('guardioes-apple-envio: chave entregue a', quem)
    return json({ p8: p8ComoArquivo(p8), key_id: keyId, issuer })
  }
  return json({ erro: 'acao' }, 400)
})

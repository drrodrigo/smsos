// guardioes-convite — convites de guardião (criar / ver / listar por telefone / aceitar / quem protejo /
// situação dos meus guardiões).
// O app chama com a chave do app (x-smsos-app). Grava via RPCs public.guardioes_* (service role).
//
// Publicar com verify_jwt: false (o app não tem login). Até a v9 este arquivo só existia no servidor; desde
// 21/11 mora aqui. A v10 acrescenta a ação "situacao" (só leitura): a tela de Guardiões mostra quem é
// "Guardião SMS" e quem é "Guardião com app" (o celular toca). Precisa da função
// public.guardioes_situacao_dos_guardioes (supabase/sql/2026-11-21-guardioes-situacao-dos-guardioes.sql).

import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const SUPABASE_URL = Deno.env.get('SUPABASE_URL') ?? ''
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
// Chaves aceitas: Android e iPhone (br.com.guardioessos).
const APP_KEY_HASHES = [
  '2307d2a6e56b1f1003660cfe233737bf01d1027a8130481c19b2417e7ebb7ec1', // Android
  'e3004feb3be97c3f3e92cdc0b93096198efb9fb92156696a64b76173925e5b32', // iPhone
]
const SITE = 'https://www.guardioessos.com.br'

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

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: cors })
  if (req.method !== 'POST') return json({ erro: 'metodo_nao_permitido' }, 405)

  const chave = req.headers.get('x-smsos-app') ?? ''
  if (!chave || !APP_KEY_HASHES.includes(await sha256hex(chave))) return json({ erro: 'nao_autorizado' }, 401)

  const c = await req.json().catch(() => ({} as Record<string, unknown>))
  const acao = (c?.acao as string) ?? 'convidar'
  const supabase = createClient(SUPABASE_URL, SERVICE_ROLE)
  const str = (k: string) => (c?.[k] == null ? null : String(c[k]))

  async function rpc(fn: string, args: Record<string, unknown>) {
    const { data, error } = await supabase.rpc(fn, args)
    if (error) return json({ erro: 'falhou', mensagem: error.message }, 400)
    return json(data ?? {})
  }

  if (acao === 'convidar') {
    if (!c?.usuario_dispositivo || !c?.guardiao_telefone) return json({ erro: 'dados_obrigatorios' }, 400)
    const { data, error } = await supabase.rpc('guardioes_convidar', {
      p_usuario_dispositivo: String(c.usuario_dispositivo),
      p_usuario_nome: str('usuario_nome'),
      p_guardiao_telefone: String(c.guardiao_telefone),
    })
    if (error) return json({ erro: 'falhou', mensagem: error.message }, 400)
    const codigo = (data as Record<string, unknown>)?.codigo as string
    return json({ ...(data as Record<string, unknown>), link: `${SITE}/convite?c=${codigo}` })
  }
  if (acao === 'por_codigo') {
    if (!c?.codigo) return json({ erro: 'dados_obrigatorios' }, 400)
    return rpc('guardioes_convite_por_codigo', { p_codigo: String(c.codigo) })
  }
  if (acao === 'do_telefone') {
    if (!c?.telefone) return json({ erro: 'dados_obrigatorios' }, 400)
    return rpc('guardioes_convites_do_telefone', { p_telefone: String(c.telefone) })
  }
  if (acao === 'aceitar') {
    if (!c?.codigo || !c?.guardiao_dispositivo) return json({ erro: 'dados_obrigatorios' }, 400)
    return rpc('guardioes_aceitar', { p_codigo: String(c.codigo), p_guardiao_dispositivo: String(c.guardiao_dispositivo) })
  }
  if (acao === 'quem_protejo') {
    if (!c?.guardiao_dispositivo) return json({ erro: 'dados_obrigatorios' }, 400)
    return rpc('guardioes_quem_protejo', { p_guardiao_dispositivo: String(c.guardiao_dispositivo) })
  }
  // Só leitura: em que pé está cada guardião que ESTE aparelho convidou. Devolve só telefone (o que o próprio
  // aparelho mandou), status do convite e se o app do guardião recebe aviso — nada do guardião além disso.
  if (acao === 'situacao') {
    if (!c?.usuario_dispositivo) return json({ erro: 'dados_obrigatorios' }, 400)
    const { data, error } = await supabase.rpc('guardioes_situacao_dos_guardioes', {
      p_usuario_dispositivo: String(c.usuario_dispositivo),
    })
    if (error) return json({ erro: 'falhou', mensagem: error.message }, 400)
    return json({ guardioes: data ?? [] })
  }

  return json({ erro: 'acao_desconhecida' }, 400)
})

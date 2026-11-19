// guardioes-web — a porta da página guardioessos.com.br/app (o GuardiõesSOS no navegador).
//
// Publicar com verify_jwt: false (a autenticação é a chave do aparelho, ver regras.ts).
//
// Ações (POST JSON):
//   { acao: 'estado' }                                   -> { ok: true }
//   { acao: 'codigo', telefone }                         -> manda o código de 6 dígitos por SMS
//   { acao: 'entrar', telefone, codigo }                 -> { ok, chave } (a chave do aparelho)
//   { acao: 'sos', chave, perfil, nome, lat, lng, contatos: [tel...] }
//   { acao: 'tudo_bem', chave, nome, contatos: [tel...] }
//
// O SMS sai pela bipe-sms (a mesma do app), mas em PROJETOS PRÓPRIOS, com teto próprio em
// sms.limites: 'guardioes-web-otp' (códigos) e 'guardioes-web' (alertas). Assim, um abuso pela
// página nunca gasta a cota dos alertas do aplicativo ('smsos').

import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import {
  assinarChave, lerChave, montarAlerta, montarTudoBem, normalizarTelefone, separarContatos,
} from './regras.ts'

const SUPABASE_URL = Deno.env.get('SUPABASE_URL') ?? ''
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
// Segredo da assinatura: um próprio, se existir; senão a service role (só existe no servidor).
const SEGREDO = Deno.env.get('GUARDIOES_WEB_SEGREDO') || SERVICE_ROLE
const BIPE_SMS_URL = `${SUPABASE_URL}/functions/v1/bipe-sms`
const PROJETO_CODIGO = 'guardioes-web-otp'
const PROJETO_ALERTA = 'guardioes-web'

const cors = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'content-type',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
}
function json(corpo: unknown, status = 200) {
  return new Response(JSON.stringify(corpo), { status, headers: { ...cors, 'Content-Type': 'application/json' } })
}

async function mandarSms(projeto: string, finalidade: string, telefone11: string, texto: string) {
  try {
    const r = await fetch(BIPE_SMS_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${SERVICE_ROLE}` },
      body: JSON.stringify({ projeto, finalidade, telefone: '55' + telefone11, texto }),
    })
    const corpo = await r.json().catch(() => ({})) as Record<string, unknown>
    const ok = r.ok && corpo?.ok === true
    const msg = String(corpo?.mensagem ?? '')
    const erro = ok ? undefined : (msg.includes('limite') ? 'limite' : 'falhou')
    return { telefone: telefone11, ok, erro }
  } catch {
    return { telefone: telefone11, ok: false, erro: 'falhou' }
  }
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: cors })
  if (req.method !== 'POST') return json({ erro: 'metodo_nao_permitido' }, 405)

  const c = await req.json().catch(() => ({})) as Record<string, unknown>
  const acao = String(c?.acao ?? '')
  const supabase = createClient(SUPABASE_URL, SERVICE_ROLE)

  if (acao === 'estado') return json({ ok: true, versao: 1 })

  if (acao === 'codigo') {
    const tel = normalizarTelefone(c.telefone)
    if (!tel) return json({ ok: false, erro: 'telefone_invalido' }, 400)
    const { data, error } = await supabase.rpc('guardioes_otp_gerar', { p_telefone: tel })
    if (error) return json({ ok: false, erro: 'falhou' }, 500)
    const codigo = (data as Record<string, unknown>)?.codigo as string
    const r = await mandarSms(PROJETO_CODIGO, 'otp', tel,
      `Seu codigo GuardioesSOS: ${codigo}. Vale 10 minutos. Nao compartilhe.`)
    return json({ ok: r.ok, erro: r.erro }, r.ok ? 200 : (r.erro === 'limite' ? 429 : 502))
  }

  if (acao === 'entrar') {
    const tel = normalizarTelefone(c.telefone)
    const codigo = String(c.codigo ?? '').replace(/\D/g, '')
    if (!tel || codigo.length !== 6) return json({ ok: false, erro: 'dados_obrigatorios' }, 400)
    const { data, error } = await supabase.rpc('guardioes_otp_verificar', { p_telefone: tel, p_codigo: codigo })
    if (error) return json({ ok: false, erro: 'falhou' }, 500)
    const r = (data ?? {}) as Record<string, unknown>
    if (r.ok !== true) return json({ ok: false, erro: r.erro ?? 'codigo_incorreto' }, 400)
    return json({ ok: true, telefone: tel, chave: await assinarChave(SEGREDO, tel, new Date()) })
  }

  if (acao === 'sos' || acao === 'tudo_bem') {
    const dono = await lerChave(SEGREDO, c.chave)
    if (!dono) return json({ ok: false, erro: 'nao_autorizado' }, 401)
    const { validos, recusados } = separarContatos(c.contatos)
    if (validos.length === 0) return json({ ok: false, erro: 'sem_contatos', recusados }, 400)
    const texto = acao === 'sos'
      ? montarAlerta(c.perfil, c.nome, c.lat, c.lng, new Date())
      : montarTudoBem(c.nome, new Date())
    const resultados: Awaited<ReturnType<typeof mandarSms>>[] = []
    for (const tel of validos) {
      resultados.push(await mandarSms(PROJETO_ALERTA, acao === 'sos' ? 'alerta' : 'tudo_bem', tel, texto))
    }
    const enviados = resultados.filter((r) => r.ok).length
    return json({ ok: enviados > 0, enviados, total: validos.length, recusados, mensagem: texto, resultados },
      enviados > 0 ? 200 : 502)
  }

  return json({ ok: false, erro: 'acao_desconhecida' }, 400)
})

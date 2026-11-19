// Rodar: node --experimental-strip-types --test supabase/functions/guardioes-web/regras.test.ts
import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  assinarChave, lerChave, limparNome, linkDoMapa, montarAlerta, montarTudoBem,
  normalizarTelefone, separarContatos,
} from './regras.ts'

const quando = new Date('2026-11-19T18:05:00Z') // 15:05 em Brasília

test('telefone: padroniza como o app (11 dígitos, acrescenta o 9)', () => {
  assert.equal(normalizarTelefone('(31) 99999-8888'), '31999998888')
  assert.equal(normalizarTelefone('+55 31 99999-8888'), '31999998888')
  assert.equal(normalizarTelefone('031 99999-8888'), '31999998888')
  assert.equal(normalizarTelefone('31 9999-8888'), '31999998888')
  assert.equal(normalizarTelefone('31 3333-4444'), null) // fixo não recebe SMS
  assert.equal(normalizarTelefone('9999-8888'), null)
  assert.equal(normalizarTelefone(''), null)
  assert.equal(normalizarTelefone(undefined), null)
})

test('nome: sem link e sem símbolo, com reserva', () => {
  assert.equal(limparNome('Maria José'), 'Maria José')
  assert.equal(limparNome('http://golpe.com'), 'http golpe com')
  assert.equal(limparNome('   '), 'Um contato seu')
  assert.equal(limparNome('A'.repeat(50)).length, 30)
})

test('mapa: 6 casas, e recusa coordenada inválida', () => {
  assert.equal(linkDoMapa(-19.9191, -43.9386), 'https://maps.google.com/?q=-19.919100,-43.938600')
  assert.equal(linkDoMapa(null, null), null)
  assert.equal(linkDoMapa(200, 0), null)
  assert.equal(linkDoMapa('x', 1), null)
})

test('alerta: mesmo texto do app, sem acento, hora de Brasília', () => {
  assert.equal(
    montarAlerta('seguranca', 'Ana Júlia', -19.9191, -43.9386, quando),
    'ALERTA! Ana Julia acionou o botao de emergencia e pode estar em perigo. Local: https://maps.google.com/?q=-19.919100,-43.938600 - 19/11 15:05',
  )
  assert.equal(
    montarAlerta('saude', '', null, null, quando),
    'ALERTA DE SAUDE! Um contato seu apertou o botao e pode estar passando mal. Local: localizacao indisponivel - 19/11 15:05',
  )
  assert.ok(montarAlerta('seguranca', 'Ana', -19.9, -43.9, quando).length <= 160)
  assert.equal(montarTudoBem('Ana', quando), 'Ana avisou que esta tudo bem. Pode desconsiderar o alerta de emergencia. - 19/11 15:05')
})

test('contatos: só válidos, sem repetir, no máximo 10', () => {
  const r = separarContatos(['31999998888', '(31) 99999-8888', 'abc', ...Array.from({ length: 15 }, (_, i) => `319${String(10000000 + i)}`)])
  assert.equal(r.validos.length, 10)
  assert.equal(r.validos[0], '31999998888')
  assert.deepEqual(r.recusados, ['abc'])
  assert.deepEqual(separarContatos('nao é lista'), { validos: [], recusados: [] })
})

test('chave do aparelho: confere a assinatura e recusa adulteração', async () => {
  const chave = await assinarChave('segredo-de-teste', '31999998888', quando)
  assert.equal(await lerChave('segredo-de-teste', chave), '31999998888')
  assert.equal(await lerChave('outro-segredo', chave), null)
  const [carga, assinatura] = chave.split('.')
  const outraCarga = Buffer.from(JSON.stringify({ t: '11988887777', i: 1 })).toString('base64url')
  assert.equal(await lerChave('segredo-de-teste', `${outraCarga}.${assinatura}`), null)
  assert.equal(await lerChave('segredo-de-teste', `${carga}.${assinatura}.x`), null)
  assert.equal(await lerChave('segredo-de-teste', 'lixo'), null)
  assert.equal(await lerChave('', chave), null)
})

-- A SIRENE respeita o guardião PAUSADO (24/11, pedido do Rodrigo: "desligar o envio por guardião... para um teste,
-- deixar só uma pessoa, ou não mandar no fim de semana").
--
-- O app (0.36.0) manda à guardioes-alertar a lista `pausados` (os números, 11 dígitos, dos guardiões com a chave
-- "Recebe os alertas" desligada). Esta versão da função de 2 argumentos deixa esses números de fora. A de 1
-- argumento continua igual (os apps antigos e a função antiga usam ela).
--
-- Só LEITURA; fechada como as outras (só a service_role, que é a Edge Function).
create or replace function public.guardioes_tokens_dos_guardioes(p_usuario_dispositivo text, p_pausados text[])
returns table(token text)
language sql
security definer
set search_path to 'public', 'guardioes'
as $$
  select distinct u.token_push
  from guardioes.convites c
  join guardioes.usuarios u
    on u.dispositivo = c.guardiao_dispositivo
    or (u.telefone is not null and u.telefone = c.guardiao_telefone)
  where c.usuario_dispositivo = p_usuario_dispositivo
    and c.status = 'aceito'
    and not (right(c.guardiao_telefone, 11) = any(coalesce(p_pausados, '{}'::text[])))
    and u.token_push is not null
    and length(u.token_push) > 20;
$$;

revoke all on function public.guardioes_tokens_dos_guardioes(text, text[]) from public, anon, authenticated;
grant execute on function public.guardioes_tokens_dos_guardioes(text, text[]) to service_role;

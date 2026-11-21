-- Em que pé está cada guardião que um aparelho convidou (21/11, pedido do Rodrigo: deixar claro na tela quem
-- é "Guardião SMS" e quem é "Guardião com app", o que tem o celular tocando).
--
-- Só LEITURA. Por telefone do guardião: o status do convite ('aceito' ganha de 'pendente') e se o app dele
-- tem como receber o aviso (token de push), com a MESMA regra da guardioes_tokens_dos_guardioes (por aparelho
-- OU pelo telefone do guardião). Fechada como as outras: só a service_role (a Edge Function guardioes-convite).
create or replace function public.guardioes_situacao_dos_guardioes(p_usuario_dispositivo text)
returns table(telefone text, status text, tem_app boolean)
language sql
security definer
set search_path to 'public', 'guardioes'
as $$
  select c.guardiao_telefone as telefone,
         case when bool_or(c.status = 'aceito') then 'aceito' else 'pendente' end as status,
         bool_or(
           c.status = 'aceito' and exists (
             select 1 from guardioes.usuarios u
             where (u.dispositivo = c.guardiao_dispositivo
                    or (u.telefone is not null and u.telefone = c.guardiao_telefone))
               and u.token_push is not null
               and length(u.token_push) > 20
           )
         ) as tem_app
  from guardioes.convites c
  where c.usuario_dispositivo = p_usuario_dispositivo
  group by c.guardiao_telefone;
$$;

revoke all on function public.guardioes_situacao_dos_guardioes(text) from public, anon, authenticated;
grant execute on function public.guardioes_situacao_dos_guardioes(text) to service_role;

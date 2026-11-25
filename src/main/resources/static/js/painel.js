const $ = (id) => document.getElementById(id);

const estado = { usuario: null, servicos: [], horario: null, servicoEditando: null };

const ABAS = {
  CLIENTE: [['sec-novo', 'Novo agendamento'], ['sec-agenda', 'Meus agendamentos']],
  FUNCIONARIO: [['sec-agenda', 'Minha agenda']],
  DONO: [['sec-agenda', 'Agenda da barbearia'], ['sec-servicos', 'Serviços'], ['sec-equipe', 'Equipe']]
};
const ROTULO_PERFIL = { DONO: 'Dono', FUNCIONARIO: 'Profissional', CLIENTE: 'Cliente' };
const ROTULO_STATUS = { AGENDADO: 'Agendado', CONCLUIDO: 'Concluído', CANCELADO: 'Cancelado' };
const TITULO_AGENDA = {
  CLIENTE: 'Meus agendamentos',
  FUNCIONARIO: 'Minha agenda',
  DONO: 'Agenda da barbearia'
};

// Executa uma ação e trata erros (401 volta ao login; o resto vira aviso na tela)
async function tratar(acao) {
  try {
    await acao();
  } catch (e) {
    if (e.status === 401) { location.href = 'login.html'; return; }
    mostrarMensagem(e.message, 'erro');
  }
}

// ---------- início ----------
(async function iniciar() {
  const u = await usuarioAtual();
  if (!u) { location.href = 'login.html'; return; }
  estado.usuario = u;
  $('nome-usuario').textContent = u.nome + ' (' + ROTULO_PERFIL[u.perfil].toLowerCase() + ')';
  $('nome-empresa').textContent = u.empresaNome;
  $('titulo-agenda').textContent = TITULO_AGENDA[u.perfil];
  const hora = new Date().getHours();
  const periodo = hora < 12 ? 'Bom dia' : hora < 18 ? 'Boa tarde' : 'Boa noite';
  $('saudacao').textContent = periodo + ', ' + u.nome.split(' ')[0];

  $('btn-sair').addEventListener('click', () => tratar(async () => {
    await api('/api/auth/logout', { metodo: 'POST' });
    location.href = 'login.html';
  }));

  montarAbas();
  ligarEventos();
  await tratar(() => mostrarAba(ABAS[u.perfil][0][0]));
})();

function montarAbas() {
  const nav = $('abas');
  nav.innerHTML = '';
  ABAS[estado.usuario.perfil].forEach(([id, rotulo]) => {
    const b = document.createElement('button');
    b.className = 'aba';
    b.dataset.alvo = id;
    b.textContent = rotulo;
    b.addEventListener('click', () => tratar(() => mostrarAba(id)));
    nav.appendChild(b);
  });
}

async function mostrarAba(id) {
  document.querySelectorAll('.painel-sec').forEach((s) => { s.hidden = s.id !== id; });
  document.querySelectorAll('.aba').forEach((b) => b.classList.toggle('ativa', b.dataset.alvo === id));
  if (id === 'sec-agenda') await carregarAgenda();
  if (id === 'sec-novo') await prepararNovo();
  if (id === 'sec-servicos') await carregarServicos();
  if (id === 'sec-equipe') await carregarEquipe();
}

function ligarEventos() {
  $('lista-agenda').addEventListener('click', (ev) => {
    const btn = ev.target.closest('button[data-acao]');
    if (!btn) return;
    tratar(() => acaoAgendamento(btn.dataset.acao, btn.dataset.id));
  });

  // CLIENTE: novo agendamento
  $('btn-horarios').addEventListener('click', () => tratar(buscarHorarios));
  $('slots').addEventListener('click', (ev) => {
    const btn = ev.target.closest('button[data-hora]');
    if (!btn) return;
    estado.horario = btn.dataset.hora;
    document.querySelectorAll('#slots .slot').forEach((s) => s.classList.toggle('escolhido', s === btn));
    $('btn-confirmar').disabled = false;
  });
  $('btn-confirmar').addEventListener('click', () => tratar(confirmarAgendamento));
  ['sel-servico', 'sel-func', 'data-agendamento'].forEach((id) =>
    $(id).addEventListener('change', limparHorarios));

  // DONO: serviços e equipe
  $('form-servico').addEventListener('submit', (ev) => { ev.preventDefault(); tratar(salvarServico); });
  $('btn-cancelar-edicao').addEventListener('click', cancelarEdicaoServico);
  $('lista-servicos').addEventListener('click', (ev) => {
    const btn = ev.target.closest('button[data-acao]');
    if (!btn) return;
    tratar(() => acaoServico(btn.dataset.acao, Number(btn.dataset.id)));
  });
  $('form-equipe').addEventListener('submit', (ev) => { ev.preventDefault(); tratar(salvarFuncionario); });
  $('lista-equipe').addEventListener('click', (ev) => {
    const btn = ev.target.closest('button[data-acao="desativar-func"]');
    if (!btn) return;
    tratar(async () => {
      if (!confirm('Desativar este profissional?')) return;
      await api('/api/funcionarios/' + encodeURIComponent(btn.dataset.id), { metodo: 'DELETE' });
      mostrarMensagem('Profissional desativado.');
      await carregarEquipe();
    });
  });
}

// ---------- agenda (todos os perfis) ----------
async function carregarAgenda() {
  const lista = await api('/api/agendamentos');
  const ativos = lista.filter((a) => a.status === 'AGENDADO')
    .sort((a, b) => a.inicio.localeCompare(b.inicio));
  const historico = lista.filter((a) => a.status !== 'AGENDADO');
  $('lista-agenda').innerHTML =
    blocoAgenda('Próximos', ativos, 'Nenhum agendamento ativo.') +
    blocoAgenda('Histórico', historico, 'Nada por aqui ainda.');
}

function blocoAgenda(titulo, itens, vazio) {
  const corpo = itens.length ? itens.map(cartaoAgendamento).join('') : `<p class="mudo">${esc(vazio)}</p>`;
  return `<h3>${esc(titulo)}</h3>${corpo}`;
}

function cartaoAgendamento(a) {
  const perfil = estado.usuario.perfil;
  const d = new Date(a.inicio);
  const diaSemana = d.toLocaleString('pt-BR', { weekday: 'short' }).replace('.', '');
  const dia = String(d.getDate()).padStart(2, '0');
  const mes = d.toLocaleString('pt-BR', { month: 'short' }).replace('.', '');
  const hora = d.toLocaleString('pt-BR', { hour: '2-digit', minute: '2-digit' });

  let quem = '';
  if (perfil === 'CLIENTE') quem = `<p class="mudo">Profissional: ${esc(a.funcionario)}</p>`;
  else if (perfil === 'FUNCIONARIO') quem = `<p class="mudo">Cliente: ${esc(a.cliente)}</p>`;
  else quem = `<p class="mudo">Cliente: ${esc(a.cliente)}</p><p class="mudo">Profissional: ${esc(a.funcionario)}</p>`;

  const ativo = a.status === 'AGENDADO';
  const botoes =
    (ativo && perfil !== 'CLIENTE'
      ? `<button class="btn btn-peq" data-acao="concluir" data-id="${esc(a.id)}">Concluir</button>` : '') +
    (ativo
      ? `<button class="btn btn-perigo btn-peq" data-acao="cancelar" data-id="${esc(a.id)}">Cancelar</button>` : '');

  return `<article class="cartao comanda status-${esc(a.status.toLowerCase())}">
    <div class="comanda-data">
      <span class="dia-semana">${esc(diaSemana)}</span>
      <span class="dia">${esc(dia)}</span>
      <span class="mes">${esc(mes)}</span>
    </div>
    <div class="comanda-corpo">
      <div class="cartao-topo">
        <strong>${esc(hora)}, ${esc(a.servico)}</strong>
        <span class="selo selo-${esc(a.status.toLowerCase())}">${esc(ROTULO_STATUS[a.status])}</span>
      </div>
      <p class="preco">${esc(moeda(a.preco))}</p>
      ${quem}
      ${botoes ? `<div class="acoes">${botoes}</div>` : ''}
    </div>
  </article>`;
}

async function acaoAgendamento(acao, id) {
  if (acao === 'cancelar' && !confirm('Cancelar este agendamento?')) return;
  await api('/api/agendamentos/' + encodeURIComponent(id) + '/' + acao, { metodo: 'PATCH' });
  mostrarMensagem(acao === 'cancelar' ? 'Agendamento cancelado.' : 'Agendamento concluído.');
  await carregarAgenda();
}

// ---------- CLIENTE: novo agendamento ----------
async function prepararNovo() {
  const [servicos, funcionarios] = await Promise.all([api('/api/servicos'), api('/api/funcionarios')]);
  estado.servicos = servicos;

  $('sel-servico').innerHTML = servicos.length
    ? servicos.map((s) =>
        `<option value="${esc(s.id)}">${esc(s.nome)}, ${esc(s.duracaoMin)} min, ${esc(moeda(s.preco))}</option>`).join('')
    : '<option value="">Nenhum serviço disponível</option>';

  $('sel-func').innerHTML = funcionarios.length
    ? funcionarios.map((f) => `<option value="${esc(f.id)}">${esc(f.nome)}</option>`).join('')
    : '<option value="">Nenhum profissional disponível</option>';

  const data = $('data-agendamento');
  data.min = hojeISO();
  if (!data.value || data.value < data.min) data.value = data.min;
  limparHorarios();
}

function limparHorarios() {
  estado.horario = null;
  $('slots').innerHTML = '';
  $('btn-confirmar').disabled = true;
  $('aviso-horarios').textContent = '';
}

async function buscarHorarios() {
  const servicoId = $('sel-servico').value;
  const funcionarioId = $('sel-func').value;
  const data = $('data-agendamento').value;
  if (!servicoId || !funcionarioId || !data) {
    mostrarMensagem('Escolha serviço, profissional e data.', 'erro');
    return;
  }
  limparHorarios();
  const qs = new URLSearchParams({ servicoId, funcionarioId, data });
  const horarios = await api('/api/agendamentos/horarios?' + qs.toString());
  if (horarios.length === 0) {
    $('aviso-horarios').textContent = 'Nenhum horário livre neste dia. Tente outra data ou profissional.';
    return;
  }
  $('slots').innerHTML = horarios
    .map((h) => `<button type="button" class="slot" data-hora="${esc(h)}">${esc(h)}</button>`).join('');
}

async function confirmarAgendamento() {
  if (!estado.horario) return;
  const corpo = {
    servicoId: Number($('sel-servico').value),
    funcionarioId: Number($('sel-func').value),
    inicio: $('data-agendamento').value + 'T' + estado.horario
  };
  await api('/api/agendamentos', { metodo: 'POST', corpo });
  mostrarMensagem('Agendamento confirmado!');
  await mostrarAba('sec-agenda');
}

// ---------- DONO: serviços ----------
async function carregarServicos() {
  estado.servicos = await api('/api/servicos');
  $('lista-servicos').innerHTML = estado.servicos.length
    ? estado.servicos.map(cartaoServico).join('')
    : '<p class="mudo">Nenhum serviço cadastrado.</p>';
}

function cartaoServico(s) {
  return `<article class="cartao">
    <div class="cartao-topo">
      <strong>${esc(s.nome)}</strong>
      <span class="selo ${s.ativo ? 'selo-ativo' : 'selo-inativo'}">${s.ativo ? 'Ativo' : 'Inativo'}</span>
    </div>
    <p class="mudo">${esc(s.duracaoMin)} min, ${esc(moeda(s.preco))}</p>
    <div class="acoes">
      <button class="btn btn-sec btn-peq" data-acao="editar" data-id="${esc(s.id)}">Editar</button>
      <button class="btn btn-sec btn-peq" data-acao="alternar" data-id="${esc(s.id)}">${s.ativo ? 'Desativar' : 'Ativar'}</button>
    </div>
  </article>`;
}

async function salvarServico() {
  const corpo = {
    nome: $('s-nome').value.trim(),
    duracaoMin: Number($('s-duracao').value),
    preco: Number($('s-preco').value)
  };
  if (estado.servicoEditando) {
    const atual = estado.servicos.find((s) => s.id === estado.servicoEditando);
    corpo.ativo = atual ? atual.ativo : true;
    await api('/api/servicos/' + estado.servicoEditando, { metodo: 'PUT', corpo });
    mostrarMensagem('Serviço atualizado.');
  } else {
    await api('/api/servicos', { metodo: 'POST', corpo });
    mostrarMensagem('Serviço criado.');
  }
  cancelarEdicaoServico();
  await carregarServicos();
}

async function acaoServico(acao, id) {
  const s = estado.servicos.find((x) => x.id === id);
  if (!s) return;
  if (acao === 'editar') {
    estado.servicoEditando = id;
    $('s-nome').value = s.nome;
    $('s-duracao').value = s.duracaoMin;
    $('s-preco').value = s.preco;
    $('titulo-form-servico').textContent = 'Editar serviço';
    $('btn-cancelar-edicao').hidden = false;
    $('s-nome').focus();
  } else if (acao === 'alternar') {
    await api('/api/servicos/' + id, {
      metodo: 'PUT',
      corpo: { nome: s.nome, duracaoMin: s.duracaoMin, preco: s.preco, ativo: !s.ativo }
    });
    await carregarServicos();
  }
}

function cancelarEdicaoServico() {
  estado.servicoEditando = null;
  $('form-servico').reset();
  $('titulo-form-servico').textContent = 'Novo serviço';
  $('btn-cancelar-edicao').hidden = true;
}

// ---------- DONO: equipe ----------
async function carregarEquipe() {
  const lista = await api('/api/funcionarios');
  $('lista-equipe').innerHTML = lista.length
    ? lista.map((f) => `<article class="cartao">
        <div class="cartao-topo"><strong>${esc(f.nome)}</strong></div>
        <p class="mudo">${esc(f.email)}</p>
        <div class="acoes">
          <button class="btn btn-perigo btn-peq" data-acao="desativar-func" data-id="${esc(f.id)}">Desativar</button>
        </div>
      </article>`).join('')
    : '<p class="mudo">Nenhum profissional cadastrado.</p>';
}

async function salvarFuncionario() {
  await api('/api/funcionarios', {
    metodo: 'POST',
    corpo: {
      nome: $('f-nome').value.trim(),
      email: $('f-email').value.trim(),
      senha: $('f-senha').value
    }
  });
  $('form-equipe').reset();
  mostrarMensagem('Profissional cadastrado.');
  await carregarEquipe();
}

const $ = (id) => document.getElementById(id);

// Alternar entre "sou cliente" e "cadastrar minha barbearia"
function mostrarForm(qual) {
  $('form-cliente').hidden = qual !== 'cliente';
  $('form-empresa').hidden = qual !== 'empresa';
  $('aba-cliente').classList.toggle('ativa', qual === 'cliente');
  $('aba-empresa').classList.toggle('ativa', qual === 'empresa');
  $('erro').textContent = '';
}
$('aba-cliente').addEventListener('click', () => mostrarForm('cliente'));
$('aba-empresa').addEventListener('click', () => mostrarForm('empresa'));

// Carrega as barbearias disponíveis para o cliente escolher
(async () => {
  try {
    const lista = await api('/api/publico/empresas');
    const sel = $('empresa-id');
    if (lista.length === 0) {
      sel.innerHTML = '<option value="">Nenhuma barbearia cadastrada ainda</option>';
    } else {
      sel.innerHTML = '<option value="">Escolha a barbearia</option>' +
        lista.map((e) => `<option value="${esc(e.id)}">${esc(e.nome)}</option>`).join('');
    }
  } catch (e) {
    $('erro').textContent = 'Não foi possível carregar as barbearias.';
  }
})();

async function cadastrar(caminho, corpo, email, senha) {
  $('erro').textContent = '';
  try {
    await api(caminho, { metodo: 'POST', corpo });
    const ok = await fazerLogin(email, senha);
    location.href = ok ? 'app.html' : 'login.html';
  } catch (e) {
    $('erro').textContent = e.message;
  }
}

$('form-cliente').addEventListener('submit', (ev) => {
  ev.preventDefault();
  const email = $('c-email').value.trim();
  const senha = $('c-senha').value;
  cadastrar('/api/auth/cadastro-cliente', {
    empresaId: Number($('empresa-id').value) || null,
    nome: $('c-nome').value.trim(),
    email,
    senha
  }, email, senha);
});

$('form-empresa').addEventListener('submit', (ev) => {
  ev.preventDefault();
  const email = $('e-email').value.trim();
  const senha = $('e-senha').value;
  cadastrar('/api/auth/cadastro-empresa', {
    nomeEmpresa: $('e-empresa').value.trim(),
    cnpj: $('e-cnpj').value.trim(),
    nome: $('e-nome').value.trim(),
    email,
    senha
  }, email, senha);
});

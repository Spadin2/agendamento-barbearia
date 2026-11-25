(async () => {
  // Se já existe sessão, vai direto para o painel
  if (await usuarioAtual()) location.href = 'app.html';
})();

document.getElementById('form-login').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const erro = document.getElementById('erro');
  const botao = ev.submitter || document.querySelector('#form-login button[type=submit]');
  erro.textContent = '';
  botao.disabled = true;
  try {
    const ok = await fazerLogin(
      document.getElementById('email').value.trim(),
      document.getElementById('senha').value
    );
    if (ok) {
      location.href = 'app.html';
    } else {
      erro.textContent = 'E-mail ou senha inválidos.';
    }
  } catch (_) {
    erro.textContent = 'Não foi possível conectar ao servidor.';
  } finally {
    botao.disabled = false;
  }
});

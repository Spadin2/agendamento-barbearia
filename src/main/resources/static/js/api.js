// Funções compartilhadas por todas as páginas.

function csrfToken() {
  const m = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
  return m ? decodeURIComponent(m[1]) : '';
}

// Garante que o cookie de CSRF existe antes de qualquer POST/PUT/PATCH/DELETE
async function garantirCsrf() {
  if (!csrfToken()) {
    await fetch('/api/publico/empresas', { credentials: 'same-origin' });
  }
}

// Escapa texto antes de colocar em innerHTML (evita XSS)
function esc(valor) {
  return String(valor ?? '').replace(/[&<>"']/g, (c) => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
  ));
}

async function api(caminho, { metodo = 'GET', corpo } = {}) {
  const headers = { Accept: 'application/json' };
  if (metodo !== 'GET') {
    await garantirCsrf();
    headers['X-XSRF-TOKEN'] = csrfToken();
  }
  if (corpo !== undefined) headers['Content-Type'] = 'application/json';

  const resp = await fetch(caminho, {
    method: metodo,
    headers,
    credentials: 'same-origin',
    body: corpo !== undefined ? JSON.stringify(corpo) : undefined
  });

  if (resp.status === 401) {
    const e = new Error('Sessão expirada. Faça login novamente.');
    e.status = 401;
    throw e;
  }
  if (!resp.ok) {
    let msg = resp.status === 403
      ? 'Ação não permitida (ou a sessão expirou: recarregue a página).'
      : 'Erro inesperado (' + resp.status + ').';
    try {
      const d = await resp.json();
      if (d && d.erro) msg = d.erro;
    } catch (_) { /* resposta sem JSON */ }
    const e = new Error(msg);
    e.status = resp.status;
    throw e;
  }
  if (resp.status === 204) return null;
  const tipo = resp.headers.get('content-type') || '';
  return tipo.includes('application/json') ? resp.json() : null;
}

// Login: o Spring espera um formulário (e-mail + senha), não JSON
async function fazerLogin(email, senha) {
  await garantirCsrf();
  const resp = await fetch('/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'X-XSRF-TOKEN': csrfToken()
    },
    credentials: 'same-origin',
    body: new URLSearchParams({ email, senha })
  });
  return resp.ok;
}

async function usuarioAtual() {
  try { return await api('/api/me'); } catch (_) { return null; }
}

function moeda(valor) {
  return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function dataHora(iso) {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

function hojeISO() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, '0');
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate());
}

let temporizadorToast;
function mostrarMensagem(texto, tipo = 'ok') {
  const el = document.getElementById('toast');
  if (!el) { alert(texto); return; }
  el.textContent = texto;
  el.className = 'toast' + (tipo === 'erro' ? ' erro' : '');
  el.hidden = false;
  clearTimeout(temporizadorToast);
  temporizadorToast = setTimeout(() => { el.hidden = true; }, 4000);
}

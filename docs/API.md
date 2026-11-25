# AgendaFácil · Contrato da API e fluxo de dados

## Fluxo de dados

```
Navegador ──(HTML/CSS/JS estáticos)──▶ Spring Boot (src/main/resources/static)
JS (fetch) ──▶ /api/** ──▶ Filtro de segurança (sessão + CSRF + perfil)
            ──▶ Controller ──▶ Service (regras, tenant = empresa da sessão)
            ──▶ Repository (JPA) ──▶ MySQL
            ◀── JSON ◀──────────────────────────────────────────────┘
```

- **Autenticação:** login em `POST /api/auth/login` (form: `email`, `senha`). O Spring cria a sessão (cookie `JSESSIONID`).
- **Autorização:** perfis `DONO`, `FUNCIONARIO` e `CLIENTE` (`@PreAuthorize` nos controllers).
- **Multi-tenant:** o `empresaId` vem sempre da sessão, nunca do front.
- **CSRF:** o JS lê o cookie `XSRF-TOKEN` e envia no header `X-XSRF-TOKEN` em POST/PUT/PATCH/DELETE.
- **Erros:** JSON `{"erro": "mensagem"}` com o status HTTP adequado (400, 401, 403, 404, 409).

## Endpoints

| Método | Rota | Quem acessa | Descrição |
|--------|------|-------------|-----------|
| POST | `/api/auth/login` | público | Login (form-urlencoded: `email`, `senha`) |
| POST | `/api/auth/logout` | logado | Encerra a sessão |
| POST | `/api/auth/cadastro-empresa` | público | Cria barbearia + usuário DONO |
| POST | `/api/auth/cadastro-cliente` | público | Cria cliente em uma barbearia |
| GET | `/api/publico/empresas` | público | Lista barbearias ativas (id, nome) |
| GET | `/api/me` | logado | Dados do usuário da sessão |
| GET | `/api/servicos` | logado | DONO vê todos; demais veem só ativos |
| POST | `/api/servicos` | DONO | Cria serviço |
| PUT | `/api/servicos/{id}` | DONO | Atualiza serviço (`ativo` opcional) |
| DELETE | `/api/servicos/{id}` | DONO | Desativa serviço |
| GET | `/api/funcionarios` | logado | Lista profissionais ativos |
| POST | `/api/funcionarios` | DONO | Cadastra profissional |
| DELETE | `/api/funcionarios/{id}` | DONO | Desativa profissional |
| GET | `/api/agendamentos` | logado | DONO: todos; FUNCIONARIO: os seus; CLIENTE: os seus |
| GET | `/api/agendamentos/horarios?funcionarioId=&servicoId=&data=AAAA-MM-DD` | logado | Horários livres (09:00 às 18:00, de 30 em 30 min) |
| POST | `/api/agendamentos` | CLIENTE | Cria agendamento (`servicoId`, `funcionarioId`, `inicio`) |
| PATCH | `/api/agendamentos/{id}/cancelar` | dono do agendamento ou DONO | Cancela |
| PATCH | `/api/agendamentos/{id}/concluir` | FUNCIONARIO do agendamento ou DONO | Conclui |

## Banco de dados

O script SQL é entregue separadamente ao professor e não faz parte deste repositório.

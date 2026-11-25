# AgendaFácil

SaaS de agendamento online para barbearias. Cada barbearia é um **tenant**: tem seus próprios usuários, serviços e agendamentos, com os dados isolados das demais.

O cliente escolhe o serviço, o profissional e um horário livre. O dono acompanha a agenda inteira e o profissional vê só a própria.

## Funcionalidades

- Cadastro de barbearia (cria o dono) e de clientes
- Login com sessão, autenticação e autorização por perfil
- Gestão de serviços (nome, duração, preço, ativar e desativar)
- Gestão da equipe (cadastro e desativação de profissionais)
- Agendamento com consulta de horários livres (09:00 às 18:00, de 30 em 30 minutos)
- Cancelamento e conclusão de atendimentos
- Bloqueio de conflito de horário para o mesmo profissional
- Isolamento de dados entre barbearias (multi-tenant)

## Perfis e permissões

| Perfil | Pode |
|---|---|
| `DONO` | Gerenciar serviços e equipe, ver e alterar toda a agenda da barbearia |
| `FUNCIONARIO` | Ver a própria agenda, concluir e cancelar seus atendimentos |
| `CLIENTE` | Agendar, ver e cancelar os próprios agendamentos |

A lista completa de rotas está em [`docs/API.md`](docs/API.md).

## Tecnologias

- **Back-end:** Java 17, Spring Boot 3.3, Spring Data JPA, Spring Security
- **Banco de dados:** MySQL 8
- **Front-end:** HTML, CSS e JavaScript, servidos pelo próprio Spring Boot
- **Build:** Maven
- **Versionamento:** Git e GitHub

## Como funciona (fluxo de dados)

```
Navegador ── HTML/CSS/JS estáticos ──▶ Spring Boot (src/main/resources/static)

JS (fetch) ──▶ /api/**
   ──▶ Filtro de segurança (sessão, CSRF e perfil)
   ──▶ Controller
   ──▶ Service (regras de negócio; a empresa vem sempre da sessão)
   ──▶ Repository (JPA)
   ──▶ MySQL
   ◀── JSON de volta até o JavaScript, que atualiza a tela
```

A página e a API ficam no mesmo endereço, então a sessão por cookie funciona sem CORS.

## Estrutura do projeto

```
agendamento-barbearia/
├── pom.xml                          dependências e configuração do serviço (XML)
├── docs/API.md                      contrato da API
└── src/main/
    ├── java/com/agendafacil/
    │   ├── controller/              rotas /api/...
    │   ├── service/                 regras de negócio
    │   ├── repository/              acesso ao MySQL
    │   ├── model/                   entidades (tabelas)
    │   ├── dto/                     objetos de entrada e saída da API
    │   ├── security/                login, sessão, perfis e CSRF
    │   └── exception/               tratamento de erros em JSON
    └── resources/
        ├── application.properties   configuração (usa variáveis de ambiente)
        └── static/                  front-end (HTML, CSS e JS)
```

## Como executar

### Pré-requisitos

- JDK 17
- MySQL 8 e MySQL Workbench
- IntelliJ IDEA (ou Maven instalado)

### Passo a passo

1. **Clone o repositório**
   ```
   git clone https://github.com/Spadin2/agendamento-barbearia.git
   ```
2. **Crie o banco de dados.** O script SQL não faz parte deste repositório: ele é entregue separadamente ao professor, e o grupo recebe o arquivo por fora. Abra o script no MySQL Workbench e execute-o inteiro (ele usa `DELIMITER` nos triggers, por isso precisa de um cliente MySQL de verdade).
3. **Defina as variáveis de ambiente** (veja a seção abaixo).
4. **Suba a aplicação:** rode a classe `AgendafacilApplication` no IntelliJ, ou use `mvn spring-boot:run`.
5. **Acesse** `http://localhost:8080`.

## Variáveis de ambiente

Nenhuma credencial fica no código ou no Git. O `application.properties` lê tudo de variáveis:

| Variável | Obrigatória | Descrição | Exemplo |
|---|---|---|---|
| `DB_URL` | sim | URL de conexão JDBC com o MySQL | `jdbc:mysql://localhost:3306/agendafacil` |
| `DB_USER` | sim | Usuário do banco | `root` |
| `DB_PASSWORD` | sim | Senha do banco | (a sua) |
| `PORT` | não | Porta do servidor (padrão `8080`) | `8081` |

**No IntelliJ:** Run → Edit Configurations → selecione `AgendafacilApplication` → campo *Environment variables*:
```
DB_URL=jdbc:mysql://localhost:3306/agendafacil;DB_USER=root;DB_PASSWORD=sua_senha
```

**No PowerShell (Windows):**
```
$env:DB_URL="jdbc:mysql://localhost:3306/agendafacil"
$env:DB_USER="root"
$env:DB_PASSWORD="sua_senha"
$env:PORT="8081"
mvn spring-boot:run
```

**Operando as variáveis na prática:** o mesmo código roda em ambientes diferentes só trocando os valores. Com `PORT=8081`, o serviço passa a responder em `http://localhost:8081`. Apontando `DB_URL` para outro banco, o serviço usa outro conjunto de dados, sem recompilar nada.

Se aparecer o erro `Public Key Retrieval is not allowed`, acrescente `?allowPublicKeyRetrieval=true&useSSL=false` ao final do `DB_URL`.

## Segurança

- **Autenticação:** login por e-mail e senha. A senha é guardada apenas como hash BCrypt.
- **Sessão:** cookie `JSESSIONID` (HttpOnly, SameSite Lax), expira em 30 minutos sem uso e é encerrada no logout.
- **Autorização:** regras por perfil (`@PreAuthorize`) nos controllers.
- **Multi-tenant:** o `empresaId` vem sempre da sessão, nunca de um valor enviado pelo front. Uma barbearia não enxerga os dados de outra.
- **CSRF:** proteção ligada. O JavaScript envia o token no header `X-XSRF-TOKEN`.
- **Segredos:** senhas do banco apenas em variáveis de ambiente; `.gitignore` bloqueia `.env`, `.idea/`, `target/` e arquivos `.sql`.

## Problemas recorrentes em nuvem

Pontos que costumam dar problema ao colocar um sistema como este em produção, e como se relacionam com o projeto:

| Problema | Por que acontece | No AgendaFácil |
|---|---|---|
| **Concorrência** | Dois clientes tentam o mesmo horário ao mesmo tempo | O service trava a linha do profissional (`SELECT ... FOR UPDATE`) e confere sobreposição; um trigger no banco é a segunda barreira. O segundo cliente recebe erro 409 |
| **Fuso horário** | Servidores na nuvem costumam rodar em UTC | Os horários são gravados sem fuso e o expediente é fixo (09:00 às 18:00). Em um servidor em UTC, o horário "agora" ficaria deslocado. Solução: definir o fuso do servidor ou tratar fuso explicitamente |
| **Vazamento de segredos** | Senha de banco commitada no repositório | Credenciais só em variáveis de ambiente; `.gitignore` protege arquivos locais |
| **Sessão em memória** | Com mais de uma instância do servidor, a sessão não é compartilhada, e reiniciar derruba os logins | Hoje a sessão fica na memória. Para escalar, usar Spring Session (Redis ou JDBC) ou tokens |
| **Banco como ponto único de falha** | Se o banco cai, tudo cai; sem backup, dados se perdem | Recomenda-se banco gerenciado, backup automático e réplica |
| **Isolamento entre clientes** | Um erro de filtro pode expor dados de outra empresa | Todo acesso filtra pelo `empresaId` da sessão |
| **Tráfego sem HTTPS** | Cookie e senha podem ser interceptados | Em produção o cookie de sessão deve ser `Secure` e o acesso apenas por HTTPS |
| **Custo e disponibilidade** | Recursos mal dimensionados ou sem monitoramento | Exigem limites, monitoramento e verificações de saúde do serviço |

## Limitações conhecidas

- Sem recuperação de senha e sem bloqueio de tentativas de login
- Horário de funcionamento fixo no código (09:00 às 18:00)
- O e-mail é único no sistema inteiro: um cliente pertence a uma só barbearia
- Desativar um profissional não cancela os agendamentos futuros dele
- A sessão fica na memória do servidor

## Equipe

- Guilherme Spadin Domingues
- Gabrielly Grasso Miranda
- William Bryam dos Reis
- Lael Gonsalves Rodrigues Junior

Projeto acadêmico.

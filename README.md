# AgendaFácil

Barbearia numero um

SAAS de agendamento para barbearias. Cada negócio é um **tenant**: tem seus próprios usuários, serviços e agendamentos, com os dados isolados dos demais.

## Funcionalidades

- Cadastro e login com autenticação e autorização por perfil
- Perfis: `DONO`, `FUNCIONARIO` e `CLIENTE`
- Gestão de serviços (nome, duração, preço)
- Agendamento de horários, sem conflito para o mesmo funcionário
- Isolamento de dados entre empresas (multi-tenant)

## Perfis e permissões

| Perfil      | Pode                                                      |
|-------------|-----------------------------------------------------------|
| DONO        | Gerenciar serviços, funcionários e ver todos os agendamentos da empresa |
| FUNCIONARIO | Ver e atualizar a própria agenda                          |
| CLIENTE     | Criar, ver e cancelar os próprios agendamentos            |

## Tecnologias

- Back-end: Java + Spring Boot (Spring Data JPA, Spring Security)
- Front-end: HTML, CSS e JavaScript
- Banco de dados: MySQL
- Controle de versão: Git + GitHub
- Testes de API: Postman

## Como executar

1. Clone o repositório
2. Crie o banco rodando `schema.sql` no MySQL
3. Configure as variáveis de ambiente:

   | Variável      | Descrição                  |
   |---------------|----------------------------|
   | `DB_URL`      | URL de conexão com o banco |
   | `DB_USER`     | Usuário do banco           |
   | `DB_PASSWORD` | Senha do banco             |

4. Suba a aplicação com `mvn spring-boot:run` (requer JDK 17 e Maven)

> Nunca commite senhas ou credenciais no repositório.

## Estrutura do banco

Tabelas: `empresas`, `usuarios`, `servicos`, `agendamentos`. Script completo em [`schema.sql`](schema.sql).

## Fluxo de dados

Cliente → API → validação de sessão e tenant → serviço → banco de dados → resposta.
_(diagrama a ser adicionado)_

## Equipe

- Guilherme Spadin Domingues
- Gabrielly Grasso Miranda
- Lael Gonsalves Rodrigues Junior
- William Bryan dos Reis

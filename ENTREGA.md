# Entrega do checkout

## Registro de TDD

Os trechos abaixo vêm das execuções feitas entre as mudanças. As contagens incluem os testes que já existiam em cada etapa.

### Ciclo 1 — Solicitação nula

- **Teste** `deveRejeitarSolicitacaoNulaSemConsultarColaboradores`
- **Red.** O teste esperava `IllegalArgumentException` e recebeu `UnsupportedOperationException`. Resultado: 0 de 1 passou.
- **Green.** Adicionei a verificação de solicitação nula no início de `finalizar`. Resultado: 1 de 1 passou.
- **Refactor.** A validação era um único `if` e ainda não havia repetição a extrair.

### Ciclo 2 — Demais entradas inválidas

- **Teste** `deveRejeitarDadosInvalidosSemConsultarColaboradores`
- **Red.** As 14 novas entradas recebiam `UnsupportedOperationException` em vez de `IllegalArgumentException`. Resultado: 1 de 15 passou.
- **Green.** Ampliei a validação para os três textos, a quantidade e o preço, antes de qualquer interação. Resultado: 15 de 15 passaram.
- **Refactor.** Extraí `textoInvalido` para reaproveitar a checagem de texto nulo ou em branco. Os 15 testes passaram de novo.

### Ciclo 3 — Compra aprovada

- **Teste** `deveSalvarPedidoEConfirmarCompraAprovadaNaOrdem`
- **Red.** A entrada válida ainda chegava ao trecho que lançava `UnsupportedOperationException`. Resultado: 15 de 16 passaram.
- **Green.** Implementei o cálculo com `BigDecimal` e a sequência de reservar, cobrar, salvar e notificar, retornando o resultado. Resultado: 16 de 16 passaram.
- **Refactor.** Mantive o fluxo em sequência direta porque cada chamada corresponde a um passo da compra. Os dois casos de limite adicionados depois elevaram a suíte para 18 testes, todos verdes.

## Respostas

- **O que foi mockado e por quê?** Estoque, pagamento, repositório e notificador. São as fronteiras externas. Os mocks deixam programar as respostas e observar os efeitos sem acessar banco, rede ou serviços de verdade.
- **Qual teste verifica estado e qual verifica interação?** `deveCalcularTotalExato` verifica o estado do resultado ao conferir seu total e também verifica a interação ao conferir o total enviado à cobrança. `devePararQuandoNaoHaEstoque` verifica que a reserva foi tentada e que os três colaboradores seguintes ficaram sem interações.
- **Que defeito o captor revelaria?** Um pedido salvo com o ID de transação errado, por exemplo. O captor pega o objeto enviado a `salvar`, e a comparação do record confere os seis campos.
- **Por que a ordem importa?** O estoque precisa estar reservado antes da cobrança. O pedido precisa ser salvo antes da confirmação, porque o número enviado ao cliente vem do repositório.

## Desafio opcional

Adicionei temporariamente um arredondamento para duas casas decimais. O segundo caso de `deveCalcularTotalExato` falhou porque a cobrança recebeu `0.02` em vez de `0.015`. Restaurei a multiplicação exata e os 22 testes voltaram a passar.

## Execução

Rode `mvn test` na pasta do `pom.xml` com JDK 21.

Os registros de TDD foram obtidos com JDK 21, JUnit Jupiter 5.11.4 e Mockito 5.14.2, disponíveis no cache local. Nessa execução, os 22 testes passaram. O `pom.xml` mantém as versões do projeto inicial.

A validação final com Maven usou JUnit Jupiter 5.13.4 e Mockito 5.18.0, conforme o projeto. O Maven confirmou 22 testes, zero falhas e zero erros. Coloquei o argumento do agente Mockito entre aspas no `pom.xml` para aceitar caminhos com espaços no Windows.

Para repetir a validação com o cache usado aqui, no PowerShell:

```powershell
mvn -o '-Dmaven.repo.local=target/maven-cache' test
```

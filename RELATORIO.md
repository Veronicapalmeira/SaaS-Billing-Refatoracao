# Relatório Técnico de Refatoração — SaaS Billing Co.

Este relatório documenta a refatoração do serviço de cobrança mensal `SubscriptionBillingService`. O ponto de partida é o código legado registrado no commit `5eaf3f6` ("chore: adiciona código legado sem alterações"), arquivo `SubscriptionBillingService-pratica.java`. As linhas citadas na seção 1 referem-se a esse arquivo.

## 1. Diagnóstico do débito técnico

| Trecho/linha afetado | Code smell ou princípio violado | "Juros" (impacto na manutenibilidade e na entrega contínua) |
|---|---|---|
| L18–105 — método `processMonthlyBilling` inteiro (88 linhas, complexidade ciclomática 18) | **Long Method** | Toda mudança de regra, de banco ou de e-mail mexe no mesmo método, o que aumenta o tempo de revisão e a chance de conflito de merge. Não é possível testar uma regra isolada; cada alteração exige reexecutar mentalmente os 18 caminhos, e o pipeline não oferece rede de segurança. |
| L12–105 — classe que calcula preço, consulta o banco, simula o pagamento, grava a fatura e monta o e-mail | **Violação do SRP / Divergent Change** | A classe muda por pelo menos quatro motivos diferentes. Trocar o provedor de SMTP obriga a revalidar o cálculo de preços, e qualquer deploy carrega risco de regressão em responsabilidades que não foram alteradas. |
| L14–16 (credenciais fixas), L59 (`DriverManager.getConnection`), L89 (`new SimpleEmail()`) | **Violação do DIP / dependências concretas fixas no código** | Não há como substituir banco e SMTP por dublês, então qualquer teste do serviço precisa de PostgreSQL e de servidor de e-mail, o que deixa a CI lenta, frágil e dependente de ambiente. Além disso, a senha fica versionada no repositório e trocar de ambiente exige recompilar. |
| L25 — `LocalDate.now()` | **Dependência oculta do relógio do sistema** | O resultado muda conforme o dia da execução: o cupom `BLACKFRIDAY` só é testável em novembro e as faixas de fidelidade mudam com o passar dos meses. Testes ficam não determinísticos (*flaky*) e cenários históricos não são reproduzíveis. |
| L100–102 — `catch (Exception e)` que transforma qualquer falha em uma linha de log | **Error Hiding / captura genérica de exceções** | Queda do banco, e-mail inválido e erro de programação recebem o mesmo tratamento e o chamador recebe uma lista "normal". A falha não quebra o build nem dispara alerta, e estados parciais (pagamento aprovado sem fatura gravada) passam despercebidos em produção. |
| L18–20 — oito parâmetros primitivos (`double basePrice`, `String signupDateStr`, `boolean isNonProfit`…) | **Long Parameter List / Primitive Obsession** | É fácil trocar a ordem de argumentos do mesmo tipo sem erro de compilação. Cada novo dado de cobrança altera a assinatura pública e quebra todos os chamadores. |
| L29–57 e L85 — valores monetários em `double`, gravados com `setDouble` | **Primitive Obsession (dinheiro em ponto flutuante)** | Surgem resíduos binários nos valores cobrados e persistidos (ex.: `85.0 * 0.70 = 59.49999999999999`). Conciliações financeiras divergem e os testes precisam de tolerâncias arbitrárias. |
| L31–53 — `0.15`, `0.10`, `0.70`, `30.0`, `0.02`, `0.001`, `11`, `"BLACKFRIDAY"`, `"WELCOME10"` | **Magic Numbers** | As regras de negócio não têm nome nem lugar único. Alterar uma alíquota exige caçar literais no método, e o código não documenta a intenção da regra. |
| L18 — parâmetro `planType` nunca utilizado | **Parâmetro morto / Speculative Generality** | A assinatura sugere que o plano influencia o preço, o que induz o leitor a erro. Os chamadores precisam fornecer um dado que nunca é usado. |
| L96–98 — `// emailSender.send();` seguido do log "E-mail disparado para" | **Código comentado (Dead Code) e log enganoso** | O log afirma um envio que não acontece, o que confunde o suporte e a auditoria. O código comentado fica sem dono e sem explicação. |
| L60–63 e L82–87 — `PreparedStatement` e `ResultSet` sem fechamento | **Vazamento de recursos** | Sob carga, cursores e conexões podem se esgotar no banco, gerando falhas intermitentes difíceis de reproduzir fora de produção. |

## 2. Métricas antes vs. depois

**Como foi medido:**
- **Antes:** checkout do commit `5eaf3f6`, que contém apenas o arquivo legado, sem `pom.xml` e sem testes.
- **Depois:** estado final da branch `main`.
- **Complexidade ciclomática e NLOC:** ferramenta `lizard` 1.24.3.
- **Cobertura:** JaCoCo 0.8.14, via `mvn clean test`.
- **Linhas do método:** da linha da assinatura até a chave de fechamento.

| Métrica | Antes (commit `5eaf3f6`) | Depois (estado final) |
|---|---|---|
| Linhas do método principal `processMonthlyBilling` | 88 (L18–105) | 18 (L26–43) |
| Linhas de código efetivas do método (NLOC) | 70 | 16 |
| Complexidade ciclomática do método principal | 18 | 4 |
| Maior complexidade ciclomática entre os métodos de produção | 18 | 5 (`PricingCalculator.applyPromoCode`) |
| Parâmetros do método principal | 8 | 1 (`BillingCommand`) |
| Dependências concretas do serviço | 3: JDBC (`DriverManager`), SMTP (`SimpleEmail`) e relógio do sistema (`LocalDate.now()`) | 0 de infraestrutura: recebe `CustomerRepository`, `PaymentGateway`, `NotificationService` e `Clock` pelo construtor. A única classe concreta usada é `PricingCalculator`, de domínio puro. |
| Classes (tipos) de produção | 1 | 14 (11 classes/records/enum + 3 interfaces) |
| Classes de teste | 0 | 7 (4 classes de teste + 3 implementações falsas em memória) |
| Testes automatizados | 0 | 33 métodos de teste, 48 execuções (com testes parametrizados), todos passando |
| Cobertura de testes (JaCoCo) | 0% (não há testes nem build) | 74,9% das instruções e 80,0% das branches no total. Sem as classes de infraestrutura (`JdbcCustomerRepository` e `SmtpNotificationService`): 98,9% das instruções e 100% das branches. |
| Blocos `catch (Exception e)` | 1 | 0 |
| Chamadas a `LocalDate.now()` sem `Clock` | 1 | 0 |

As classes `JdbcCustomerRepository` e `SmtpNotificationService` não são cobertas porque dependem de banco e de servidor SMTP reais. Por regra da atividade, os testes unitários não usam infraestrutura externa.

## 3. Refatorações do catálogo aplicadas

**Commit 2 — Value Objects (`c5a57e0`, `ee4ca8c`)**
- *Introduce Parameter Object*: os oito parâmetros viraram o record `BillingCommand`.
- *Replace Primitive with Object*: o valor monetário passou a ser o record `Money` (`BigDecimal`).

**Commit 3 — Interfaces e injeção de dependência**
- *Extract Interface*: `NotificationService`, `CustomerRepository` e `PaymentGateway`.
- *Extract Class* e *Move Method*: o acesso a banco foi para `JdbcCustomerRepository`, o e-mail para `SmtpNotificationService` e a validação do cartão para `CardTokenPaymentGateway`.
- *Move Field*: as credenciais do banco e a configuração de SMTP saíram do serviço e foram para as implementações concretas.
- *Introduce Parameter Object*: os dados do cliente passaram a ser o record `Customer`, com o método `isActive()`.
- *Replace Type Code with Class*: os status `"PAID"`/`"FAILED"` viraram o enum `InvoiceStatus`.
- *Parameterize Constructor (injeção de dependência)*: as três interfaces e o `java.time.Clock` são recebidos pelo construtor, e `LocalDate.now()` virou `LocalDate.now(clock)`.
- *Replace Error Code with Exception*: o `catch (Exception e)` foi removido. Falhas de banco e de e-mail propagam como `CustomerRepositoryException` e `NotificationException`. Os recursos JDBC passaram a ser fechados com *try-with-resources*.

**Commit 4 — Motor de precificação e suíte verde**
- *Extract Class* e *Move Method*: todas as regras de preço foram para a classe de domínio pura `PricingCalculator`, que usa `Money`.
- *Extract Method*: `monthsBetween`, `applyLoyaltyDiscount`, `applyNonProfitDiscount`, `applyPromoCode`, `applyLateFees`, `charge` e `sendReceipt`.
- *Replace Magic Number with Symbolic Constant*: alíquotas, prazos e códigos de cupom viraram constantes nomeadas.
- *Consolidate Conditional Expression*: a checagem de cupom nulo ou vazio foi absorvida por `"CODIGO".equalsIgnoreCase(promoCode)`.
- *Replace Nested Conditional with Guard Clauses*: o orquestrador retorna cedo para cliente inexistente ou inativo.
- *Remove Parameter*: o campo `planType`, nunca usado, foi retirado de `BillingCommand`.
- *Remove Dead Code*: foram removidos as cópias locais dos campos do comando, as constantes e os imports sem uso e o `send()` comentado.
- *Substitute Algorithm*: a aritmética em `double` foi trocada por `Money`/`BigDecimal`, sem arredondamento.

**Testes**
- `PricingCalculatorTest` cobre faixas de fidelidade, ONG, cupons e as bordas de data com `Clock.fixed`: limites de mês, virada de ano, início e fim de novembro e fuso horário. Também cobre juros, valor negativo e arredondamento.
- `SubscriptionBillingServiceTest` cobre cobrança aprovada, cobrança recusada, cliente inexistente, cliente inativo, envio do recibo e propagação de falha. Ele usa `InMemoryCustomerRepository`, `FakePaymentGateway` e `InMemoryNotificationService`.

## 4. Preservação do comportamento e débitos remanescentes

**Comportamento preservado**
- **Mesmas regras de preço.** O cálculo de `PricingCalculator` foi comparado ao cálculo legado em `double` em 287.820 combinações de preço, data de adesão, data atual, cupom, atraso e indicador de ONG. A maior diferença encontrada foi de 2,3 × 10⁻¹³, que é apenas ruído de ponto flutuante binário: onde o legado obtinha `59.49999999999999`, `Money` obtém `59.5`. Nenhuma política de arredondamento foi introduzida, assim como no legado.
- **Mesma ordem de execução.** O preço continua sendo calculado antes da consulta ao cliente. Assim, uma data de adesão inválida ainda gera `DateTimeParseException` antes de qualquer acesso ao banco.
- **Mesmas mensagens de log.** Os textos e o formato dos valores são os mesmos, e cliente inexistente continua retornando uma lista vazia. O envio real do e-mail continua desativado, como no código legado.

**Mudança intencional**
- Falhas de infraestrutura deixam de virar a linha de log "Erro fatal no processamento" e passam a propagar como exceções específicas, conforme exigido na etapa de remoção do `catch (Exception e)`.

**Débitos remanescentes**
- As credenciais do banco continuam fixas em `JdbcCustomerRepository`. Agora estão isoladas, mas deveriam vir de configuração externa.
- A gravação de faturas está em `CustomerRepository` para manter as três interfaces pedidas. Um `InvoiceRepository` próprio deixaria a separação mais clara.

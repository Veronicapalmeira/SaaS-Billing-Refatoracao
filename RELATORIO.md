# Relatório Técnico de Refatoração — SaaS Billing

## 1. Matriz de diagnóstico de Code Smells e violações arquiteturais

| Trecho do código legado | Code Smell ou princípio violado | Impacto na manutenibilidade | Impacto na CI |
|---|---|---|---|
| Método `processMonthlyBilling`, responsável pelos cálculos, consulta ao banco, processamento do pagamento e preparação do e-mail. | **Long Method** | Concentra muitas etapas em um único método, dificultando a leitura e a alteração das regras. | Dificulta testar cada etapa isoladamente e exige dependências de infraestrutura para testar o fluxo completo. |
| Classe `SubscriptionBillingService`, que combina regras financeiras, acesso ao banco de dados, processamento de pagamento e preparação de e-mail. | **Violação do Single Responsibility Principle (SRP)** | Mudanças em responsabilidades diferentes afetam a mesma classe, aumentando o acoplamento e o risco de regressões. | Dificulta executar testes unitários independentes de banco de dados e serviço de e-mail. |
| Parâmetros e variáveis primitivas, como `double basePrice` e `int overdueDays`, usados no processamento da cobrança. | **Primitive Obsession** | Os tipos primitivos não encapsulam as regras específicas dos valores financeiros, favorecendo operações inconsistentes. | Dificulta testar operações monetárias de forma isolada e consistente. |
| Chamadas a `DriverManager.getConnection(...)`, comandos SQL e criação direta de `SimpleEmail`. | **Dependência direta de I/O e violação da Inversão de Dependência** | O serviço fica acoplado às tecnologias de banco de dados e envio de e-mail. | Os testes podem depender de infraestrutura externa, dificultando sua execução isolada na CI. |
| Chamada a `LocalDate.now()` dentro de `processMonthlyBilling`. | **Dependência implícita do relógio do sistema** | O resultado depende da data em que o método é executado, dificultando reproduzir cenários históricos. | Testes que dependem da data real podem apresentar resultados diferentes em momentos distintos. |
| Bloco `catch (Exception e)` que registra a mensagem do erro e continua o fluxo de retorno. | **Captura genérica de exceções e mascaramento de falhas** | Erros de naturezas diferentes recebem o mesmo tratamento, dificultando identificar e tratar suas causas adequadamente. | Falhas de infraestrutura podem ser transformadas em mensagens de log, sem que o teste identifique claramente a falha. |
| Parâmetro `String planType` da assinatura original de `processMonthlyBilling`, que não é utilizado no corpo do método. | **Parâmetro não utilizado / código desnecessário** | A assinatura sugere que o tipo de plano influencia a operação, mas o parâmetro não participa da lógica. | Não provoca necessariamente uma falha na CI, mas aumenta o ruído e dificulta compreender e testar a interface do método. |

## 2. Métricas antes da refatoração

Os valores abaixo referem-se ao código legado registrado no commit inicial do repositório.

| Métrica | Antes da refatoração |
|---|---|
| Linhas totais da classe original | 106 |
| Linhas do método `processMonthlyBilling` | 86 |
| Parâmetros do método original | 8 |
| Complexidade ciclomática | A medir com ferramenta apropriada |
| Integrações externas acopladas diretamente ao serviço | JDBC/banco de dados e SMTP/e-mail |
| Dependência direta da data do sistema | Presente, por meio de `LocalDate.now()` |
| Capturas genéricas de exceções | 1 bloco `catch (Exception e)` |

## 3. Métricas depois da refatoração

As métricas posteriores serão preenchidas após a conclusão das etapas de refatoração. As medições devem utilizar os mesmos critérios e, quando aplicável, a mesma ferramenta usada para avaliar o código original.

| Métrica | Antes | Depois |
|---|---|---|
| Linhas do método orquestrador | 86 | A medir |
| Complexidade ciclomática | A medir | A medir com a mesma ferramenta |
| Parâmetros do método principal | 8 | A medir |
| Dependências externas diretas do orquestrador | JDBC e SMTP | A verificar |
| Dependência direta de `LocalDate.now()` | Presente | A verificar |
| Capturas genéricas `catch (Exception e)` | 1 | A verificar |
| Testes unitários passando | A registrar com a execução inicial | A registrar ao final |

## 4. Observações sobre a implementação de `Money`

O código legado não possui uma política explícita de arredondamento monetário. Portanto, a implementação de `Money` não deve introduzir uma regra de arredondamento arbitrária. Qualquer política adicional deve ser definida e validada para evitar alterações indevidas no comportamento das regras de negócio.

A validação final deverá considerar a execução dos testes automatizados e a comparação das métricas antes e depois da refatoração.
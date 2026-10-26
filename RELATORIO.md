# Relatório de Refatoração

## 1. Diagnóstico de Code Smells

### 1.1 Long Method

**Trecho:** método `processMonthlyBilling`, na classe `SubscriptionBillingService`.

**Problema:** o método concentra cálculos de preço, consulta ao banco de dados, processamento do pagamento e preparação do e-mail.

**Impacto na manutenção:** dificulta a leitura, a compreensão e a alteração das regras de cobrança.

**Impacto na CI:** dificulta testar cada etapa isoladamente, especialmente porque o método depende de acesso ao banco de dados.

### 1.2 Violação do princípio de responsabilidade única (SRP)

**Trecho:** classe `SubscriptionBillingService`.

**Problema:** a classe reúne regras de precificação e operações de acesso ao banco de dados, processamento do pagamento e preparação do e-mail.

**Impacto na manutenção:** alterações em responsabilidades diferentes podem exigir mudanças na mesma classe, aumentando o acoplamento e o risco de regressões.

**Impacto na CI:** as dependências de infraestrutura dificultam a execução de testes isolados, sem serviços externos.

### 1.3 Parâmetro não utilizado

**Trecho:** parâmetro `String planType` na assinatura do método `processMonthlyBilling`.

**Problema:** o parâmetro é recebido, mas não é utilizado no corpo do método.

**Impacto na manutenção:** torna a assinatura do método menos clara e pode gerar dúvidas sobre a finalidade do parâmetro.

**Impacto na CI:** não causa necessariamente uma falha de teste, mas representa código desnecessário e pode dificultar a compreensão e a manutenção do método.
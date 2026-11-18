# Protocolo experimental — mes-boot35-v1

Preparação: 07/11/2026. Este protocolo cobre apenas a primeira etapa, **Spring Boot 2.7.18 → 3.5.16**. O experimento compara as configurações do Codex e do Ark na mesma aplicação. O alvo é o último patch OSS da linha 3.5, [publicado pelo Spring](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/), com [Java 17 e Maven 3.6.3 ou superior](https://docs.spring.io/spring-boot/3.5/system-requirements.html). A escolha é para o TP, cuja segunda etapa prevê a linha 4.0.

## Condições e limites

| Item | Regra |
| --- | --- |
| Fonte | Commit consolidado 1e8ea40939d92023df8dab81b64506c8f83a9ca6 (Boot 2.7.18). Uma nova extração do mesmo pacote por tentativa. |
| Primeira condição | Codex desktop, modelo gpt-6.1-sol, esforço Alto (high), em tarefa nova sem o histórico de construção. Registrar versão do cliente e opção efetivamente exibida. |
| Segunda condição | Ark, revisão/configuração a fixar antes da tentativa. Buscar o mesmo modelo e esforço e registrar o que a versão escolhida efetivamente suporta. Adaptador de execução dos testes, se necessário, documentado como infraestrutura, sem mudar o conteúdo de negócio ou mostrar o avaliador externo. |
| Ambiente | Windows, mesmo JDK 17, Maven Wrapper 3.9.11 e PostgreSQL 16.10 fixado por digest. Usar o mesmo ambiente nas duas condições; registrar Docker, Compose e eventual diferença. |
| Entrada e ferramentas | Mesmo prompt.txt e os mesmos arquivos de aplicação/testes. Inspeção, edição e testes locais permitidos; rede só para dependências e imagens de build/testes. Sem busca de soluções externas, subagentes ou acesso ao histórico/solução da outra condição. |
| Repetições | Uma tentativa por condição na v1. Estudo de caso exploratório; não estimar superioridade geral. Qualquer ampliação deve ser registrada antes das novas tentativas. |
| Limite comum | Até 45 minutos de tempo de parede por tentativa, a partir do envio do prompt até a entrega ou o encerramento; inclui aprovações e builds do agente. Preparação e avaliação externa são medidas à parte. O operador controla o relógio. Limites próprios do cliente/agente podem encerrar antes e devem ser registrados. |
| Assistência humana | Operador fornece o pacote, inicia a tentativa e autoriza ações/edits conforme a interface; pode comunicar a indisponibilidade do ambiente. Não dá pistas de migração, não escolhe correções técnicas nem edita código durante a tentativa. Registrar aprovações, negações e intervenções. Uma correção humana posterior é uma execução assistida, com evidência separada. |
| Uso e custo | Registrar tokens e custos quando observáveis; usar “não disponível” se a interface não informar. Não estimar tokens a partir do tempo nem impor um teto de tokens que não possa ser aplicado nas duas condições. |

Fixar estas regras e o SHA256 do pacote antes de enviar o prompt. Se as condições forem mudadas durante ou após a primeira tentativa (por exemplo, dicas novas, testes enfraquecidos ou orçamento ampliado), registrar a rodada como piloto e comparar sob uma nova versão comum do protocolo. Não usar o resultado da primeira condição para orientar o agente da segunda.

## Pacote de entrada

O gerador [New-MigrationInput.ps1](../experiments/New-MigrationInput.ps1) exporta uma lista explícita de arquivos versionados a partir do commit fixado. Inclui src/ com testes e SQL, POM, Wrapper, Compose e .env.example. Acrescenta README.md, AGENTS.md e [prompt.txt](../experiments/input/prompt.txt) específicos da tarefa. O contrato público é extraído da especificação da baseline, preservando as regras e retirando o histórico de construção e a configuração da avaliação externa.

O ZIP contém somente o material do agente. O manifesto JSON, o protocolo, evaluation/, o histórico Git, .env real, logs e resultados ficam com o operador, fora do ZIP. O manifesto registra commit de origem, SHA256 do ZIP e hashes por arquivo. A saída padrão é .experiment-local/input/; o gerador recusa sobrescrever uma saída existente. Execute-o na raiz em PowerShell 7:

~~~powershell
.\experiments\New-MigrationInput.ps1
~~~

Se for necessário gerar novamente, escolha outro diretório com -OutputDirectory. Para a comparação, reutilize o ZIP validado, em vez de montar outra entrada com arquivos do diretório de trabalho.

Pacote validado em 07/11/2026: 71 arquivos; SHA256 do ZIP 77a806f21c689fb493b0ae85d02f60c4c7af549c0a888fd7c93a07e8b0b1ebcf. O SHA256 do prompt.txt extraído é 278c5cb64daeb607d06136db420f58a597f69dd33608ee59c67df6304c70e807.

## Primeira tentativa

1. Revise este protocolo e registre o SHA256 do ZIP. Confirme os pré-requisitos e a validação anotada em [STATUS.md](STATUS.md). Configure o ambiente antes de iniciar o relógio.
2. Extraia o ZIP em uma **pasta nova, fora deste repositório**. Abra só essa pasta como projeto de uma nova tarefa no Codex desktop. A cópia usada para validar a baseline já contém resultados de build; não a reutilize como entrada.
3. No seletor do cliente, confirme **GPT‑6.1 Sol / Alto**, registre a opção selecionada e envie o conteúdo integral de prompt.txt da pasta extraída. Acompanhe o limite de 45 minutos e guarde o registro da tarefa. O AGENTS.md do pacote autoriza só esta migração; o AGENTS.md da baseline original permanece como está.
4. Ao encerrar, preserve a saída, a transcrição e os logs mesmo em caso de falha. Compare a árvore entregue com uma extração nova do ZIP. A comparação deve incluir arquivos adicionados ou removidos, sem misturar target/ e logs ao código. Registre o tempo de migração antes da avaliação externa.

## Avaliação independente

- Confira o alvo real, o JAR gerado, as alterações no código e se as verificações de negócio da suíte interna foram preservadas. Sucesso técnico exige build e testes internos sem falhas ou verificações desativadas.
- A partir do repositório do operador, confira o SHA256 de evaluation/SNAPSHOT.sha256 e os nove insumos do snapshot. O snapshot da versão 1.0.0 é d878ffcab48bc0067438a8d8c423fff25496b5c5094621192e470a5db9c1589b. Execute o avaliador congelado contra o **JAR da tentativa**, com JDK 17:

~~~powershell
.\evaluation\run.ps1 -ApplicationJar 'C:\caminho\da\tentativa\target\mes-legacy-shop-0.0.1-SNAPSHOT.jar'
~~~

- O avaliador inicia a aplicação em PostgreSQL descartável e executa 26 verificações HTTP. Preserve os logs da aplicação, do banco e do avaliador, os relatórios XML e a identidade do JAR. A avaliação passa somente com 26 testes executados, sem falhas, erros ou skips.
- Em run.json, sourceCommit é o commit do checkout do **avaliador**. A origem da saída migrada deve ser registrada à parte, com o SHA256 do pacote de entrada, o diff/árvore final e o SHA256 do JAR efetivamente avaliado. Não confundir os dois.
- Classifique o resultado como sucesso sob as regras, falha de build/testes/contrato, encerramento por limite ou bloqueio de infraestrutura. Um bloqueio de ambiente pode justificar nova tentativa com o mesmo protocolo, preservando a evidência anterior. Não descarte falhas de migração. Uma segunda etapa para Boot 4 requer um intermediário validado e um protocolo próprio.

### Registro mínimo por tentativa

| Campo | Registrar |
| --- | --- |
| Identidade | ID da tentativa; data/hora inicial e final com fuso; condição; versão do cliente/Ark; modelo e esforço; protocolo; SHA256 do ZIP e do prompt. |
| Ambiente | JDK, Maven, sistema, Docker, Compose e imagem PostgreSQL. |
| Processo | Tempo de parede, comandos, falhas, aprovações e intervenções; transcrição/exportação se disponível; tokens/custo observáveis. |
| Saída e avaliação | Diff/árvore final, versão Boot efetiva, SHA256 do JAR, resultados internos e externos e localização das evidências. |
| Resultado | Classificação, limite atingido, pendências e eventual assistência técnica em rodada separada. |

Mantenha o registro fora da pasta fornecida ao agente. Os resultados dessa preparação não são uma migração: a validação do pacote ainda executa a baseline Boot 2.7.18.

## Emenda v2 — 09/11/2026 (registrada antes da nova tentativa)

**Motivo.** As duas tentativas da condição Codex + GPT‑6.1 Sol (Alto) foram classificadas como **bloqueio de infraestrutura**. Na codex-01, a saída foi validada depois (123 internos e 26 externos, sem correções), mas a tentativa não concluiu os testes dentro do relógio. A codex-02 foi interrompida pelo autor. Os logs do Docker Desktop 4.38 mostram a causa. A economia de recursos (Resource Saver) pausou a VM do motor 5 minutos após o último contêiner, às 17:05:31 (−03:00) de 09/11. A VM não retomou, e o proxy passou a responder no route to host e HTTP 500. As evidências ficam em .experiment-local/records/.

**Mudança de ambiente (vale para todas as tentativas seguintes).** Resource Saver desligado no Docker Desktop, conferido antes do envio do prompt, com o motor ainda respondendo após mais de 5 minutos sem contêineres. As demais versões do ambiente permanecem e são registradas por tentativa. A pasta da tentativa fica fora do OneDrive, para evitar sincronização de target/.

**Nova condição.** Fica acrescentada a condição **Claude Code desktop (aba Code) + claude-opus-5-5, esforço Médio (medium)**, em sessão nova sem o histórico de construção. O modo de permissão é auto: o agente executa comandos sem aprovação manual. Valem o mesmo ZIP (SHA256 77a806f2…ebcf), o mesmo prompt.txt sem alterações, o limite de 45 minutos, o avaliador congelado e as regras de assistência humana. A instrução sem outros agentes do prompt.txt abrange os subagentes do cliente.

**Operador.** A sessão é criada e acompanhada por outra sessão do Claude Code, que conhece o histórico do projeto e mantém evaluation/. Essa sessão apenas envia o prompt.txt como mensagem inicial. A ferramenta de criação de sessão exige um campo de contexto, preenchido somente com a identificação neutra da tentativa, sem pistas de migração. A sessão operadora não interage com o agente durante a tentativa, salvo para comunicar indisponibilidade do ambiente, registrada como intervenção.

**Status das condições.** As tentativas do Codex permanecem como evidência e não são descartadas. Uma nova tentativa Codex sob o ambiente corrigido é opcional e, se feita, segue esta emenda. Na condição Ark, o modelo e o esforço serão fixados antes da tentativa. Preferencialmente, devem ser os mesmos desta condição; caso contrário, a diferença é registrada. Continua sendo um estudo de caso exploratório, com uma tentativa válida por condição.

## Emenda v3 — 10/11/2026 (registrada antes da tentativa codex-03)

**Esforço da condição Codex.** A próxima tentativa da condição Codex usa **GPT‑6.1 Sol, esforço Médio (medium)**, em vez de Alto. O objetivo é alinhar o nível nominal de esforço com a condição Claude Code + Opus 5.5 (Médio). Os níveis de esforço não são calibrados entre fornecedores: "Médio" igual nos dois clientes não garante orçamento de raciocínio equivalente. A comparação continua sendo entre configurações completas (cliente + modelo + esforço).

**Configuração antes do envio.** A permissão de Acesso total da tarefa Codex é definida antes do envio do prompt e registrada como configuração, como o modo auto na condição Claude Code. Na codex-02, ela foi ajustada durante a tentativa e registrada como intervenção.

**Status.** As tentativas codex-01 e codex-02 (GPT‑6.1 Sol, Alto) continuam como evidência de bloqueio de infraestrutura. A codex-03 segue as emendas v2 e v3, com mesmo ZIP, prompt.txt, limite de 45 minutos, avaliador congelado e regras de assistência.

# Protocolo experimental — mes-boot35-v2

Preparação: 15/11/2026. **Situação: baseline e pacote fixados em 15/11/2026; nenhuma tentativa foi iniciada.** O protocolo v1 e suas emendas ficam como registro do estudo piloto (tentativas claude-01 e codex-03, origem Java 17 / Boot 2.7.18). O v2 usa a origem reconstruída (D1 de [DECISOES.md](DECISOES.md)) e mantém as regras do v1 e das emendas v2 e v3, salvo o que está abaixo.

## Origem, destino e condições

| Item | Regra |
| --- | --- |
| Origem | Java 8 e Spring Boot 2.1.18.RELEASE, [caracterizada](CARACTERIZACAO_ORIGEM.md) antes do experimento. Commit da baseline: `32537ebe5fa7301b75fe30e2b86afa43abb5c2c7`, fixado pelo autor em 15/11/2026 depois da revisão do R7. |
| Destino | Java 17 e Spring Boot 3.5.16, ainda o último patch da linha 3.5 no Maven Central em 15/11/2026. Testes internos em JUnit 5 (Jupiter), sem o motor vintage do JUnit 4, como requisito explícito do prompt. |
| Condição C1 | Claude Code, modelo claude-opus-5-5, esforço médio, modo de permissão auto, em sessão nova sem o histórico de construção. |
| Condição C2 | Codex desktop, GPT‑6.1 Sol, esforço Médio, com Acesso total configurado antes do envio do prompt, em tarefa nova. |
| Fora do v2 | Ark e etapa Boot 4. |
| Repetições | Uma tentativa por condição. Estudo de caso exploratório. A ordem das condições é registrada; o resultado de uma não orienta a outra. |
| Ambiente | Windows; Docker Desktop 4.38 com Resource Saver desligado; PostgreSQL 16.10 pelo digest fixado; JDK 17 em JAVA_HOME no ambiente do agente; pastas de tentativa fora do OneDrive. Registrar JDK, Maven, Docker e Compose. |
| Limite, assistência e custo | Iguais ao v1: 45 minutos de tempo de parede, operador sem pistas ou correções técnicas, tokens e custos quando observáveis. |

## Pacote de entrada

Gerado por [New-MigrationInput.ps1](../experiments/New-MigrationInput.ps1) a partir do commit da baseline, em Windows PowerShell 5.1 ou PowerShell 7:

~~~powershell
.\experiments\New-MigrationInput.ps1 -Baseline <commit-completo-da-baseline>
~~~

O pacote contém src/ (código, testes e SQL), POM, Wrapper, Compose, .env.example, o contrato público extraído de docs/ESPECIFICACAO.md e os arquivos da tarefa em [experiments/input/](../experiments/input/) (AGENTS.md, README.md e prompt.txt). Não contém evaluation/, tools/, documentos de caracterização, histórico Git nem resultados do piloto. **Pacote definitivo (15/11/2026):** `mes-legacy-shop-boot-2.1-to-3.5.16-input.zip`, gerado a partir do commit 32537eb, com 72 arquivos e nenhum dos itens excluídos. O manifesto fica com o operador em `.experiment-local/input-v2/`.

| Arquivo | SHA256 |
| --- | --- |
| ZIP do pacote | `07804f235afc5ec4ca0993e26eced9ca5ff0ceefa3699a4b7311bf9fc4f8e4e5` |
| prompt.txt | `61f0fbf02e4c6423e37a0ba618861f5218b4997d69945d987adec610ddbd5f13` |
| AGENTS.md | `53dbfe89b4f247aa817e07928d383d72268acc83cf34144dbe5b3990e1e7bb5f` |
| README.md | `38433739ef8051aef064c51335ab3b07f35bc4f6523ce1b068d2b4144467df25` |
| docs/ESPECIFICACAO.md | `9f695eed8089f996e538c6e3417868b68e00f9993d166a2dd1c849effd8351f2` |

Antes de cada tentativa, confira o SHA256 do ZIP com `Get-FileHash`. Se ele não bater, não use o pacote.

## Avaliação de cada tentativa

| Verificação | Critério |
| --- | --- |
| Build interno | `clean verify` na saída do agente e em cópia independente, com JDK 17: sem falhas, erros ou skips. |
| Inventário da suíte | `java tools/TestInventory.java` comparado com a origem: 39 métodos, 123 casos executados, 230 pontos de asserção, nenhum skip. Cada diferença é classificada; remoção de cenário ou de asserção é falha. |
| Varredura de legado | `java tools/LegacyScan.java <saída>`: os marcadores de quebra (15 tipos) devem zerar; os de obsolescência (4 tipos) são registrados como grau de modernização. JUnit 4 remanescente descumpre o prompt. |
| Avaliador externo 1.1.0 | 26/26 com a aplicação em JDK 17. A origem é avaliada com `APPLICATION_JAVA_HOME` no JDK 8 como referência. |
| Roteiro de sondagem | `experiments/supplementary/probe-behaviors.sh` com a aplicação em JDK 17, comparado com a origem em JDK 8. A barra final continua fora do contrato e é registrada como análise complementar. |

Classificação: sucesso sob as regras; falha de build/testes/contrato; falha de requisito do prompt (por exemplo, JUnit 4 remanescente); encerramento por limite; ou bloqueio de infraestrutura. O registro mínimo por tentativa é o do v1, acrescido do inventário e da varredura.

## Snapshot do avaliador

Versão 1.1.0: a aplicação pode rodar em JDK diferente do avaliador (`APPLICATION_JAVA_HOME` ou `-ApplicationJavaHome`), e as duas versões de Java ficam registradas. Os testes HTTP, a fixture e o compose não mudaram. As ressalvas de atribuição foram retiradas de docs/ESPECIFICACAO.md, sem mudança de contrato. O novo SHA256 de evaluation/SNAPSHOT.sha256 está em [STATUS.md](STATUS.md).

## Emenda v2.1 — 18/11/2026 (registrada antes da tentativa C3)

**Nova condição.** Fica acrescentada a condição **C3: Ark** (github.com/mtov/ark, commit `04b3eb1`), que deixa de estar fora do v2 nesta etapa. Configuração fixada antes da tentativa: modelo `anthropic/claude-opus-5.5` via OpenRouter (API compatível com a da OpenAI), sem nível de esforço enviado, o que corresponde ao padrão medium da API da Anthropic; `max_iterations` 150; `test_timeout_seconds` 900; `timeout_seconds` 600.

**Adaptador de testes (infraestrutura).** O comando fixo do Ark, `./mvnw test`, é trocado por `./mvnw -B -ntp clean verify`, porque a suíte interna roda no Failsafe (fase `verify`). O adaptador não muda conteúdo de negócio nem testes e não expõe o avaliador externo ao agente.

**Ambiente.** O Ark exige shell POSIX, então a tentativa roda no WSL2 Ubuntu, com OpenJDK 17 em JAVA_HOME, Docker Desktop (motor 27.5.1, Compose v2.32.4) com integração WSL e Resource Saver desligado, e pasta da tentativa no sistema de arquivos do WSL, fora do OneDrive. A diferença em relação ao ambiente Windows das condições C1 e C2 é registrada.

**Operação.** O Ark pede aprovação para cada edição. O operador aprova todas, sem pistas, e registra o número de aprovações. Valem o mesmo ZIP (`07804f23…e4e5`), o mesmo prompt.txt sem alterações, o limite de 45 minutos, a avaliação descrita acima e as regras de assistência humana. A ordem das condições é registrada.

Nesta tentativa, a condição C1 também roda no WSL2 Ubuntu, com o mesmo ambiente do Ark (Claude Code 2.1.327 CLI, plano Claude Pro).
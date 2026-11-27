# Clínica Viva — TP2 de Programação Orientada a Objetos

Aplicação desktop Java Swing para a **Opção A: Sistema de Gestão de Clínica Médica**. Permite cadastrar, editar, pesquisar e excluir profissionais e pacientes; agendar atendimentos e acompanhar pagamentos mensais. Os dados são gravados em CSV e restaurados na abertura.

Projeto preparado com assistência de IA a partir do enunciado do TP2 CEUB 2026/2. As decisões adicionais de negócio estão descritas abaixo para facilitar a compreensão e apresentação do código.

## Executar

Para compilar os fontes e executar testes: **JDK 17 ou superior**, com `java`, `javac` e `jar` no PATH ou `JAVA_HOME`. Não precisa de Maven, banco externo ou bibliotecas de terceiros.

O **ZIP de entrega para Windows x64** inclui um runtime Java portátil em `runtime/` e o JAR em `build/`; basta extrair e abrir `iniciar.bat`, sem instalar Java. Esse runtime e o JAR são artefatos de distribuição e não são versionados no GitHub. O clone do repositório requer o JDK para compilar.

No Windows, extraia o ZIP para uma pasta gravável e abra **iniciar.bat**. Para executar os testes, abra **testar.bat**.

Também é possível usar PowerShell:

```powershell
.\build.ps1 -Executar
.\build.ps1 -Testar
```

Se a política local bloquear scripts, os arquivos `.bat` executam o script com política apenas para o processo. Alternativa explícita:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -Testar
```

No Linux/macOS, com sessão gráfica disponível para a aplicação:

```sh
bash build.sh test
bash build.sh run
```

O JAR compilado fica em `build/clinica-viva.jar`. Para executá-lo diretamente, mantenha o diretório do projeto como pasta atual:

```sh
java -jar build/clinica-viva.jar
```

Para escolher outra pasta de dados:

```sh
java -Dclinica.dados="caminho/para/dados" -jar build/clinica-viva.jar
```

## Fluxo de uso

1. Em **Profissionais**, cadastre um médico com nome, CRM e UF, especialidade, valor/hora, horas mensais e taxa mensal de cirurgia. Ou escolha enfermeiro e informe COREN, turno e adicional noturno mensal.
2. Em **Pacientes**, informe nome, CPF válido e telefone com DDD.
3. Em **Agenda**, selecione os cadastros, data, horário e duração. Os intervalos ocupados do profissional e do paciente são validados.
4. Use **Editar / status** para reagendar, concluir ou cancelar. O cancelamento libera o horário e preserva a consulta no histórico.
5. Em **Profissionais → Ver ficha**, visualize os componentes do pagamento. A visão geral apresenta o total da folha.
6. Feche e reabra pelo mesmo inicializador: os registros serão recuperados automaticamente.

As tabelas possuem busca em todas as colunas e ordenação ao clicar no cabeçalho. Selecione uma linha antes de editar ou excluir. Uma pessoa só pode ser excluída depois da remoção de todas as consultas vinculadas, inclusive canceladas.

## Regras de negócio adotadas

O edital define os atributos e os métodos abstratos, mas não fixa fórmulas nem carga horária. Este projeto adota as seguintes regras didáticas:

| Categoria | Pagamento mensal |
| --- | --- |
| Médico | valorHora × horasMensais + taxaCirurgia |
| Enfermeiro | valorHora × horasMensais + adicionalNoturno |

`taxaCirurgia` representa o **total fixo mensal** de cirurgias; `adicionalNoturno` representa um **valor fixo mensal em reais**. No turno diurno, o adicional deve ser zero. Não há incidência por consulta, impostos, descontos ou cálculo trabalhista. Exemplo: médico com R$ 100/h, 160 horas e R$ 500 de taxa recebe R$ 16.500. Enfermeiro com R$ 50/h, 160 horas e R$ 400 de adicional recebe R$ 8.400.

Valores monetários usam `BigDecimal`, de zero a R$ 10.000.000,00 por campo, com até duas casas decimais. A entrada aceita vírgula ou ponto decimal sem separador de milhar. Horas mensais: 1 a 744. Consultas: 10 a 240 minutos, precisão de minutos, intervalos adjacentes permitidos. Consultas concluídas continuam ocupando o intervalo no histórico; canceladas não ocupam. Datas passadas são permitidas para registro histórico. A agenda não presume restrições de expediente ou especialidade.

CPF é validado pelos dígitos verificadores e deve ser único. Telefone: 10 a 13 dígitos. Registro profissional é único dentro da categoria; informe a UF junto ao número. Textos são obrigatórios, com até 160 caracteres e sem quebras de linha.

## Arquitetura e POO

```text
Main → View → Controller → Service → Repository → CSV
                           ↓
                         Model
```

| Pacote | Responsabilidade |
| --- | --- |
| `model` | Entidades imutáveis, validações de atributos e cálculos |
| `view` | Telas Swing, listeners, confirmação e alertas |
| `controller` | Conversão das entradas textuais e delegação ao serviço |
| `service` | Unicidade, associações, conflitos, folha e coordenação da gravação |
| `repository` | Contrato de persistência e implementação CSV UTF-8 |

`Profissional` é abstrata e declara `calcularPagamentoMensal()` e `exibirFicha()`. `Medico` e `Enfermeiro` usam `super()` e sobrescrevem os dois métodos. A folha percorre `List<Profissional>` e chama o método de cada subtipo, demonstrando polimorfismo. Os atributos das entidades são privados e finais, com getters; edições criam um novo objeto com o mesmo UUID, evitando mudanças que contornem a persistência. `Consulta` associa paciente e profissional por UUID estável. Não existe `Scanner` ou menu de terminal.

## Persistência e recuperação

O arquivo `data/clinica.csv` vem vazio, apenas com cabeçalho. Nenhum paciente real está incluído. O cabeçalho `CLINICA_TP2;1` identifica a versão do formato. As demais linhas têm campos entre aspas, separados por ponto e vírgula; aspas internas são duplicadas. Datas são gravadas em ISO e decimais usam ponto.

```text
MEDICO;id;nome;registro;valorHora;horasMensais;especialidade;taxaCirurgia
ENFERMEIRO;id;nome;registro;valorHora;horasMensais;turno;adicionalNoturno
PACIENTE;id;nome;cpf;telefone
CONSULTA;id;pacienteId;profissionalId;inicio;duracaoMinutos;status
```

O esquema acima descreve a ordem dos campos; o arquivo efetivo inclui aspas em todos os campos de dados.

Cada mudança monta um snapshot, valida todas as relações, grava um temporário na mesma pasta e publica o arquivo por renomeação atômica. **Somente depois da gravação a memória é atualizada.** Falhas de disco são exibidas em diálogo. Sistemas de arquivos sem suporte à renomeação atômica retornam erro e preservam a versão anterior. Uma trava `clinica.lock` impede abertura simultânea da mesma pasta por duas instâncias da aplicação.

Arquivo com cabeçalho, linha ou referência inválida impede a abertura e apresenta um alerta, sem reinicializar ou sobrescrever dados. Para recuperação, feche o sistema e restaure uma cópia de `clinica.csv`. Faça backup do CSV com a aplicação fechada. Não existe sincronização em rede ou backup automático; a atomicidade protege a substituição do arquivo, sem prometer recuperação de falhas físicas do dispositivo.

## Verificação

`test/br/edu/ceub/clinica/ClinicaTest.java` executa verificações de cálculo, folha polimórfica, CPF, duplicidade, parsing, conflitos de ambos os participantes, intervalos adjacentes, cancelamento, edição, exclusão, recuperação dos subtipos e vínculos, escape CSV e falhas de disco simuladas e reais. Os testes usam somente uma pasta temporária isolada. O workflow do GitHub compila e testa com JDK 17.

Roteiro visual e explicação para apresentação: `docs/ROTEIRO.md`.

## Entrega

O prazo informado é **30/11/2026, às 23h59 (Brasília)**. O PDF pede envio de ZIP completo pelo AVA; a mensagem da atividade também permite link público do GitHub. O pacote ZIP acompanha o projeto, os fontes, o CSV vazio e um JAR compilado. A submissão no AVA é feita pelo estudante.

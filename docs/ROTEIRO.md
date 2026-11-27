# Roteiro de demonstração e estudo

## Demonstração em aproximadamente cinco minutos

1. Abra `iniciar.bat`. Mostre a visão geral e as quatro abas.
2. Cadastre um médico: nome `Profissional de demonstração`, registro `CRM-DF DEMO-1`, valor/hora `100`, horas `160`, especialidade `Clínica geral` e taxa mensal `500`. Abra a ficha: pagamento esperado **R$ 16.500,00**.
3. Cadastre um enfermeiro noturno com registro `COREN-DF DEMO-2`, R$ 50/h, 160 horas e R$ 400 de adicional. Folha total esperada: **R$ 24.900,00**.
4. Cadastre um paciente de demonstração com CPF de teste `529.982.247-25` e telefone fictício `61999990000`. Esses dados servem apenas para teste.
5. Agende uma consulta às 09:00, por 30 minutos. Tente outra às 09:15 com o mesmo paciente: o alerta deve explicar o conflito.
6. Cancele a consulta original e repita o agendamento: agora será permitido.
7. Tente valor/hora `abc`, data `31/02/2026` e CPF `00000000000`: os alertas devem manter o formulário aberto para correção.
8. Feche e reabra. Mostre que profissionais, paciente, consultas e seus estados continuam disponíveis.
9. Execute `testar.bat` e examine a mensagem final das verificações.

## Onde explicar cada conceito

- **Abstração:** `model/Profissional.java` define o contrato e os atributos comuns.
- **Herança:** os construtores de `Medico` e `Enfermeiro` delegam a `super()`.
- **Encapsulamento:** atributos privados e finais; alterações passam pelo serviço e pela gravação.
- **Polimorfismo:** `ClinicaService.calcularFolhaMensal()` chama `calcularPagamentoMensal()` sobre `List<Profissional>`. A ficha também é resolvida pelo subtipo.
- **Associação:** `Consulta` mantém o UUID de um paciente e de um profissional.
- **MVC:** a View recolhe os textos; o Controller os converte; o Service coordena validações e persistência; o Model calcula.
- **Listener:** em `ClinicaView.salvarDialogo()`, o botão chama a ação do Controller; não calcula pagamento.
- **Persistência:** `RepositorioCsv.carregar()` reconstrói objetos e `salvar()` escreve no disco. A memória muda somente após o sucesso do arquivo.
- **Exceções:** `ClinicaView.botao()` captura erros de entrada e I/O; `Main` trata falhas de abertura; o Controller captura erros de data e de inteiros.

## Checklist visual

- Criar, editar, filtrar, ordenar e excluir um cadastro sem vínculo.
- Editar uma pessoa com consulta: o UUID permanece e a agenda mostra o nome atualizado.
- Tentar excluir pessoa vinculada: deve aparecer uma orientação visual.
- Selecionar uma linha depois de ordenar ou pesquisar e conferir que edita a pessoa correta.
- Abrir uma segunda instância na mesma pasta: deve aparecer alerta de sistema já aberto.
- Com o sistema fechado, fazer backup do CSV e testar uma cópia corrompida em pasta isolada: abertura deve falhar sem reescrever o conteúdo.

## Perguntas para revisar

Por que `BigDecimal` é usado para dinheiro? Por que uma superclasse abstrata não pode ser instanciada? Qual a diferença entre sobrescrita e sobrecarga? Como o UUID preserva vínculos durante uma edição? Por que salvar depois de alterar a memória pode deixar a aplicação inconsistente? Como a View fica independente do formato CSV? O que aconteceria se duas instâncias escrevessem o arquivo ao mesmo tempo?

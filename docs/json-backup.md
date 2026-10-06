# Importação e exportação de lançamentos

Acesse **Menu → Configurações → Backup**. A operação inclui todos os
anos, independentemente dos filtros ativos. O seletor de documentos do Android
permite escolher o arquivo/destino sem permissão ampla de armazenamento.

## Formato versão 1

JSON UTF-8 com indentação, ordenado por `yearMonth`, `occurredAtMillis` e `id`, em
ordem crescente. Valores monetários são inteiros em centavos, nunca ponto flutuante.
Datas preservam o instante em milissegundos e o mês contábil armazenado, sem
recalculá-lo no fuso horário do aparelho que importa.

```json
{
  "format": "controle-de-gastos",
  "version": 1,
  "amountUnit": "cents",
  "transactions": [
    {
      "id": 1,
      "yearMonth": 202409,
      "occurredAtMillis": 1726444800000,
      "description": "Salário",
      "observation": "",
      "io": "Entrada",
      "paymentMethod": "Pix",
      "amountCents": 250000,
      "category": "Salário"
    }
  ]
}
```

`io` é `Entrada` (valor não negativo) ou `Saída` (valor não positivo). Todos os
campos são obrigatórios, exceto `id`, que é apenas informativo. Os IDs são gerados
localmente na importação. Versões desconhecidas, campos desconhecidos/repetidos,
campos ausentes, números fracionários, meses inválidos e JSON truncado são rejeitados.

## Mesclagem e duplicatas

A importação não apaga nem atualiza registros existentes. Compara todos os campos,
exceto o ID, e adiciona apenas ocorrências que faltam. Se o arquivo contém dois
lançamentos idênticos e o banco contém um, adiciona um. Reimportar o mesmo arquivo
não adiciona novas ocorrências. Se um lançamento foi editado depois da exportação,
a versão antiga é considerada um lançamento diferente; não há sincronização por ID.

O arquivo inteiro é validado antes de gravar. Leitura dos registros existentes e
inserção em lotes de 500 ocorrem na mesma transação Room: uma falha desfaz todos os
lotes. Não há migração de esquema nem alteração das regras dos lançamentos.

## Execução e limites

`BackupActivity` renderiza o estado e abre os seletores. `BackupViewModel` controla
uma operação por vez em `viewModelScope`. `BackupRepository` abstrai a operação;
`JsonBackupRepository` coordena Room, `BackupJsonCodec` e `BackupDocumentStore`.
Repositório e dispatcher de IO são injetados por Hilt. Arquivos, parsing e preparação
dos lotes executam fora da main thread. A coleta usa `repeatOnLifecycle` e nenhum
componente retém Activity/Views fora da camada de UI.

A operação continua durante mudanças de configuração e é cancelada quando o
ViewModel é destruído. Não é um trabalho persistente após encerrar o processo.
Streams usam `use`, loops verificam cancelamento e arquivos temporários são
removidos em `finally`. A exportação é preparada no cache privado antes de escrever
no documento selecionado; falhas no provedor durante a cópia podem deixar um arquivo
parcial no destino, informadas na tela.

Limites: 100.000 registros, 50 MiB de JSON e 100.000 caracteres por campo textual.
Esses limites se aplicam à exportação e importação. O parsing é sequencial, sem uma
árvore JSON completa em memória, mas mantém os registros validados para a transação.
O arquivo é texto simples e contém os detalhes dos lançamentos.

## Validação

`BackupRepositoryTest` usa banco em memória e arquivos temporários: ordenação de
vários anos, precisão acima de 2^53, Unicode, duplicatas legítimas, reimportação,
IDs conflitantes, arquivos inválidos, cancelamento, arquivo vazio e rollback entre
lotes. `UiRedesignTest` também cobre abertura e recriação da tela de backup.
`BackupViewModelTest` verifica exclusão de operações concorrentes e cancelamento
quando o ViewModel é destruído.

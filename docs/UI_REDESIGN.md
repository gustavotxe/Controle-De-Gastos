# Renovação da interface

Todas as telas continuam em XML com ViewBinding e Material Components. A paleta preserva os azuis do aplicativo; verde e vermelho continuam indicando entradas e saídas. Os recursos compartilhados ficam em `res/values/styles.xml`, `colors.xml` e `ui_strings.xml`.

## Apresentação

- Tela inicial com saldo destacado, menu de filtros, estado vazio e ação de novo lançamento.
- Cabeçalhos recolhíveis para liberar espaço durante a rolagem.
- Cartões com hierarquia de informação, observações vazias ocultas e ações de editar/excluir com áreas de toque de 48 dp.
- Formulário rolável, teclado decimal, seletores identificados, calendário e títulos diferentes para criação/edição.
- Resumos mensais compactos e gráfico anual com altura específica para paisagem.
- Ajuda revisada para refletir o seletor de ano existente.
- Paleta clara consistente também quando o sistema está em modo escuro, mantendo o comportamento anterior.

## Desempenho e arquitetura

`MyAdapter` e `MyAdapterMonth` usam `ListAdapter`: o cálculo das diferenças entre listas ocorre fora da main thread. As animações de itens são curtas e respeitam a desativação de animações nas configurações do sistema. Os gráficos mensais não reiniciam animações durante o reaproveitamento de células. Não foram adicionados timers, processos de fundo, dependências ou operações de banco na UI.

ViewModels, repositórios, banco, regras de cálculo e filtros foram preservados. O teste de inserção existente passou a aguardar o Job retornado pelo ViewModel, corrigindo uma corrida entre o teste e Dispatchers.IO.

## Verificação

- APK de debug e APK dos testes compilados.
- Dois testes unitários aprovados.
- Cinco testes em `UiRedesignTest` aprovados no emulador Android 8 (API 26): navegação para cadastro, preservação de rascunho/data após recriação, resumo em paisagem, meses/ajuda e detalhe mensal.
- Inspeção visual de tela inicial, formulário, lista com lançamento de exemplo, menu, meses e gráfico anual.
- Lint sem erros; permanecem avisos do projeto, incluindo dependências, recursos e textos.

Comandos de build:

```text
gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug --offline
```

O executor UTP do Gradle não estava completo no cache local. Os testes de interface foram executados diretamente com os APKs instalados no emulador:

```text
adb shell am instrument -w -e class com.example.controledegastos.features.UiRedesignTest com.example.controledegastos.test/com.example.controledegastos.HiltTestRunner
```

O Espresso 3.5.1 existente não conseguiu injetar eventos na imagem Android de prévia (API 37); a execução automatizada foi feita na imagem API 26. Não foram realizados profiling de bateria/memória nem testes em aparelho físico.

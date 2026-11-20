# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Estado do projeto

WeatherFish é um app Android (Kotlin) de estudo, ainda no esqueleto inicial: um único módulo `:app`, sem Activities, sem código de produção em `app/src/main/java` e sem README. O `AndroidManifest.xml` declara apenas o `<application>` (sem nenhuma Activity ainda). Package/namespace: `dev.rogerbertan.weatherfish`.

## Comandos

Use sempre o wrapper (`./gradlew`):

```bash
./gradlew assembleDebug                  # build do APK debug
./gradlew installDebug                   # instala em device/emulador conectado
./gradlew test                           # testes unitários (JVM, JUnit 4)
./gradlew testDebugUnitTest --tests "dev.rogerbertan.weatherfish.ExampleUnitTest"          # uma classe
./gradlew testDebugUnitTest --tests "dev.rogerbertan.weatherfish.ExampleUnitTest.<metodo>" # um teste
./gradlew connectedDebugAndroidTest      # testes instrumentados (exige device/emulador)
./gradlew lint                           # Android Lint
```

## Configuração de build

- **AGP 9.5.2** com `compileSdk`/`targetSdk` 37 e `minSdk` 35. O plugin Kotlin não é aplicado explicitamente em `app/build.gradle.kts` (AGP 9 traz suporte a Kotlin embutido); não adicione `kotlin-android` sem necessidade.
- Dependências e plugins ficam no version catalog `gradle/libs.versions.toml` e são referenciados como `libs.*`. Adicione novas dependências lá, não com strings inline.
- `settings.gradle.kts` usa `RepositoriesMode.FAIL_ON_PROJECT_REPOS`: repositórios só podem ser declarados lá, nunca em `build.gradle.kts` de módulos.
- Release usa o novo bloco `optimization { enable = true }` do AGP (no lugar de `minifyEnabled`), com regras em `app/src/main/keepRules/rules.keep` (não `proguard-rules.pro`).
- O template veio com Views/Material (`appcompat` + `material`), mas a UI alvo é **Jetpack Compose**. Compose ainda não está configurado.
- `local.properties` (caminho do SDK) é específico da máquina e não deve ser commitado.

## Arquitetura: Compose + MVVM

Camadas: UI (Composables + ViewModel) → Data (Repository + data sources). Uma camada de domínio (use cases) só deve existir quando a lógica for reutilizada por mais de um ViewModel. O fluxo é unidirecional: o estado desce do ViewModel para a UI e os eventos sobem da UI para o ViewModel.

**Faça:**
- Exponha o estado de cada tela como uma única propriedade `uiState` (`StateFlow` imutável de uma classe imutável `<Feature>UiState`). Mantenha o `MutableStateFlow` privado e atualize-o com `update`.
- Na UI, colete o estado com `collectAsStateWithLifecycle`.
- Receba eventos da UI como chamadas de função do ViewModel.
- Faça a conversão de Flows do repositório para estado com `stateIn` e rode o trabalho assíncrono em `viewModelScope`.
- Mantenha os Composables sem estado e faça state hoisting. Somente o Composable de nível de tela conhece o ViewModel. Os filhos recebem dados e lambdas.
- Use um `Modifier` como primeiro parâmetro opcional dos Composables reutilizáveis.
- Use `key` estável e única (id do item) em listas lazy.
- Use `remember` apenas para cálculos caros e `derivedStateOf` para limitar recomposições.
- Exponha os dados do repositório como `Flow` ou funções `suspend`, sempre main-safe (o repositório escolhe o dispatcher).
- Escreva testes unitários do ViewModel isolados da UI.

**Não faça:**
- Não acesse data sources (rede, banco, DataStore) diretamente de Composables ou ViewModels. Passe sempre pelo repositório.
- Não passe ViewModel, `Context`, `Activity` ou `NavController` para Composables filhos, nem guarde nenhum deles dentro do ViewModel.
- Não coloque lógica de negócio em Composables.
- Não exponha estado mutável (`MutableStateFlow`, `var` público) para fora do ViewModel.
- Não escreva em um estado que já foi lido na mesma composição (backwards write).
- Não use índice nem `hashCode()` como key de lista.
- Não marque como `@Stable`/`@Immutable` classes que não sejam de fato estáveis.
- Não use `GlobalScope` nem lance coroutines sem um escopo ligado ao ciclo de vida.
- Não use LiveData em código novo.

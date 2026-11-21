# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Estado do projeto

WeatherFish é um app Android de estudo em Kotlin. O setup inicial da Fase 0 está pronto. O projeto tem um único módulo, o `:app`. Ele tem a `WeatherFishApplication`, anotada com `@HiltAndroidApp`, e uma `MainActivity` Compose, anotada com `@AndroidEntryPoint`, que só mostra o nome do app. O package e o namespace são `dev.rogerbertan.weatherfish`.

O projeto usa um módulo só por escolha. Não crie módulos Gradle novos. As camadas ficam separadas por pacote dentro de `dev.rogerbertan.weatherfish`:
- `domain` guarda os modelos e as interfaces de repositório. Não importa nada de Android.
- `data` guarda as implementações dos repositórios, como o fake local e o cliente da Xweather.
- `ui` guarda o tema em `ui/theme` e um pacote por tela, como `ui/today`, `ui/week`, `ui/search`, `ui/map` e `ui/share`.
- Os pacotes de `ui` dependem das interfaces de `domain`, nunca das classes de `data`. O Hilt liga cada interface à implementação.

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

- AGP 9.5.2 com `compileSdk` e `targetSdk` 37 e `minSdk` 35. O AGP 9 já compila Kotlin, então o `:app` não aplica o plugin Kotlin. Não adicione `kotlin-android`. O plugin `kotlin-compose` define a versão do Kotlin do build, e a versão dele no catálogo é a `kotlin`.
- O Hilt gera código com KSP, declarado como `ksp(libs.hilt.compiler)`. Não use kapt.
- O Compose BOM está em 2026.09.00. O Compose 1.13, que vem nos BOMs mais novos, exige `compileSdk` 37.1, e esta máquina só tem a plataforma `android-37.0`.
- Dependências e plugins ficam no version catalog `gradle/libs.versions.toml` e são referenciados como `libs.*`. Adicione novas dependências lá, não com strings inline.
- `settings.gradle.kts` usa `RepositoriesMode.FAIL_ON_PROJECT_REPOS`: repositórios só podem ser declarados lá, nunca em `build.gradle.kts` de módulos.
- Release usa o novo bloco `optimization { enable = true }` do AGP (no lugar de `minifyEnabled`), com regras em `app/src/main/keepRules/rules.keep` (não `proguard-rules.pro`).
- A UI usa só Jetpack Compose. As bibliotecas de Views `appcompat` e `material` foram removidas. O tema XML `Theme.WeatherFish` só pinta a janela antes do Compose desenhar.
- `local.properties` é específico da máquina, está no `.gitignore` e não deve ser commitado. Além do `sdk.dir`, ele guarda três chaves, e as três vão para o `BuildConfig` do `:app`. `XWEATHER_CLIENT_ID` e `XWEATHER_CLIENT_SECRET` são as credenciais da Xweather. `MAPBOX_ACCESS_TOKEN` é o token público do Mapbox, que começa com `pk.`. Uma chave ausente vira string vazia, e o build passa sem ela.

## Arquitetura com Compose e MVVM

O app tem duas camadas. A camada de UI tem os Composables e o ViewModel, e chama a camada de dados, que tem o Repository e os data sources. Crie uma camada de domínio com use cases só quando mais de um ViewModel reutilizar a mesma lógica. O fluxo é unidirecional. O estado desce do ViewModel para a UI, e os eventos sobem da UI para o ViewModel.

**Faça:**
- Exponha o estado de cada tela numa única propriedade `uiState`. Ela é um `StateFlow` imutável de uma classe imutável `<Feature>UiState`. Mantenha o `MutableStateFlow` privado e atualize-o com `update`.
- Na UI, colete o estado com `collectAsStateWithLifecycle`.
- Receba eventos da UI como chamadas de função do ViewModel.
- Faça a conversão de Flows do repositório para estado com `stateIn` e rode o trabalho assíncrono em `viewModelScope`.
- Mantenha os Composables sem estado e faça state hoisting. Somente o Composable de nível de tela conhece o ViewModel. Os filhos recebem dados e lambdas.
- Use um `Modifier` como primeiro parâmetro opcional dos Composables reutilizáveis.
- Em listas lazy, use o id do item como `key`. Ele precisa ser estável e único.
- Use `remember` só para cálculos caros e `derivedStateOf` para limitar recomposições.
- Exponha os dados do repositório como `Flow` ou funções `suspend`. Eles precisam ser main-safe, e o repositório escolhe o dispatcher.
- Escreva testes unitários do ViewModel isolados da UI.

**Não faça:**
- Não acesse data sources a partir de Composables ou ViewModels. Rede, banco e DataStore só são acessados pelo repositório.
- Não passe ViewModel, `Context`, `Activity` ou `NavController` para Composables filhos, nem guarde nenhum deles dentro do ViewModel.
- Não coloque lógica de negócio em Composables.
- Não exponha `MutableStateFlow` nem `var` público para fora do ViewModel.
- Não escreva num estado que já foi lido na mesma composição. Isso é um backwards write.
- Não use índice nem `hashCode()` como key de lista.
- Não marque com `@Stable` ou `@Immutable` uma classe que não é estável.
- Não use `GlobalScope` nem lance coroutines sem um escopo ligado ao ciclo de vida.
- Não use LiveData em código novo.

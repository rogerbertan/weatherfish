# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Estado do projeto

WeatherFish é um app Android de estudo em Kotlin. As Fases 0, 1 e 2 estão prontas. As telas mostram dados reais da Xweather pelo SDK Aeris. Os dados falsos de `app/src/main/assets/fake/` voltam com a flag `DataSourceConfig.useFake`. O projeto tem um único módulo, o `:app`. A `WeatherFishApplication` é anotada com `@HiltAndroidApp` e inicializa o SDK no `onCreate` com `XweatherSdk.init()`. A `MainActivity` é anotada com `@AndroidEntryPoint` e só chama `WeatherFishApp`. O package e o namespace são `dev.rogerbertan.weatherfish`.

O projeto usa um módulo só por escolha. Não crie módulos Gradle novos. As camadas ficam separadas por pacote dentro de `dev.rogerbertan.weatherfish`:
- `domain` guarda os modelos `GeoPoint`, `Place`, `CurrentWeather`, `HourlyForecast`, `DailyForecast` e `WeatherCondition`. Guarda também as interfaces `WeatherRepository` e `ActiveLocation`, mais a `sealed class WeatherError`, que estende `Exception`. Não importa nada de Android.
- O `WeatherRepository` devolve os dados direto e lança `WeatherError` quando falha, como um service do Spring. Não crie tipos de resultado como `Result` ou `Either` para o erro.
- `data/xweather` guarda o formato cru da API e o mapeamento para o domínio. As classes com sufixo `Response` são as que vêm da API, como `XweatherResponse<T>`, `ConditionsResponse`, `ForecastResponse` e `PlaceResponse`. As classes aninhadas não levam sufixo. Não use `Dto` no nome. Não crie classes `*Request`, porque a Xweather só recebe GET com query params.
- `data/xweather` também guarda o adapter do SDK Aeris, cujo pacote é `com.aerisweather`. É o único pacote que importa o SDK Aeris. O `XweatherWeatherRepository` roda `AerisCommunicationTask(context, request).executeSyncTask()` dentro de `withContext(Dispatchers.IO)`. Não use `execute()` com callback, porque ele passa pelo executor serial do `AsyncTask` e devolve o resultado na main thread. O `AerisMapping.kt` converte os models do SDK Aeris para o domínio. Os tipos do SDK Aeris têm os mesmos nomes das classes do pacote, então o `AerisMapping.kt` os importa com alias, como `ConditionsResponse as AerisConditionsResponse`.
- O `XweatherSdk` é o único lugar que lê `XWEATHER_CLIENT_ID` e `XWEATHER_CLIENT_SECRET` do `BuildConfig`. Ele usa o overload `initWithKeys(id, secret, appId)`, que não faz rede nem disco no startup. Ele também desliga o log do SDK Aeris com `LogLevel.NONE`, porque o SDK grava no logcat a URL com o `client_secret` quando um request falha. Com as chaves vazias, o repositório lança `Unauthorized` sem fazer request.
- O `weatherErrorFor(code)` traduz os códigos de erro da Xweather e do SDK para `WeatherError`. O SDK Aeris devolve `"IOException"` para qualquer falha de HTTP, inclusive um 401 de chave inválida, e o `weatherErrorFor` traduz esse código para `Network`. Nunca logue nem exiba a `description` dos erros do SDK Aeris, porque ela pode conter a URL com o `client_secret`.
- A busca real tira os acentos do termo com `foldForSearch` antes de enviar, porque a Xweather não encontra `são paulo`. O SDK Aeris não codifica a URL, então a busca também descarta qualquer caractere que não seja letra, dígito, espaço, `-` ou `'`.
- O `parsingOrThrow` de `XweatherMapping.kt` converte as exceções de parsing em `WeatherError.Unknown`. O fake e o repositório real usam o mesmo helper. Os detalhes da API estão em `docs/aeris-spike.md`.
- `data/fake` guarda o `FakeWeatherRepository`. Ele lê os JSONs de `assets/fake/`, um por endpoint, e reaproveita as classes de `data/xweather`.
- `data` também guarda a `FixedActiveLocation`, que por enquanto sempre devolve São Paulo.
- `di` guarda o `DataModule`, que liga cada interface de `domain` à implementação. O `DataSourceConfig(useFake = false)` escolhe o repositório. Troque para `true` para voltar ao fake. A latência do fake e a flag `simulatedError` ficam no `FakeConfig` desse módulo. Para ver a tela de erro, troque `simulatedError = null` por um `WeatherError`.
- `ui` guarda o NavHost e a bottom bar em `WeatherFishApp.kt`, o tema em `ui/theme` e os componentes comuns em `ui/common`. Há um pacote por tela: `ui/today`, `ui/details`, `ui/week`, `ui/search`, `ui/map` e `ui/share`.
- Os pacotes de `ui` dependem das interfaces de `domain`, nunca das classes de `data`. O `ArchitectureTest`, escrito com Konsist, falha se `domain` ou `ui` importar algo de `data`, do SDK Aeris, do Gson, do Mapbox ou de bibliotecas de rede. Ele também falha se `domain` importar algo de Android, se um arquivo fora de `data.xweather` importar o SDK Aeris ou se um arquivo fora de `data.xweather` mencionar `XWEATHER_CLIENT`.

## Comandos

Use sempre o wrapper (`./gradlew`):

```bash
./gradlew assembleDebug                  # build do APK debug
./gradlew installDebug                   # instala em device/emulador conectado
./gradlew test                           # testes unitários (JVM, JUnit 5)
./gradlew testDebugUnitTest --tests "dev.rogerbertan.weatherfish.ArchitectureTest"         # uma classe
./gradlew testDebugUnitTest --tests "*TodayViewModelTest.should show error*"               # um teste
./gradlew connectedDebugAndroidTest      # testes instrumentados (exige device/emulador)
./gradlew lint                           # Android Lint
```

## Configuração de build

- AGP 9.5.2 com `compileSdk` e `targetSdk` 37 e `minSdk` 35. O AGP 9 já compila Kotlin, então o `:app` não aplica o plugin Kotlin. Não adicione `kotlin-android`. O plugin `kotlin-compose` define a versão do Kotlin do build, e a versão dele no catálogo é a `kotlin`.
- O Hilt gera código com KSP, declarado como `ksp(libs.hilt.compiler)`. Não use kapt.
- O plugin `kotlin-serialization` usa a mesma versão `kotlin` do catálogo. Ele serve às rotas type-safe do Navigation e ao parsing dos JSONs com `kotlinx-serialization-json`.
- Os ícones vêm de `material-icons-extended`, com a versão definida pelo Compose BOM.
- Os testes usam JUnit 5, com a versão definida pelo `junit-bom`. O plugin `de.mannodermaus.android-junit` liga a JUnit Platform nos testes locais e nos instrumentados. Nenhum teste usa JUnit 4. O `androidx.test:runner`, que o plugin adiciona, ainda traz o `junit:junit` de forma transitiva nos testes instrumentados, porque o `AndroidJUnitRunner` depende dele. Não adicione `junit:junit`, `androidx.test.ext:junit` nem `ui-test-junit4` como dependência direta. Fique na linha 5.x do JUnit. A troca para a 6.x só acontece se o usuário pedir.
- O Compose BOM está em 2026.09.00. O Compose 1.13, que vem nos BOMs mais novos, exige `compileSdk` 37.1, e esta máquina só tem a plataforma `android-37.0`.
- Dependências e plugins ficam no version catalog `gradle/libs.versions.toml` e são referenciados como `libs.*`. Adicione novas dependências lá, não com strings inline.
- `settings.gradle.kts` usa `RepositoriesMode.FAIL_ON_PROJECT_REPOS`: repositórios só podem ser declarados lá, nunca em `build.gradle.kts` de módulos.
- Release usa o novo bloco `optimization { enable = true }` do AGP (no lugar de `minifyEnabled`), com regras em `app/src/main/keepRules/rules.keep` (não `proguard-rules.pro`). O `packageScope` limita o R8 a `androidx`, `kotlin` e `kotlinx`. O Gson preenche por reflexão o pacote `model` do SDK Aeris. O `rules.keep` mantém esse pacote para quando o `packageScope` crescer.
- O `app/build.gradle.kts` declara o `aeris-core-lib` com `isTransitive = false`, o equivalente ao `@aar`. O POM dele traz `play-services-maps` e `androidx.test.ext:junit`, que o SDK Aeris não usa. Por isso o `app/build.gradle.kts` declara o Gson à parte.
- O manifest do SDK Aeris pede storage, localização, `READ_GSERVICES` e OpenGL. O `AndroidManifest.xml` do app remove esses itens com `tools:node="remove"`. Quando uma fase precisar de localização, tire a remoção correspondente. A comparação de APK, manifest e dependências antes e depois do Aeris está em `docs/aeris-comparativo.md`.
- A UI usa só Jetpack Compose. As bibliotecas de Views `appcompat` e `material` foram removidas. O tema XML `Theme.WeatherFish` só pinta a janela antes do Compose desenhar.
- `local.properties` é específico da máquina, está no `.gitignore` e não deve ser commitado. Além do `sdk.dir`, ele guarda três chaves, e as três vão para o `BuildConfig` do `:app`. `XWEATHER_CLIENT_ID` e `XWEATHER_CLIENT_SECRET` são as credenciais da Xweather. `MAPBOX_ACCESS_TOKEN` é o token público do Mapbox, que começa com `pk.`. Uma chave ausente vira string vazia, e o build passa sem ela.

## Arquitetura com Compose e MVVM

O app tem duas camadas. A camada de UI tem os Composables e o ViewModel, e chama a camada de dados, que tem o Repository e os data sources. Crie uma camada de domínio com use cases só quando mais de um ViewModel reutilizar a mesma lógica. O fluxo é unidirecional. O estado desce do ViewModel para a UI, e os eventos sobem da UI para o ViewModel.

**Faça:**
- Exponha o estado de cada tela numa única propriedade `uiState`. Ela é um `StateFlow` imutável de um tipo imutável. Mantenha o `MutableStateFlow` privado. Use `update` quando o novo estado depender do anterior, como em `update { it.copy(...) }`. Para substituir o estado inteiro, atribua a `.value`.
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
- Siga a convenção de arquivos do Kotlin. Agrupe num arquivo só as declarações do mesmo assunto, como `Place.kt` com `GeoPoint` e `Place`, ou `Weather.kt` com a condição e as previsões. Um arquivo com uma classe só leva o nome dela, como `WeatherRepository.kt` e `WeatherError.kt`. Não coloque num arquivo com nome de classe outros tipos sem relação com ela.
- Não escreva nome qualificado no corpo do código, como `java.time.ZoneId.of(...)`. Importe sempre no topo do arquivo, inclusive em mudanças temporárias.

**Padrões já usados no projeto:**
- O estado de carga de uma tela é o `LoadState<T>` de `ui/common`, com os casos `Loading`, `Failed` e `Loaded`. Use o tipo direto, como `LoadState<TodayWeather>`, sem `typealias`. Não use flags booleanas para isso.
- O helper `loadState { }` chama o repositório e converte o resultado em `Loaded`, ou um `WeatherError` em `Failed`. Ele é o único lugar da UI que captura `WeatherError`.
- O `LoadStateContent` desenha os estados `Loading` e `Failed`, este com o botão de retry. A tela só desenha o estado `Loaded`. O `modifier` vai para o `LoadStateContent`, não para o conteúdo.
- Hoje, Semana e Detalhes carregam no `init` com uma função privada `load()`. Ela lê a localidade com `activeLocation.place.first()` e atualiza o `MutableStateFlow` privado. O `retry()` chama a mesma `load()`. O estado fica no ViewModel enquanto a tela estiver na back stack, então voltar para a aba não recarrega os dados.
- Quando a localidade ativa puder mudar, troque o `first()` por uma coleta da `ActiveLocation` no `init`.
- O texto da Busca é um `TextFieldState` no `SearchViewModel`. O ViewModel observa o texto sem espaços nas pontas com `snapshotFlow`, que só emite quando o valor muda. Cada termo novo cancela o `searchJob` anterior, põe o estado em `Loading`, espera 300 ms e busca. Um termo vazio limpa os resultados sem passar por `Loading`.
- As rotas são `@Serializable data object`. Hoje e Detalhes ficam num grafo aninhado, o `TodayGraph`, para que a aba Hoje continue selecionada em Detalhes. Só o `WeatherFishApp` conhece o `NavController`, e as telas recebem lambdas.

## Testes

- Use as anotações e asserções de `org.junit.jupiter.api`. Mocke as dependências com `mockk`, `coEvery` e `coVerify`, do MockK. Não escreva classes stub à mão.
- Para casos de tabela, use `@ParameterizedTest` com `@CsvSource`, como no `XweatherMappingTest`. Não junte vários casos num único `assertEquals` de listas.
- O nome de cada teste segue o padrão `should ...`, entre crases, por exemplo `` `should show error when hourly forecast fails` ``.
- O corpo segue o padrão AAA, separado pelos marcadores `// Arrange`, `// Act` e `// Assert`.
- Nos testes de ViewModel, registre a `MainDispatcherExtension` num campo com `@JvmField @RegisterExtension`. Rode cada teste com `runTest(mainDispatcher.dispatcher)` e avance o tempo com `advanceUntilIdle` ou `runCurrent`.
- Nos testes da Busca, escreva no `TextFieldState` com `setTextAndPlaceCursorAtEnd` e chame `Snapshot.sendApplyNotifications()` em seguida. Fora da composição, o `snapshotFlow` só vê a mudança depois dessa chamada.
- Teste funções puras, como parsing e mapeamento, sobre entradas reais. O `FakeJsonTest` lê os arquivos de `src/main/assets/fake/` direto do disco.
- Os dados de teste compartilhados ficam em `ui/TestData.kt`, no source set de teste.

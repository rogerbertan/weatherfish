# Endurecimento do release

A Fase 8 não traz funcionalidade nova. Ela confere o APK release com R8 no emulador, provoca os estados de erro, procura o `client_secret` no APK e mede o resultado final. No fim, responde as perguntas que o discovery deixou em aberto. Os testes rodaram no emulador `sdk_gphone64_arm64` com Android 17, Play Services e idioma do sistema em inglês. O APK release foi assinado com a chave de debug, depois do `zipalign -p 16`. O build não tem `signingConfig`.

## Smoke test do release

O release usa o `DataSource.OpenMeteo` atual. O roteiro passou por todas as telas com o logcat gravando. Nenhuma linha `FATAL EXCEPTION` apareceu, e nenhum erro veio do R8. O `rules.keep` não mudou.

| Tela ou fluxo                  | O que foi feito                                                                       | Resultado                                                              |
|--------------------------------|---------------------------------------------------------------------------------------|------------------------------------------------------------------------|
| Hoje                           | Primeira abertura, com a permissão de localização concedida                            | Abre em São Paulo e troca para a cidade do aparelho                    |
| Detalhes                       | Abrir e voltar                                                                        | Grid completo                                                          |
| Favoritas                      | Favoritar pela Hoje, trocar para °F, tocar em "Minha localização"                     | A lista, a unidade e a localização funcionam                           |
| Semana                         | Abrir a aba                                                                           | 7 dias. O nome do dia sai em inglês, como mostra a seção abaixo        |
| Busca                          | Digitar `florian` e tocar em Florianópolis                                            | Resultados com região e país. A Hoje troca de cidade                   |
| Mapa                           | Abrir a aba, ligar o vento, girar                                                     | Mapa centralizado na cidade, sem camadas, como mostra a seção de erros |
| Compartilhar                   | Abrir a aba                                                                           | QR da cidade ativa                                                     |
| Scanner do Google              | Abrir e apertar voltar                                                                | Abre, e o voltar fecha sem mensagem                                    |
| Câmera do app                  | Negar a permissão, pedir de novo, conceder, girar                                     | Mensagem de recusa, depois o preview. A rotação mantém a câmera        |
| `place?name=...` com app aberto | `adb shell am start` sobre a câmera, e Salvar                                        | Diálogo de importação, salvo com "Rio Grande do Sul, Brasil"           |
| `week` a frio                  | `am force-stop` e o link                                                              | Abre a Semana, e o voltar leva à Hoje                                  |
| `share/scan` a frio            | `am force-stop` e o link                                                              | Abre a câmera, e o voltar leva à Compartilhar                          |
| Rotação                        | Hoje, câmera e Mapa com o vento ligado                                                | Estado mantido, sem crash                                              |

O código do app e os SDKs ficam fora do `packageScope` do R8, que só cobre `androidx`, `kotlin` e `kotlinx`. Por isso o risco de R8 ficava nas rotas `@Serializable` do Navigation e no Compose, e os dois passaram.

### Dia da semana em inglês

A Semana mostra "Tuesday" e "Wednesday", enquanto o resto do app fala português. O app só tem strings em `values`, sem qualificador, então o Android usa essas strings em qualquer idioma. O `formatWeekday` de `ui/common/Format.kt` usa o `Locale.getDefault()`, que segue o sistema, e o emulador está em inglês. O defeito não é do release e aparece também no debug. Há duas saídas: fixar o locale `pt-BR` no `formatWeekday`, ou declarar `pt-BR` como o único idioma do app com o `localeConfig` e o `LocaleManager` da plataforma, que o `minSdk` 35 já tem.

## Estados de erro

Cada caso rodou num release próprio, com uma troca temporária no `DataModule` ou no `local.properties`. A chave inválida é um `XWEATHER_CLIENT_SECRET` falso. A cota da Xweather já estava esgotada, então o caso de cota usa a conta real. Depois de cada caso, um `grep` no logcat procurou o valor do segredo, o real ou o falso, e não achou nada.

| Caso            | Adapter    | Tela                                                        |
|-----------------|------------|-------------------------------------------------------------|
| Sem rede        | Open-Meteo | "Sem conexão. Confira sua internet.", com Tentar de novo     |
| Cota esgotada   | REST       | "Limite de consultas atingido. Tente mais tarde."           |
| Cota esgotada   | SDK Aeris  | "Sem conexão. Confira sua internet."                        |
| Chave inválida  | REST       | "Não foi possível acessar o serviço de previsão."           |
| Chave inválida  | SDK Aeris  | "Sem conexão. Confira sua internet."                        |
| Cota ou chave   | Mapa       | Mapa base sem camadas e sem mensagem                        |
| Chaves vazias   | Mapa       | "O mapa precisa das chaves da Xweather no local.properties" |

- **Sem rede.** Com o modo avião, a Hoje mostra o erro de rede. O "Buscando sua localização" some sozinho, porque o `Geocoder` não responde sem rede e a `DeviceActiveLocation` mantém a última cidade salva. Depois de religar a rede, o Tentar de novo carrega o clima.
- **REST.** O `parseOrThrow` decide pelo status, e o `xweatherErrorIn` lê o código do corpo. A chave inválida vira `Unauthorized`, e a cota esgotada vira `QuotaExceeded`. O teste não registrou o corpo das respostas, só o erro que chegou à tela. Com a cota diária esgotada, os headers por minuto ainda diziam `98/100 per minute`. O `x-ratelimit-*-minute` não avisa sobre a cota do dia.
- **SDK Aeris.** O SDK devolve `"IOException"` para qualquer falha de HTTP, como o `aeris-spike.md` registra. Cota esgotada e chave inválida viram "Sem conexão", e o usuário recebe a orientação errada. Só o REST distingue os três casos.
- **Mapa.** O MapsGL registra `MapsGL layer conditions.temperature.fill data load failed, will retry: Invalid authentication token` com a cota esgotada, e continua tentando. A falha não sai do SDK. O `MapboxMapController` não expõe nenhum callback de erro, então o `WeatherMapView` não tem como mostrar uma mensagem. O caso de chaves vazias já mostra uma mensagem no lugar do mapa, porque o `mapCredentials` vira `null` antes de criar o controller.

## O `client_secret` no APK

O jadx 1.5.7 decompilou o release em 22 s. O segredo apareceu em dois lugares:

- O `dev/rogerbertan/weatherfish/BuildConfig.java` tem o `XWEATHER_CLIENT_ID` e o `XWEATHER_CLIENT_SECRET` como constantes `String`, com o valor em texto puro.
- O `data/xweather/XweatherSdk.java` lê as duas constantes com o nome original, porque o R8 não toca o código do app, como diz o [smoke test](#smoke-test-do-release).

O jadx nem é preciso. O `strings classes2.dex | grep` acha o valor direto. O token público do Mapbox está em `res/values/strings.xml`, como o `resValue` `mapbox_access_token`. Esse token é público por desenho, e quem o protege são as restrições de URL e de app no painel do Mapbox.

Ampliar o `packageScope` ou ofuscar não resolve. O R8 renomeia classes, mas não cifra literais, e qualquer constante que o app usa em runtime pode ser extraída do APK, ou capturada na rede com um proxy. A Xweather manda as chaves como query param em cada request. As saídas de verdade são:

- **Proxy no backend.** O app chama um servidor próprio, que guarda o segredo e repassa o request. É a única forma de o segredo não sair do servidor.
- **Restrição por namespace.** A Xweather amarra a chave ao `applicationId` pelo header que o SDK manda. O `aeris-spike.md` mostrou que a chave atual não restringe namespace, e que um REFERER de outro namespace recebe 200. Mesmo ligada, a restrição só compara um header que qualquer cliente pode forjar.
- **Cota como limite de dano.** Com uma conta Developer, o pior caso é alguém esgotar a cota, que é o estado em que a conta já está.

O segredo não vaza por log. O `XweatherSdk` desliga o log do SDK Aeris, o REST nunca loga URL nem exceção do Ktor, e o MapsGL não loga o segredo. Os greps desta fase confirmam isso no release.

## Merged manifest final

O manifest do APK release, lido com `aapt2 dump xmltree`:

| Item                          | Valor                                                                                                                                              |
|-------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| Permissões                    | `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `ACCESS_COARSE_LOCATION`, `CAMERA` e a `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` do AndroidX |
| Features                      | `android.hardware.camera` com `required="false"`, `glEsVersion` 3.0 com `required="true"` e `android.hardware.wifi` com `required="false"`         |
| Activities                    | `MainActivity`, a `GmsBarcodeScanningDelegateActivity` do scanner e a `GoogleApiActivity` do Play Services                                          |
| Services, receivers, providers | `MetadataHolderService` da CameraX, `MlKitComponentDiscoveryService`, `MlKitInitProvider`, `InitializationProvider` do App Startup, `LocationUpdatesReceiver` e `LifecycleService` do Mapbox, `ProfileInstallReceiver` e três componentes do `datatransport` |
| Inicializadores do App Startup | Mapbox Maps, Mapbox Common, EmojiCompat, ProcessLifecycle, ProfileInstaller e o `PlatformInitializer` do OkHttp 5                                   |

Os `tools:node="remove"` continuam valendo. O manifest não tem `ACCESS_FINE_LOCATION`, `WRITE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE` nem `READ_GSERVICES`. O `glEsVersion` que o app removeu do SDK Aeris voltou pelo MapsGL. Ele filtra na Play Store os aparelhos sem OpenGL ES 3.0. O manifest do MapsGL avisa que as camadas de partículas, como o vento, precisam de GLES 3.1 e devem ser ligadas só depois de checar a versão em runtime. O app não faz essa checagem. Num aparelho só com 3.0, o botão Vento tentaria compilar shaders `#version 310 es`.

O `LocationUpdatesReceiver` do Mapbox entra no manifest, mas o app não declara o `ACCESS_FINE_LOCATION`, e o mapa não usa o componente de localização do Mapbox.

## Tamanho final

| APK                    | Tamanho        | Arquivos |
|------------------------|----------------|----------|
| Debug                  | 194.514.016 B  | 819      |
| Release sem assinatura | 123.064.188 B  | 667      |
| Release assinado       | 123.124.731 B  | 670      |

O release assinado tem três arquivos a mais, que são a assinatura v1 em `META-INF`.

O release por tipo de conteúdo:

| Conteúdo              | Bytes no zip | Parte do release |
|-----------------------|--------------|------------------|
| `lib/x86`             | 27.979.020   | 22,7%            |
| `lib/x86_64`          | 27.223.584   | 22,1%            |
| `lib/arm64-v8a`       | 24.847.720   | 20,2%            |
| `classes*.dex`        | 24.130.768   | 19,6%            |
| `lib/armeabi-v7a`     | 16.613.404   | 13,5%            |
| `assets`              | 1.070.486    | 0,9%             |
| `resources.arsc`      | 516.180      | 0,4%             |
| `res`                 | 254.026      | 0,2%             |
| `META-INF` e outros   | 106.635      | 0,1%             |

As `.so` somam 96,7 MB, ou 78% do release. Em cada ABI, as maiores são o `libmapbox-maps.so`, o `libmapbox-common.so` e o `libbarhopper_v3.so` do ML Kit bundled. No `arm64-v8a`, elas têm 12,1 MB, 5,8 MB e 4,9 MB. O AGP guarda as `.so` e os `.dex` sem compressão, porque o `minSdk` 35 permite carregar os dois direto do APK. Por isso o zip não encolhe esses arquivos.

A evolução do release, com os números dos docs de cada fase:

| Marco                                | Release        | Doc                                       |
|--------------------------------------|----------------|-------------------------------------------|
| Fase 2, SDK Aeris                    | ~3.500.000 B   | [aeris-comparison](aeris-comparison.md)   |
| Fase 4, ZXing e scanner do Google    | 6.517.150 B    | [qr-comparison](qr-comparison.md)         |
| Fase 5, CameraX e ML Kit bundled     | 31.994.335 B   | [qr-camera-comparison](qr-camera-comparison.md) |
| Fase 6, Mapbox e MapsGL              | 120.464.143 B  | [mapsgl-comparison](mapsgl-comparison.md) |
| Fase 7, Ktor e OkHttp                | 123.064.180 B  | [rest-comparison](rest-comparison.md)     |

Três mudanças cortariam a maior parte do tamanho. Nenhuma foi feita nesta fase:

- **App Bundle ou splits por ABI.** A Play Store entrega só a ABI do aparelho. Um celular `arm64` baixaria uns 25 MB de `.so`, e não 96,7 MB. É a maior economia, e não muda código.
- **ML Kit unbundled.** Tira o `libbarhopper_v3.so`, cerca de 5 MB por ABI, ao custo do download do modelo pelo Play Services.
- **`packageScope` maior.** Passar o app e os SDKs pelo R8 reduziria parte dos 24 MB de `.dex`. Isso exige keep rules para o Gson do SDK Aeris, que o `rules.keep` já tem, e para o que mais usar reflexão.

## Perguntas em aberto do discovery

| Pergunta                                                                                                  | Resposta                                                                                                                                                                                                                                                                                                                                                       |
|-----------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| O `MapboxMapController` funciona criado dentro do `DisposableMapEffect`, ou precisa do `AndroidView` com layout listener? | Funciona no `DisposableMapEffect`, sem o fallback. As camadas entram no `subscribeMapLoaded`. Ver [mapsgl-spike](mapsgl-spike.md#criar-o-controller-no-disposablemapeffect).                                                                                                                                                                                  |
| Quem manda no estilo: o `style` do `MapboxMap` composable ou o `loadStyle` do controller?                 | O app nunca chama o `loadStyle`. O `MapStyle(style = Style.LIGHT, projection = Projection.MERCATOR)` do composable define o estilo, e o controller só adiciona camadas sobre ele. O conflito não foi testado, porque não foi preciso.                                                                                                                       |
| Como remover ou alternar uma camada no MapsGL, e como liberar o controller no dispose?                    | O vento entra com `addWeatherLayer` e sai com `removeWeatherLayer`, pelo código da camada. O `onDispose` do `DisposableMapEffect` chama `shutdown()`, porque trocar de aba não destrói a `NavBackStackEntry`. Ver [mapsgl-spike](mapsgl-spike.md#recursos-depois-de-sair-da-tela).                                                                           |
| Quantos accesses um minuto de uso do mapa consome?                                                        | Sem resposta. A cota da conta esgotou antes da medição, e com ela esgotada o MapsGL não carrega tiles. A medição continua pendente em [mapsgl-comparison](mapsgl-comparison.md#cota-da-xweather).                                                                                                                                                           |
| Em qual thread roda o callback do `execute()` no Aeris Core?                                              | Na main thread, porque o `execute()` passa pelo executor serial do `AsyncTask`. O app usa o `executeSyncTask()` dentro de `withContext(Dispatchers.IO)`, que roda na thread de quem chama. Ver [aeris-spike](aeris-spike.md).                                                                                                                                |
| O `AerisResponse` expõe os headers `X-RateLimit-*`?                                                       | Não. O SDK não dá acesso aos headers. O adapter REST lê e loga os `x-ratelimit-*-minute`, e eles medem só a janela por minuto, não a cota do dia. Ver [rest-comparison](rest-comparison.md).                                                                                                                                                                 |
| O token secreto de download da Mapbox ainda é exigido pro Maps SDK v11?                                   | Não. O repositório Maven do Mapbox entrega a v11 sem credenciais. Ver [mapsgl-spike](mapsgl-spike.md#versões).                                                                                                                                                                                                                                              |
| O namespace aceita wildcard pra variantes `.debug`?                                                       | Não testado. O app não usa `applicationIdSuffix`, então debug e release têm o mesmo namespace. A chave atual também não restringe namespace, e por isso não havia o que testar. Ver [aeris-spike](aeris-spike.md).                                                                                                                                         |
| O minSdk real do MapsGL é 26 ou 28?                                                                       | 28. O `AndroidManifest.xml` do AAR `mapsgl-android-sdk-v1.8.0` declara `minSdkVersion="28"`, o mesmo do demo. A doc que diz Android 8.0 está desatualizada. O app usa `minSdk` 35, então a diferença não pesa aqui.                                                                                                                                       |
| O code scanner roda no emulador com imagem Google Play, ou só em aparelho físico?                         | Roda no emulador `sdk_gphone64_arm64` com Play Services. Todos os testes de leitura das Fases 4 e 8 rodaram nele. Ver [qr-comparison](qr-comparison.md#o-que-o-scanner-devolve).                                                                                                                                                                          |
| Quanto demora o download do módulo `barcode_ui` num build de debug, e o que o usuário vê enquanto isso?   | Não medido. No emulador, o módulo já estava presente na primeira leitura, e o scanner abriu sem espera visível. O app não sabe que o download acontece, porque o `startScan()` só completa a `Task` quando o scanner fecha. Numa instalação pela Play Store, o meta-data `barcode_ui` baixa o módulo junto com o app. Ver [qr-comparison](qr-comparison.md). |
| Compensa trocar `AndroidView` + `PreviewView` por `CameraXViewfinder` com conversão de coordenadas manual? | Não foi implementado. A conversão manual não seria precisa. O `QrCameraView` usa o `MlKitAnalyzer` com `COORDINATE_SYSTEM_ORIGINAL`, porque só lê o texto dos códigos e não desenha nada sobre eles. A moldura da área de leitura é fixa, em Compose. A troca tiraria o `AndroidView` e o defeito do `clipToBounds` em paisagem. Ela custaria a integração pronta do `LifecycleCameraController` com o `PreviewView`. É uma avaliação, não uma medição. |
| Bundled ou unbundled no ML Kit: qual a diferença real medida no APK?                                      | O bundled soma 25.423.665 B ao release, e o unbundled soma 3.979.691 B. A diferença é o `libbarhopper_v3.so` e os modelos, cerca de 5 MB por ABI. O app ficou com o bundled. Ver [qr-camera-comparison](qr-camera-comparison.md).                                                                                                                             |

## Conclusões do estudo

- **As portas do domínio isolaram a UI dos adapters.** Os quatro adapters, fake, SDK Aeris, REST da Xweather e Open-Meteo, trocam com uma linha no `DataModule`. A UI não mudou em nenhuma troca. O `ArchitectureTest` com Konsist manteve as fronteiras durante oito fases.
- **O SDK Aeris soma pouco ao APK, mas traz defeitos de comportamento.** Ele tem `AsyncTask`, log com a URL e o segredo, URL sem codificação e o mesmo `"IOException"` para qualquer falha. Por causa disso, o usuário vê "Sem conexão" com a cota esgotada ou com a chave errada. O REST com Ktor tem mais linhas, mas controla o status, o cancelamento e o log.
- **O scanner do Google evita a permissão, e a câmera do app evita comportamento não documentado.** O scanner do Google não pede câmera e não pesa no APK, mas o voltar devolve um erro `INTERNAL` em vez do cancelamento documentado. A câmera do app custa a permissão, o lifecycle e uns 25 MB, e em troca dá controle total da tela.
- **O mapa é o item mais caro do app.** O Mapbox e o MapsGL respondem por quase três quartos do release. Eles também trouxeram um crash de thread em debug, um `glEsVersion` obrigatório e uma camada que some sem aviso quando a cota acaba.
- **Qualquer um que tenha o APK lê o `client_secret`.** O R8 não protege constantes, e o `strings` acha o `client_secret` no `.dex`. Os greps desta fase confirmam que o segredo não vaza por log. Para um app de verdade, a chave da Xweather ficaria atrás de um backend.
- **O release com R8 parcial é seguro, mas grande.** Com o R8 parcial, o release rodou sem nenhuma keep rule nova. O preço é o código do app e dos SDKs entrar inteiro. O que mais reduziria o tamanho é o App Bundle, e não o R8.

## O que ficou pendente

- Medir os accesses de um minuto de mapa quando a cota da Xweather renovar.
- Fixar o idioma do `formatWeekday`.
- Checar o GLES 3.1 antes de ligar o vento.
- Mostrar uma mensagem no mapa quando as camadas do MapsGL falham. Isso depende de um callback de erro que o SDK não tem.

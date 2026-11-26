# Spike do MapsGL com Compose

A Fase 6 liga o MapsGL 1.8.0 a um mapa Mapbox 11.15.3 desenhado pelo `maps-compose`. O exemplo oficial do MapsGL usa uma `MapView` de XML e cria o `MapboxMapController` dentro de um `OnGlobalLayoutListener`, depois do primeiro layout. Este documento registra o que funcionou em Compose e os problemas que o SDK trouxe.

## Versões

O POM do MapsGL 1.8.0 no JitPack não declara o Mapbox. O SDK compila contra ele, mas o app escolhe a versão. O app usa a `11.15.3` na variante `ndk27`, a mesma que o demo do MapsGL usa. A variante `ndk27` tem bibliotecas nativas alinhadas em páginas de 16 KB. O repositório Maven do Mapbox entrega a v11 sem token secreto, então o `settings.gradle.kts` declara o repositório sem credenciais.

## Criar o controller no `DisposableMapEffect`

O controller é criado pelo app dentro do `DisposableMapEffect` do `MapboxMap`, e o teste de viabilidade funcionou sem precisar recorrer ao `AndroidView`. Esse efeito é executado quando a `MapView` já está disponível. Já o `subscribeMapLoaded` aguarda a conclusão do layout e o carregamento do estilo para, então, permitir que o app adicione as camadas do mapa.

```kotlin
DisposableMapEffect(credentials) { mapView ->
    val controller = MapboxMapController(mapView, XweatherAccount(id, secret))
    val subscription = controller.mapboxMap?.subscribeMapLoaded { ... }
    onDispose {
        subscription?.cancel()
        controller.shutdown()
    }
}
```

O construtor registra o controller como observer do `ViewTreeLifecycleOwner` da `MapView`, que é o `NavBackStackEntry` da tela. O `onDestroy` desse observer só chama `shutdown()`. Trocar de aba com `saveState` não destrói a entrada, só a leva a `CREATED`. Por isso o `onDispose` chama `shutdown()` direto. O `shutdown()` cancela as assinaturas do Mapbox e o job de coroutines do controller. Ele também remove o próprio observer do lifecycle.

O `MapStyle(style = Style.LIGHT, projection = Projection.MERCATOR)` define a projeção. O MapsGL não desenha na projeção globe, que os estilos novos do Mapbox usam por padrão.

O app adiciona as camadas com `addWeatherLayer(code, null, beforeId)`. O `beforeId` é o id da primeira camada `symbol` do estilo. Sem ele, a temperatura cobre o mapa base inteiro, inclusive os nomes das cidades.

## Centralizar na localidade ativa

O `rememberMapViewportState` salva a câmera com um `Saver`, e por isso a câmera sobrevive à rotação. Quando a aba volta da back stack, o viewport aplica a câmera salva ao se ligar à nova `MapView`. Essa câmera sobrescreve um `flyTo` chamado num `LaunchedEffect` antes da ligação, e o mapa mostra a cidade anterior. A centralização roda num `MapEffect(center)`, que só executa com a `MapView` pronta. Um `rememberSaveable` guarda a última coordenada centralizada, para a rotação não desfazer o pan do usuário.

## `WorkerThreadException` em debug

No debug, o app fechava segundos depois de abrir o mapa:

```
com.mapbox.maps.exception.WorkerThreadException: The exception that is thrown when an application attempts to
perform a map operation on a worker thread.
    at com.mapbox.maps.MapboxMap.executeOnRenderThread(MapboxMap.kt:1368)
    at A.i.invokeSuspend(SourceFile:747)
```

O carregamento de tiles do MapsGL roda em `Dispatchers.Default` e chama `MapboxMap.executeOnRenderThread` dessa mesma thread. O `ThreadChecker` do Mapbox só lança a exceção quando duas condições valem: a meta-data `com.mapbox.maps.ThreadChecker` não está em `false` e o `BuildConfig.DEBUG` do app é verdadeiro. No release, ele retorna sem checar nada. O `AndroidManifest.xml` declara a meta-data com `false`, o que deixa o debug igual ao release. O Mapbox ainda registra `CameraManager::getCameraState called from a thread that is not owning the object` no logcat. O defeito é do MapsGL e vale reportar no repositório do SDK.

## Recursos depois de sair da tela

Os testes no emulador seguiram este roteiro: 16 entradas e saídas da aba Mapa, quatro rotações com o vento ligado e um heap dump com o mapa fora da tela.

- As threads subiram de 86 para 115 durante as trocas, e voltaram a 62 depois de 70 s parado. As threads extras eram do pool de `Dispatchers.Default` e `IO`, que mantém threads ociosas por 60 s.
- As duas threads `mapsgl-encoded-` não crescem com novas entradas no mapa.
- O PSS caiu de 338 MB para 239 MB depois das trocas.
- O heap dump com o mapa fora da tela tem uma `MapView` e um `MapboxMapController`, e não 16. Campos estáticos do próprio MapsGL guardam a referência a essa instância, como o `com.xweather.mapsgl.anim.Timeline.v0` e dois singletons ofuscados. Cada novo controller sobrescreve esses campos, então fica no máximo uma instância presa até a próxima abertura do mapa. A `MapView` dessa instância já passou pelo `onDestroy`, então o GL e as threads de render já foram liberados.
- Ao sair da tela, o MapsGL ainda termina trabalho em andamento sobre o estilo destruído, e o Mapbox registra avisos `should not be stored and used after MapView is destroyed`. Os avisos param logo depois.

## Credenciais e log

O MapsGL recebe o `client_id` e o `client_secret` da Xweather. O teste conferiu o logcat no uso normal e com a rede desligada, enquanto o mapa pedia tiles. Nenhuma linha continha `client_secret` nem o valor do segredo.

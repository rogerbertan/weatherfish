# Adapters do WeatherRepository: SDK, REST e Open-Meteo

O app tem quatro implementações do `WeatherRepository`, escolhidas pelo `DataSource` que o `DataModule` provê: `Fake`, `XweatherSdk`, `XweatherRest` e `OpenMeteo`. Trocar de adapter é trocar uma linha. Os números abaixo foram medidos em 2026-11-24. O "antes" é o commit `472dae9`.

## Linhas

| Adapter | Arquivos próprios | Linhas |
| --- | --- | --- |
| Fake | `FakeWeatherRepository`, `FakeJson` | 69 |
| SDK Aeris | `XweatherWeatherRepository`, `AerisMapping`, `XweatherSdk` | 171 |
| REST Xweather | `XweatherRestRepository`, `HttpText` | 101 |
| Open-Meteo | `OpenMeteoWeatherRepository`, `OpenMeteoMapping`, `OpenMeteoResponses` | 234 |
| Geocodificação reversa do Open-Meteo | `ReverseGeocoder`, `AndroidReverseGeocoder` | 51 |

Os três adapters da Xweather também usam o `XweatherResponses.kt`, com 63 linhas, e o `XweatherMapping.kt`, com 142. O REST não precisa de um mapping próprio. Ele decodifica o JSON com as mesmas classes `*Response` e os mesmos `parse*` do fake. O SDK precisa do `AerisMapping`, porque o Aeris entrega os models dele, preenchidos pela Gson. O Open-Meteo é o maior porque tem formato próprio: arrays paralelos por seção, códigos WMO com descrição em português e direção do vento em graus.

## APK

| | Antes | Depois | Delta |
| --- | --- | --- | --- |
| Debug | 191.356.589 B | 194.719.079 B | +3.362.490 B, ou +1,8% |
| Release | 120.464.151 B | 123.064.180 B | +2.600.029 B, ou +2,2% |

O release cresce quase tanto quanto o debug porque o `packageScope` do R8 só otimiza `androidx`, `kotlin` e `kotlinx`. O código do Ktor e do OkHttp entra inteiro.

## Dependências do debugRuntimeClasspath

```diff
+\--- io.ktor:ktor-client-okhttp:3.6.1
+     +--- io.ktor:ktor-client-core, ktor-http, ktor-http-cio, ktor-network, ktor-utils, ktor-io
+     +--- org.jetbrains.kotlinx:kotlinx-io-core:0.9.1
+     +--- org.slf4j:slf4j-api:2.0.19
+     \--- com.squareup.okhttp3:okhttp:5.5.1
-     com.squareup.okhttp3:okhttp:4.10.0
+     com.squareup.okhttp3:okhttp:4.10.0 -> 5.5.1
-     com.squareup.okio:okio:3.17.0
+     com.squareup.okio:okio:3.17.0 -> 3.18.2
-     androidx.startup:startup-runtime:1.1.1
+     androidx.startup:startup-runtime:1.1.1 -> 1.2.0
```

O `com.mapbox.common` já trazia o OkHttp 4.10. O Ktor sobe esse OkHttp para a 5.5.1, que vira a versão do Mapbox também. Com a 5.5.1, os tiles do mapa base continuam carregando no emulador. O `slf4j-api` vem sem implementação, então o SLF4J só escreve um aviso no stderr e não loga nada.

## Erros e headers

| | SDK Aeris | REST Xweather | Open-Meteo |
| --- | --- | --- | --- |
| Status HTTP | Escondido. Qualquer 4xx vira `IOException` | Visível | Visível |
| Cota esgotada, 429 `maxhits` | `Network`, "Sem conexão" | `QuotaExceeded`, "Limite de consultas atingido" | `QuotaExceeded` |
| Chave inválida, 401 `invalid_client` | `Network` | `Unauthorized` | Não tem chave |
| Headers `x-ratelimit-*` | Inacessíveis | Logados com `Log.d` | Não manda |
| Segredo no log | O Aeris loga a URL com o `client_secret` se o log não estiver desligado | Nunca. O adapter não usa o plugin `Logging` e só loga os valores dos headers | Não tem segredo |

A Xweather manda um `content-type` malformado, `application/json;  charset=utf-8; x-cost-v1=error||0`. Por isso os adapters REST não usam o `ContentNegotiation`. Eles leem o corpo com `bodyAsText()` e decodificam com kotlinx.serialization.

O `parseOrThrow` decide pelo status antes de decodificar. Com status de erro, o REST da Xweather usa o `error.code` do corpo, para um 400 manter o código da API. Quando o corpo não traz um código, o `errorForStatus` traduz o status. Assim, um 429 do Open-Meteo na busca vira `QuotaExceeded`, e não uma lista vazia.

Os headers que a Xweather manda são o `x-ratelimit-limit-minute`, o `x-ratelimit-remaining-minute` e o `x-ratelimit-reset-minute`. No emulador, o log fica assim:

```
D XweatherRest: rate limit 99/100 per minute, reset Wed, 25 Nov 2026 01:25:00 GMT
```

## Cancelamento

O `XweatherWeatherRepository` roda o `executeSyncTask()` dentro de `withContext(Dispatchers.IO)`. Essa é uma chamada bloqueante do `HttpURLConnection`, e o cancelamento de uma coroutine não interrompe a thread. Quando a tela é cancelada, a coroutine só termina depois que o request acaba, e a thread do `Dispatchers.IO` fica presa até lá.

Com o Ktor e o OkHttp, o cancelamento da coroutine cancela o `Call` do OkHttp, que fecha o socket. O teste `should cancel the http request when the caller is cancelled`, do `XweatherRestRepositoryTest`, mostra o request recebendo o cancelamento.

No app, a diferença só aparece na busca:
- **Clima.** O `CachingWeatherRepository` roda as condições e as previsões num `async` do `@ApplicationScope`, e quem chama só espera o resultado. Cancelar a tela cancela a espera, mas não o request, que termina e fica no cache. Isso vale para os quatro adapters.
- **Busca.** A busca e o `placeAt` não passam pelo cache. O `mapLatest` do `SearchViewModel` cancela a busca anterior quando o termo muda. Com o Ktor, o request anterior é cancelado de verdade. Com o SDK, ele continua até o fim numa thread do `Dispatchers.IO`, e o resultado é descartado.

## Open-Meteo

- Não é necessário utilizar uma chave de API. O plano gratuito para uso não comercial permite até 10.000 requisições por dia. Ao ultrapassar esse limite, a API retorna o erro 429.
- A busca usa o `geocoding-api.open-meteo.com` com `language=pt`. Os nomes chegam com acento, como `São Paulo`, e o termo não precisa do `foldForSearch`.
- Não tem geocodificação reversa. O `placeAt` pega o nome, a região e o país do `Geocoder` do Android, em pt-BR, e o fuso de um forecast sem variáveis. Quando o `Geocoder` não responde, o `placeAt` lança `NotFound`, e a cidade ativa continua a anterior. No emulador, a localização do aparelho volta como "Cocal do Sul".
- O `dailyForecast` usa o `timeformat` padrão, que devolve a data local como `2026-11-24`. O `currentWeather` e o `hourlyForecast` usam `unixtime`.

# Spike do Aeris Core 2.7.1

Os requests rodaram em 2026-11-21 contra `https://api.aerisapi.com`, com `curl` no host e com o SDK no emulador.

## Respostas da API

| Caso | HTTP | Corpo |
| --- | --- | --- |
| `/conditions/-23.5475,-46.63611` | 200 | `success:true`, um item com um período |
| Secret errado | 401 | `success:false`, `invalid_client` |
| REFERER de outro namespace | 200 | `success:true`, porque a chave atual não restringe namespace |
| `/conditions/zzzzqqq` | 200 | `success:false`, `invalid_location` |
| `/places/search?query=name:^são paulo` | 200 | `success:true`, `warn_no_data`, lista vazia |
| `/places/search?query=name:^sao paulo&sort=pop:-1` | 200 | São Paulo primeiro, com população de 12,4 milhões |

## Campos

- No `/conditions`, todos os campos que o domínio usa vieram preenchidos em São Paulo, de dia, e em Tóquio, de noite. À noite o `uvi` vem `0`, não nulo.
- No `/forecasts?filter=1hr`, `tempC`, `minTempC` e `maxTempC` vêm com o mesmo valor. O `weatherPrimaryCoded` tem a forma `:VL:RW`.
- No `/forecasts?filter=day`, o `tempC` vem `null`. `minTempC`, `maxTempC` e `dateTimeISO` vêm preenchidos, e o horário do `dateTimeISO` é `07:00-03:00`.
- No `/places/search`, os nomes vêm sem acento, como `Sao Paulo`. Para o Brasil, o `stateFull` vem vazio.
- O campo `weather` traz a descrição em inglês. O fake tem descrições em português.

## SDK

- Numa falha de rede, o SDK devolve `error.code = "IOException"` e põe a mensagem da exceção na `description`.
- Num HTTP 4xx, o `HttpURLConnection` lança `FileNotFoundException` no `getInputStream`, e o SDK devolve o mesmo `IOException`.
- A mensagem dessa `FileNotFoundException` é a URL do request. Por isso a `description` contém o `client_id` e o `client_secret`.
- O SDK não expõe o status HTTP, então uma chave inválida chega como erro de rede. Para o caso de chaves vazias, o adapter lança `Unauthorized` antes de fazer o request.
- Quando a resposta não é JSON válido, o `NetworkUtils.getJSONZipped` engole a `JSONException` e o `executeSyncTask()` devolve `null`.
- O `Logger` do SDK começa no nível `VERBOSE` e grava no logcat a URL dos requests que falham, com o `client_secret`. O `XweatherSdk.init()` desliga esse log com `LogLevel.setLoggingLevel(LogLevel.NONE)`.
- O `executeSyncTask()` roda na thread de quem chamou, que no spike era a `spike-bg`.
- O callback de `AerisCommunicationTask.execute()` chega na thread `main`. A regra `ThreadConstraint` do lint exige que o próprio `execute()` seja chamado na main thread.
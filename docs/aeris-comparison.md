# Antes e depois do Aeris Core

Os dados apresentados a seguir foram extraídos do APK de debug, do Manifest mesclado e da configuração `:app:dependencies --configuration debugRuntimeClasspath`. Para comparação, o cenário anterior corresponde ao commit `7be32b9`, enquanto o cenário atual representa o commit desta etapa.

## APK debug

| | Antes | Depois | Delta |
| --- | --- | --- | --- |
| Tamanho | 63.750.741 B | 65.376.210 B | +1.625.469 B, ou +2,5% |
| Arquivos | 177 | 290 | +113 |

Os 113 novos arquivos correspondem aos recursos incluídos no AAR: 112 drawables de ícones meteorológicos, um arquivo `menu` e um `values`, totalizando aproximadamente 0,9 MB. O restante do aumento de tamanho está relacionado ao `classes.jar` do SDK, com 250 KB, e à Gson. Após aplicar o resource shrinking, o APK de release ficou com 3,5 MB.

## Merged manifest

Só entraram duas permissões novas:

```diff
+    <uses-permission android:name="android.permission.INTERNET" />
+    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

O manifest do Aeris também pede `WRITE_EXTERNAL_STORAGE`, `READ_GSERVICES`, `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` e `uses-feature glEsVersion required=true`. Ele ainda traz `READ_EXTERNAL_STORAGE` de forma implícita, por causa do `targetSdk` antigo da lib. O `AndroidManifest.xml` do app remove todos esses itens com `tools:node="remove"`.

## Dependências do debugRuntimeClasspath

```diff
+--- com.aerisweather:aeris-core-lib:2.7.1
\--- com.google.code.gson:gson:2.15.0
     \--- com.google.errorprone:error_prone_annotations:2.50.0
```

O POM do Aeris declara no escopo `compile` o `play-services-maps:18.0.2`, o `play-services-basement`, o `androidx.test.ext:junit:1.1.3`, o `androidx.test:core` e o `androidx.test:monitor`. Com o `isTransitive = false`, nenhum deles entra no app.
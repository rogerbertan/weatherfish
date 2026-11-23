# Antes e depois do MapsGL

A Fase 6 troca o placeholder da aba Mapa por um mapa Mapbox 11.15.3, `ndk27`, com as camadas de temperatura e de vento em partículas do MapsGL 1.8.0. O "antes" é o commit `5b8015a`. Os números vêm do APK debug, do APK release sem assinatura, do merged manifest e do `:app:dependencies --configuration debugRuntimeClasspath`.

## APK

|                     | Antes        | Depois        | Delta                     |
|---------------------|--------------|---------------|---------------------------|
| Debug               | 101.250.452 B | 192.284.645 B | +91.034.193 B, ou +89,9%  |
| Release             | 32.011.183 B | 120.464.143 B | +88.452.960 B, ou +276,3% |
| Arquivos no debug   | 693          | 816           | +123                      |
| Arquivos no release | 421          | 651           | +230                      |

O release, por tipo de conteúdo:

|                                   | Antes        | Depois       | Delta         |
|-----------------------------------|--------------|--------------|---------------|
| `libmapbox-maps.so` e `libmapbox-common.so`, 4 ABIs | 0 B | 69.465.688 B | +69.465.688 B |
| `libc++_shared.so`, 4 ABIs        | 0 B          | 4.672.964 B  | +4.672.964 B  |
| Outras `.so`                      | 235.844 B    | 2.302.268 B  | +2.066.424 B  |
| `libbarhopper_v3.so`, 4 ABIs      | 20.222.808 B | 20.222.808 B | 0 B           |
| `classes*.dex`                    | 9.902.252 B  | 21.537.124 B | +11.634.872 B |
| `assets/`                         | 892.937 B    | 1.028.204 B  | +135.267 B    |
| `res/`                            | 152.043 B    | 254.026 B    | +101.983 B    |
| `resources.arsc`                  | 339.228 B    | 516.180 B    | +176.952 B    |

Quase todo o aumento é nativo, porque as `.so` ficam sem compressão no APK. O Mapbox traz o `libmapbox-maps.so`, o `libmapbox-common.so` e o `libc++_shared.so`. O MapsGL traz o `libnativelib.so`, que responde por quase todo o aumento das "outras `.so`". Em `arm64-v8a`, as quatro bibliotecas somam 19.843.104 B. Um App Bundle na Play Store entrega só a ABI do aparelho, então um celular `arm64` baixa uns 20 MB a mais, e não 76 MB. Os `assets/` novos são os shaders GLSL e os sprites SDF do MapsGL.

O dex cresce 11,6 MB, de 9,9 MB para 21,5 MB. O `packageScope` do R8 cobre só `androidx`, `kotlin` e `kotlinx`, então o Mapbox, o MapsGL e o Cronet entram sem shrinking.

## Merged manifest

```diff
+    <uses-feature
+        android:glEsVersion="0x00030000"
+        android:required="true" />
+    <uses-feature
+        android:name="android.hardware.wifi"
+        android:required="false" />
+    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
+        <meta-data
+            android:name="com.mapbox.maps.ThreadChecker"
+            android:value="false" />
+            <meta-data
+                android:name="com.mapbox.maps.loader.MapboxMapsInitializer"
+                android:value="androidx.startup" />
+            <meta-data
+                android:name="com.mapbox.common.MapboxSDKCommonInitializer"
+                android:value="androidx.startup" />
+        <receiver
+            android:name="com.mapbox.common.location.LocationUpdatesReceiver"
+            android:enabled="true"
+            android:exported="false" >
+        <service
+            android:name="com.mapbox.common.LifecycleService"
+            android:exported="false" />
```

O `ACCESS_NETWORK_STATE` já existia e só mudou de lugar. O `ACCESS_WIFI_STATE` vem do Mapbox Common, que o usa para medir a conectividade. As duas são permissões normais, concedidas na instalação, sem diálogo. O MapsGL exige OpenGL ES 3.0 com `required="true"`, e a Play Store esconde o app de aparelhos sem ES 3.0. As partículas de vento precisam de ES 3.1, e o MapsGL recusa a camada em runtime num aparelho sem esse suporte. O receiver de localização do Mapbox não é exportado, e o app não usa a localização do Mapbox. A meta-data `ThreadChecker` é do app, e o motivo está em `docs/mapsgl-spike.md`.

## Dependências do debugRuntimeClasspath

```diff
+--- com.github.vaisala-xweather:mapsgl-android-sdk:v1.8.0
+--- com.mapbox.maps:android-ndk27:11.15.3
\--- com.mapbox.extension:maps-compose-ndk27:11.15.3
```

As transitivas novas mais relevantes:

- `com.mapbox.common:common-ndk27:24.15.3`, `android-core-ndk27`, `base-ndk27`, `maps-style-ndk27`, `maps-localization-ndk27`, `mapbox-android-gestures` e `mapbox-sdk-geojson`.
- `com.google.android.gms:play-services-cronet:18.1.0`, que o Mapbox Common usa para HTTP.
- `org.maplibre:earcut4j:3.0.0`, que o MapsGL usa para triangular polígonos.
- `androidx.constraintlayout:constraintlayout:2.0.1` e `androidx.lifecycle:lifecycle-livedata-ktx`, pedidos pelo MapsGL.
- `androidx.compose.material:material:1.12.1`, o Material 2, trazido pelo `maps-compose`. A UI do app continua só em Material 3.

## Release

O token do Mapbox é um `resValue` chamado `mapbox_access_token`, e o Mapbox o lê pelo nome em runtime. O resource shrinking do release apagava o recurso, porque nenhum código o referencia. O `res/raw/keep.xml` mantém o recurso com `tools:keep`. O APK release assinado com a chave de debug abriu o mapa com temperatura e vento, sem erro de R8.

## Cota da Xweather

Pendente: usar o mapa por 1 min, com pan, zoom e vento ligado, e anotar aqui o consumo no painel da Xweather.

# Antes e depois do QR

Os números abaixo vêm do APK debug, do APK release sem assinatura, do merged manifest e do `:app:dependencies --configuration debugRuntimeClasspath`. O "antes" é o commit `cb1e454` com o `PlaceQrCodec`, que é Kotlin puro e não traz dependência. O "depois" soma o ZXing core 3.5.4 e o `play-services-code-scanner` 16.1.0.

## APK

|                   | Antes        | Depois       | Delta                   |
|-------------------|--------------|--------------|-------------------------|
| Debug             | 67.101.438 B | 68.941.489 B | +1.840.051 B, ou +2,7%  |
| Release           | 5.129.157 B  | 6.517.150 B  | +1.387.993 B, ou +27,1% |
| Arquivos no debug | 347          | 360          | +13                     |

Os 13 arquivos novos são os `.properties` de versão de cada dependência nova, mais o `.version` do `exifinterface`. Quase todo o aumento está no `classes.dex`. O `packageScope` do R8 só cobre `androidx`, `kotlin` e `kotlinx`, então nem o ZXing nem o ML Kit passam pelo shrinking. O jar do ZXing core tem 610 KB. O release cresce mais em proporção porque parte de um APK pequeno.

O código do scanner não fica no APK. Ele é um módulo do Play Services, que o aparelho baixa na primeira leitura. Numa instalação pela Play Store, o meta-data `barcode_ui` faz esse download junto com o app.

## Merged manifest

O scanner não trouxe permissão nova. O `ACCESS_NETWORK_STATE` que o ML Kit pede já vinha do Aeris. A câmera fica com o Play Services, então o app não pede a permissão `CAMERA`. O manifest ganhou estes componentes:

```diff
+        <meta-data
+            android:name="com.google.mlkit.vision.DEPENDENCIES"
+            android:value="barcode_ui" />
+        <activity
+            android:name="com.google.mlkit.vision.codescanner.internal.GmsBarcodeScanningDelegateActivity"
+            android:exported="false"
+            android:screenOrientation="portrait" />
+        <service android:name="com.google.mlkit.common.internal.MlKitComponentDiscoveryService" ... />
+        <provider
+            android:name="com.google.mlkit.common.internal.MlKitInitProvider"
+            android:authorities="dev.rogerbertan.weatherfish.mlkitinitprovider"
+            android:initOrder="99" />
+        <service android:name="com.google.android.datatransport.runtime.backends.TransportBackendDiscovery" ... />
+        <service android:name="com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService" ... />
+        <receiver android:name="com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver" ... />
```

O `MlKitInitProvider` é um `ContentProvider` e roda no startup do app, antes do `Application.onCreate`. Os componentes de `datatransport` são a telemetria do Firebase. O ML Kit usa esses componentes para mandar estatísticas de uso ao Google.

## Dependências do debugRuntimeClasspath

```diff
+--- com.google.android.gms:play-services-code-scanner:16.1.0
|    +--- com.google.android.datatransport:transport-api:2.2.1
|    +--- com.google.android.datatransport:transport-backend-cct:2.3.3
|    +--- com.google.android.datatransport:transport-runtime:2.2.6
|    +--- com.google.firebase:firebase-components:16.1.0
|    +--- com.google.firebase:firebase-encoders:16.1.0
|    +--- com.google.firebase:firebase-encoders-json:17.1.0
|    +--- com.google.mlkit:barcode-scanning-common:17.0.0
|    |    \--- com.google.mlkit:vision-common:17.0.0
|    |         +--- androidx.exifinterface:exifinterface:1.0.0
|    |         \--- com.google.android.odml:image:1.0.0-beta1
|    \--- com.google.mlkit:common:18.9.0
+--- com.google.zxing:core:3.5.4
```

O `play-services-base`, o `play-services-basement` e o `play-services-tasks` também aparecem na árvore do scanner, mas já vinham do `play-services-location`. O ZXing core não tem dependências.

## Linhas de código

|                              | Linhas   |
|------------------------------|----------|
| Produção, arquivos novos     | 332      |
| Produção, arquivos alterados | +38 / −2 |
| Testes, arquivos novos       | 478      |
| `ArchitectureTest`           | +13      |

Os arquivos novos de produção são o `PlaceQrCodec` (68), o `QrScanner` (6), o `GmsQrScanner` (31), o `QrCode` (49), o `ShareViewModel` (83) e a `ShareScreen` (95). A `ShareScreen` já existia como placeholder de 13 linhas e entra aqui inteira. As linhas alteradas são do `DataModule`, do catálogo, do build, do manifest e das strings.

## O que o scanner devolve

Os resultados abaixo vêm do emulador `sdk_gphone64_arm64`, com o Play Services atual:

| Ação          | Resultado da `Task`                                                      |
|---------------|--------------------------------------------------------------------------|
| Lê um código  | `onSuccess` com o `Barcode`                                              |
| Toca no X     | `onCanceled`                                                             |
| Aperta voltar | `onFailure` com `MlKitException` `INTERNAL` (13), "Failed to scan code." |

O voltar não devolve o `CODE_SCANNER_CANCELLED`, de código 201, que a API documenta. O resultado é o mesmo quando o client usa o context de uma Activity. O `GmsQrScanner` trata o 201 e o 13 como fechamento e devolve `null`. Os outros códigos, como o 200 do `CODE_SCANNER_UNAVAILABLE`, viram `QrScanException`.

O `GmsBarcodeScanning.getClient` funciona com o application context. Por isso o `DataModule` cria o client sem depender de uma Activity.
# Fase 4 contra Fase 5 na leitura do QR

A Fase 4 lê o QR com o Google code scanner, uma tela pronta que roda no Play Services. A Fase 5 lê o mesmo QR com um componente do próprio app, feito com CameraX 1.7.0 e ML Kit `barcode-scanning` 17.3.0, na versão bundled. As duas variantes ficam lado a lado na aba Compartilhar.

O "antes" é o commit `8e63f37`, o fim da Fase 4. Os números vêm do APK debug, do APK release sem assinatura, do merged manifest e do `:app:dependencies --configuration debugRuntimeClasspath`. Cada medida do APK debug parte de um empacotamento do zero, porque o empacotamento incremental deixa espaço morto no zip quando uma entrada some. Por isso o baseline difere do "depois" de `docs/qr-comparison.md`, que é de antes dos últimos commits da Fase 4.

## Resumo

|                       | Fase 4 · Google code scanner                                                    | Fase 5 · CameraX + ML Kit bundled                                                                                                   |
|-----------------------|---------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| Linhas de produção    | 37 do scanner, sendo 31 do `GmsQrScanner` e 6 do `QrScanner`, mais o `scan()` do ViewModel | 419 novas, sendo 225 da `CameraScanScreen`, 96 do `QrCameraView`, 56 do `CameraScanViewModel` e 42 do `ScanAreaOverlay`, mais 78 adicionadas e 6 removidas em arquivos existentes |
| Permissões            | Nenhuma. A câmera é do Play Services                                            | `CAMERA` em runtime, com os estados negada e bloqueada, e `uses-feature camera` com `required="false"`                               |
| APK release           | O scanner não entra no APK. É um módulo baixado pelo Play Services             | +25.423.665 B, ou +386,9%. O unbundled fica em +3.979.691 B, ou +60,6%                                                               |
| Onde o modelo roda    | No processo do Play Services                                                    | No processo do app. No unbundled, o modelo vem do Play Services                                                                      |
| Lifecycle             | Nada a gerenciar. O app só recebe a `Task`. A Activity do scanner só fica em retrato | O app cuida de tudo: abrir e fechar a câmera, rotação, background e saída da tela                                                   |
| Customização          | Só formatos, auto zoom e entrada manual, nas `GmsBarcodeScannerOptions`         | O app define o overlay, o texto, o tamanho da área, os frames de confirmação e a escolha entre vários códigos no frame. A tela fica dentro da navegação do app |
| Arquitetura           | Fica atrás da interface `QrScanner`, em `data/scanner`. O ViewModel usa um mock nos testes | O componente é de UI. O `ArchitectureTest` abre uma exceção para o `QrCameraView` importar ML Kit e CameraX                         |
| O que o ViewModel testa | A tradução dos resultados da `Task`                                          | A confirmação por frames e a parada da análise, sem câmera                                                                           |

## APK

|                     | Baseline     | Bundled       | Delta bundled            | Unbundled    | Delta unbundled          |
|---------------------|--------------|---------------|--------------------------|--------------|--------------------------|
| Debug               | 68.847.635 B | 101.233.612 B | +32.385.977 B, ou +47,0% | 79.756.927 B | +10.909.292 B, ou +15,8% |
| Release             | 6.570.670 B  | 31.994.335 B  | +25.423.665 B, ou +386,9% | 10.550.361 B | +3.979.691 B, ou +60,6%  |
| Arquivos no debug   | 360          | 693           | +333                     | 685          | +325                     |
| Arquivos no release | 191          | 421           | +230                     | 412          | +221                     |

O release, por tipo de conteúdo:

|                               | Baseline    | Bundled      | Unbundled   |
|-------------------------------|-------------|--------------|-------------|
| `classes*.dex`                | 6.023.296 B | 9.896.108 B  | 9.582.484 B |
| `libbarhopper_v3.so`, 4 ABIs  | 0 B         | 20.222.808 B | 0 B         |
| Outras `.so`                  | 67.480 B    | 235.844 B    | 235.844 B   |
| Modelos `assets/mlkit_barcode_models` | 0 B | 880.888 B    | 0 B         |
| `res/`                        | 62.800 B, 60 arquivos | 152.043 B, 252 arquivos | 152.043 B, 252 arquivos |
| `resources.arsc`              | 271.084 B   | 338.764 B    | 338.764 B   |

Quase todo o peso do bundled é o decodificador nativo `libbarhopper_v3.so`, que vem em quatro ABIs: 4.946.720 B em `arm64-v8a`, 3.244.440 B em `armeabi-v7a`, 6.122.368 B em `x86` e 5.909.280 B em `x86_64`. Um App Bundle na Play Store entrega só a ABI do aparelho, então um celular `arm64` baixa uns 5 MB de biblioteca, e não 20 MB. As outras `.so` novas são da CameraX, como a `libimage_processing_util_jni.so`.

O dex cresce cerca de 3,6 MB nas duas variantes. O `packageScope` do R8 cobre `androidx`, então a CameraX passa pelo shrinking, mas o ML Kit, a Guava e o Media3 não passam. O dex do bundled tem 313.624 B a mais que o do unbundled, provavelmente o código do ML Kit que, no unbundled, fica no módulo do Play Services.

## Merged manifest

```diff
+    <uses-permission android:name="android.permission.CAMERA" />
+    <uses-feature
+        android:name="android.hardware.camera"
+        android:required="false" />
+        <service
+            android:name="androidx.camera.core.impl.MetadataHolderService"
+            android:enabled="false"
+            android:exported="false" >
+            <meta-data
+                android:name="androidx.camera.core.impl.MetadataHolderService.DEFAULT_CONFIG_PROVIDER"
+                android:value="androidx.camera.camera2.Camera2Config$DefaultProvider" />
+        </service>
+            <meta-data
+                android:name="com.google.firebase.components:com.google.mlkit.vision.barcode.internal.BarcodeRegistrar"
+                android:value="com.google.firebase.components.ComponentRegistrar" />
```

A permissão `CAMERA` implica a feature `android.hardware.camera` como obrigatória. Sem o `required="false"`, a Play Store esconderia o app de aparelhos sem câmera traseira. O resto do app funciona sem ela. Nesses aparelhos, a câmera do app usa a frontal. Sem câmera nenhuma, a Compartilhar esconde o botão, e a tela, aberta pelo link, mostra uma mensagem. O `MetadataHolderService` vem desligado e só guarda a configuração padrão da CameraX. O `BarcodeRegistrar` entra no `MlKitComponentDiscoveryService` que a Fase 4 já tinha trazido. O manifest do unbundled é igual ao do bundled.

## Dependências do debugRuntimeClasspath

Os módulos novos, já com as versões resolvidas:

```diff
+androidx.camera:camera-camera2:1.7.0
+androidx.camera:camera-camera2-pipe:1.7.0
+androidx.camera:camera-common:1.7.0
+androidx.camera:camera-core:1.7.0
+androidx.camera:camera-lifecycle:1.7.0
+androidx.camera:camera-mlkit-vision:1.7.0
+androidx.camera:camera-video:1.7.0
+androidx.camera:camera-view:1.7.0
+androidx.camera.featurecombinationquery:featurecombinationquery:1.7.0
+androidx.camera.viewfinder:viewfinder-core:1.5.1
+androidx.media3:media3-common:1.9.0
+androidx.media3:media3-container:1.9.0
+androidx.media3:media3-muxer:1.9.0
+com.google.guava:guava:33.3.1-android
+com.google.guava:failureaccess:1.0.2
+androidx.appcompat:appcompat:1.6.1
+androidx.appcompat:appcompat-resources:1.6.1
+com.google.mlkit:barcode-scanning:17.3.0
+com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1
+com.google.mlkit:vision-interfaces:16.3.0
 com.google.mlkit:common:18.9.0 -> 18.11.0
 com.google.mlkit:vision-common:17.0.0 -> 17.3.0
 com.google.android.gms:play-services-basement:18.9.0 -> 18.10.0
```

A lista também ganha bibliotecas pequenas que vêm junto com essas, como `drawerlayout`, `vectordrawable` e `atomicfu`. Três dependências chegam sem o app usar:

- O `camera-view` depende do `camera-video`, porque o `CameraController` também grava vídeo. O `camera-video` traz o Media3, e o Media3 traz a Guava inteira.
- O `com.google.mlkit:common` 18.11.0, que o `barcode-scanning` puxa, depende do `appcompat`. O app tinha removido o `appcompat`, e ele volta com 98 recursos `abc_`.
- O `barcode-scanning` bundled depende do `play-services-mlkit-barcode-scanning`, que é a variante unbundled. Ele usa as classes de API dessa variante e acrescenta o modelo nativo.

## Confirmação por frames

O `MlKitAnalyzer` entrega o resultado de cada frame ao `CameraScanViewModel`. A partir daí:

- Um código só conta quando aparece em 3 frames seguidos, o `CONFIRMATION_FRAMES`. Um frame sem código, ou com outro código, zera a contagem. No emulador, a análise rodou em 1280x960.
- Num frame com vários códigos, o ViewModel prefere o link `place`. Assim, a ordem dos resultados, que muda de um frame para outro, não zera a contagem.
- Quando o link `place` é confirmado, o `isAnalyzing` vira falso. O `QrCameraView` chama `clearImageAnalysisAnalyzer()`, e o ViewModel ignora os frames que ainda estavam a caminho.
- Um QR que não é link `place` mostra o snackbar uma vez por aparição na câmera. A análise continua.

## Lifecycle no emulador

O teste usou o emulador `sdk_gphone64_arm64`, com API 37. A coluna "Câmera aberta" conta os clientes do app no `dumpsys media.camera`.

| Ação                                         | Resultado                                                         | Câmera aberta |
|----------------------------------------------|-------------------------------------------------------------------|---------------|
| Abrir a tela sem a permissão                 | O sistema pede a permissão                                        | 0             |
| Girar com o pedido de permissão aberto       | Continua um pedido só                                             | 0             |
| Negar                                        | Estado negada, com o botão "Permitir câmera"                      | 0             |
| Negar de novo                                | Estado bloqueada, com o botão "Abrir configurações"               | 0             |
| Girar no estado bloqueada                    | O estado continua                                                 | 0             |
| Liberar nas configurações e voltar           | O `LifecycleResumeEffect` vê a permissão, e o preview abre        | 1             |
| Apertar Home                                 | A câmera fecha                                                    | 0             |
| Voltar pelos recentes                        | A câmera reabre                                                   | 1             |
| Girar para paisagem e voltar para retrato    | A câmera reabre em cada orientação e o estado do ViewModel continua | 1           |
| Abrir `weatherfish://share/scan` com o app aberto | O link chega pelo `onNewIntent` e abre a tela                | 1             |
| Sair da tela pelo voltar                     | A câmera fecha                                                    | 0             |

O `LifecycleCameraController` segue o `LocalLifecycleOwner`, que numa destination do Navigation é o `NavBackStackEntry`. Por isso a câmera fecha tanto no background quanto quando a tela sai da pilha. O `DisposableEffect` também chama `unbind()` quando o componente sai da composição.

O teste achou um defeito em paisagem. O `PreviewView` no modo `COMPATIBLE`, que usa `TextureView`, desenhava o preview fora dos próprios limites e cobria a barra do topo. O `clipToBounds` no container corrigiu. O `QrCameraView` usa o modo `COMPATIBLE` porque a `SurfaceView` do modo padrão não acompanha o fade da transição do Navigation.

## Bundled contra unbundled

O unbundled troca o `com.google.mlkit:barcode-scanning` pelo `com.google.android.gms:play-services-mlkit-barcode-scanning`. O código não muda, porque as duas variantes usam a mesma API `BarcodeScanning`. O release cai de +25,4 MB para +4,0 MB, porque o `libbarhopper_v3.so` e os modelos saem do APK.

No emulador, o unbundled leu os frames de primeira, sem esperar download. O log mostra o modelo carregando do módulo `TfliteDynamite` do Play Services. O módulo de barcode já estava no aparelho. Não verifiquei o que o baixou. Num aparelho sem o módulo, o primeiro uso espera o download. Para baixar na instalação, o meta-data `com.google.mlkit.vision.DEPENDENCIES` precisaria do valor `barcode_ui,barcode`.

O app ficou com o bundled, que é o que a Fase 5 pede. O unbundled é a escolha mais leve se o app já depende do Play Services, como o WeatherFish depende pela localização.

## Ainda não verificado

A câmera traseira do emulador é a virtual scene, e ela não mostra um QR sem configuração. O pôster só entra com o emulador reiniciado com `-virtualscene-poster wall=<imagem>`, e depois é preciso mover a câmera virtual até ele. Por isso ninguém testou ainda a leitura de ponta a ponta, em que as duas variantes leem o mesmo QR e abrem o diálogo de importação. O log confirma que o ML Kit carrega o modelo e analisa os frames da câmera. A lógica de confirmação está coberta pelo `CameraScanViewModelTest`.

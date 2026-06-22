# DevBrowser - Android DevTools Browser

## Concept & Vision
DevBrowser é um navegador Android minimalista para desenvolvedores mobile, oferecendo DevTools completos diretamente no dispositivo. Interface escura com toques de cor para destacar elementos interativos.

## Features

### Core Browser
- WebView com JavaScript habilitado
- Navegação básica (URL bar, back/forward/refresh)
- Gerenciamento de abas (até 10 abas)

### DevTools (embedded)
- **Console**: Exibe logs, errors, warnings do JavaScript
- **Elements**: Inspetor DOM simplificado
- **Network**: Monitor de requests (interceptação de tráfego)
- **Application**: Cookies, LocalStorage, SessionStorage viewer

### Cookie Management
- Visualizar todos os cookies
- Copiar cookie (nome, valor, domínio)
- Deletar cookie específico
- Deletar todos os cookies

## Tech Stack
- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34
- **Architecture**: MVVM
- **UI**: Jetpack Compose
- **DI**: Hilt
- **Networking**: OkHttp + WebSocket (para DevTools Protocol)

## Dependencies
```gradle
implementation("androidx.webkit:webkit:1.8.0")
implementation("androidx.compose:compose-bom:2024.01.00")
implementation("com.google.dagger:hilt-android:2.50")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

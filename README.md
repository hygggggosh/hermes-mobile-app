# Hermes Mobile — Android Compose 前端

在 Termux 上跑 Hermes Agent，配一个 Android 原生前端。

## 当前状态

- 框架来源：`onezion12344/hermes-mobile`（Apache/MIT，63KB Kotlin），已在 `~/hermes-mobile-local/` 本地 fork
- 改动：包名 `com.termux.hermesmobile`、默认 URL `http://127.0.0.1:8642`、默认 model `MiniMax-M3`、provider `minimax_cn`、assets 内置 gateway key（loopback-only）
- 未推送、未构建

## 目录布局

```
hermes-mobile-local/
├── app/
│   ├── build.gradle.kts          (applicationId=com.termux.hermesmobile)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/hermes_defaults.json   (含 8642 + 真实 key)
│       └── java/com/termux/hermesmobile/
│           ├── HermesApplication.kt
│           ├── HermesForegroundService.kt
│           ├── MainActivity.kt
│           ├── network/                  (HermesApiClient, ConfigImporter, SettingsRepository, Models)
│           ├── ui/screens/               (ChatScreen, SettingsScreen, SetupWizard)
│           ├── ui/theme/                 (Color, Theme, Type)
│           └── viewmodel/ChatViewModel.kt
├── build.gradle.kts
├── gradle/wrapper/
├── gradlew
└── settings.gradle.kts
```

## 后端约定

- Hermes gateway 跑在 `127.0.0.1:8642`
- 端点：POST `/v1/chat/completions`，OpenAI 兼容，支持 SSE 流式
- Header：`Authorization: Bearer <key>`
- 当前 key：`VI7IxKay3liyWc76H7g43iFT8xWvg2MZD2S49-bp8uQ`（来自 `~/.hermes/config.yaml`，已硬编码进 assets，loopback only）

## 跑通流程

APK 还没编译，下一步三条路选一：

### 选项 A：推到 GitHub → Actions 云端构建

```bash
cd ~/hermes-mobile-local
gh auth login   # 一次性
# 用户仓库：hygggggosh/- → 新建一个 e.g. hermes-mobile-app
# 然后：
git init && git add .
git config user.email "hermes@local" && git config user.name "Hermes"
git commit -m "Initial fork from onezion12344/hermes-mobile, retargeted to Hermes gateway"
# 在 Actions workflow 里要加 gradle actions：android-actions/setup-android@v3 + ./gradlew assembleDebug
```

需要我加 workflow 文件就告诉我。

### 选项 B：本地 Gradle 构建（要 Java 17 + Android SDK）

Termux 上没 Android SDK。理论上可以装 SDK 但极重（约 5GB）。不推荐。

### 选项 C：丢给电脑本地 Android Studio 打开

把 `~/hermes-mobile-local/` 拷到电脑，用 Android Studio Hedgehog/Iguana 打开 → Sync → Run。前提是电脑上有 SDK 35。

## 验证后端可达

```bash
curl -s http://127.0.0.1:8642/health
# → {"status": "ok", "platform": "hermes-agent"}

curl -s http://127.0.0.1:8642/v1/chat/completions \
  -H "Authorization: Bearer VI7IxKay3liyWc76H7g43iFT8xWvg2MZD2S49-bp8uQ" \
  -H "Content-Type: application/json" \
  -d '{"model":"MiniMax-M3","messages":[{"role":"user","content":"hi"}],"stream":false,"max_tokens":20}'
```

## 关键设计决策

1. `DEFAULT_URL` 保留 `http://127.0.0.1:11434`（Ollama）作为 sentinel
   - `isConfigured() = serverUrl != DEFAULT_URL`
   - 真正的 Hermes URL 用常量 `HERMES_GATEWAY_URL = "http://127.0.0.1:8642"` 暴露
2. SetupWizard 启动时读 `hermes_defaults.json`，自动写入 8642+key
   - 用户首次启动直接进 ChatScreen（前提：Termux gateway 跑着）
   - 解析失败时回退默认的 `HermesConfig(...)`（仍含 8642+key）
3. API key 在 assets 里明文——只针对 127.0.0.1 loopback，无所谓
   - 真要外发/分享，把 key 改回空 string 即可
4. `ChatViewModel` 每次发消息都从 StateFlow 取最新 serverUrl/key，不需要重建 ViewModel
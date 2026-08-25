---
name: deploy-render
description: 協助使用者將 TalkWeb 即時通訊系統完整部署至 Render 雲端平台（含 Blueprint、Dockerfile 多階段構建、PostgreSQL 資料庫綁定與常見問題排查）。當使用者提到「部署到 Render」、「Render 部署」、「上線雲端」、「render.yaml 設定」或 Render 運行異常時使用。
---

# Deploy to Render (Render 雲端部署指南)

本技能提供將 **TalkWeb** 企業即時通訊系統部署至 **[Render](https://render.com/)** 雲端託管平台的完整流程、架構說明、自動化 Blueprint 設定與常見問題排查。

---

## 1. 部署架構概覽

TalkWeb 採用全端整合容器化架構：

```
                    ┌───────────────────────────────┐
                    │       Render Platform         │
                    │                               │
  Users (Web/App) ─►│  [Web Service: talkweb-app]   │
  HTTPS / WSS:443   │  ├─ Vue 3 SPA (Static UI)     │
                    │  └─ Spring Boot 3 (API/WS)    │
                    │              │                │
                    │              ▼                │
                    │  [PostgreSQL: talkweb-db]     │
                    │  └─ chatapp Schema            │
                    └───────────────────────────────┘
```

- **Runtime**：Docker (Alpine Linux + Temurin 17 JRE)
- **Database**：Managed PostgreSQL 14+ (`chatapp` Schema)
- **Protocols**：HTTP/REST API + WebSocket (STOMP / SockJS)

---

## 2. 核心設定檔與定義

### A. Render Blueprint (`render.yaml`)
專案根目錄中的 `render.yaml` 是 Render 的基礎架構即程式碼 (IaC) 定義，支援一鍵建立 Web Service 與 PostgreSQL 資料庫：

```yaml
services:
  - type: web
    name: talkweb-app
    runtime: docker
    dockerfilePath: ./Dockerfile
    plan: free
    region: singapore
    envVars:
      - key: EXISTING_DB_HOST
        fromDatabase:
          name: talkweb-db
          property: host
      - key: EXISTING_DB_PORT
        fromDatabase:
          name: talkweb-db
          property: port
      - key: DB_NAME
        fromDatabase:
          name: talkweb-db
          property: database
      - key: DB_USERNAME
        fromDatabase:
          name: talkweb-db
          property: user
      - key: DB_PASSWORD
        fromDatabase:
          name: talkweb-db
          property: password
      - key: JWT_SECRET
        value: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
      - key: SPRING_PROFILES_ACTIVE
        value: prod

databases:
  - name: talkweb-db
    databaseName: chatapp
    user: chatapp_user
    plan: free
    region: singapore
```

### B. 多階段 Dockerfile (`Dockerfile`)
- **Stage 1 (Node.js 20)**：編譯並打包 Vue 3 前端靜態檔案至 `dist/`。
- **Stage 2 (Maven 3.9 + JDK 17)**：將前端靜態檔案複製至 `resources/static`，並打包 Spring Boot 可執行 JAR。
- **Stage 3 (JRE 17 Alpine)**：以非 root 使用者執行，確保安全性與極速啟動。

---

## 3. 部署步驟 (SOP)

### 方法一：Blueprint 一鍵自動部署（推薦）

1. **推送最新程式碼至 GitHub**：
   ```bash
   git add .
   git commit -m "feat: prepare for render deployment"
   git push origin main
   ```
2. **進入 Render 控制台**：
   - 瀏覽並登入 [Render Dashboard](https://dashboard.render.com)。
3. **建立 Blueprint**：
   - 點擊右上角 **「New +」** -> 選擇 **「Blueprint」**。
   - 選擇對應的 GitHub 倉庫（例如 `gone0522/TalkWeb`）。
   - Render 會自動偵測並解析 `render.yaml`。
4. **點擊「Apply」**：
   - Render 會自動依序建立 `talkweb-db` (PostgreSQL) 與 `talkweb-app` (Web Service)。
   - 等待約 3-5 分鐘完成 Docker 構建與資料庫初始化。
5. **完成與訪問**：
   - 部署完成後，取得專屬網址（例如 `https://talkweb-app.onrender.com`）。

---

### 方法二：手動分步部署 (Manual Setup)

若不使用 Blueprint，可手動分步建立：
1. **建立資料庫**：
   - **New + -> PostgreSQL**。
   - Name: `talkweb-db`、Database: `chatapp`、User: `chatapp_user`、Region: `Singapore`。
2. **建立 Web 服務**：
   - **New + -> Web Service**，選擇 GitHub Repository。
   - Runtime 選擇 **Docker**，Dockerfile Path 填入 `./Dockerfile`。
   - 在 **Environment Variables** 新增：
     - `EXISTING_DB_HOST`：填入資料庫 Internal Database URL 之 Host
     - `EXISTING_DB_PORT`：`5432`
     - `DB_NAME`：`chatapp`
     - `DB_USERNAME`：資料庫使用者名稱
     - `DB_PASSWORD`：資料庫密碼
     - `JWT_SECRET`：`404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970`
     - `SPRING_PROFILES_ACTIVE`：`prod`

---

## 4. 預設帳號與初始化

- **預設管理員帳號**：`admin`
- **預設管理員密碼**：`admin123`
- **強制改密機制**：初次登入系統會自動彈出修改密碼視窗，引導設定新密碼。

---

## 5. 常見問題與排查手冊 (Troubleshooting)

### Q1: 免費方案實例冷啟動 (Spin down on idle)
- **現象**：若 15 分鐘無人存取，Render 免費實例會休眠。首次存取時網頁會載入約 30~50 秒。
- **處理**：此為 Render Free Plan 正常機制，等候後端喚醒即可。亦可使用外部 Uptime 服務（如 UptimeRobot）定期 Ping 健康檢查端點（`/api/users/me` 或 `/`）。

### Q2: 聊天發送出現 500 (欄位約束或 Schema 不一致)
- **檢查點**：確認資料庫 `messages` 表結構包含 `chat_type` 欄位。
- **修復**：後端 `schema.sql` 已內建自動遷移腳本，重新部署時會自動補齊並解除限制：
  ```sql
  ALTER TABLE chatapp.messages ALTER COLUMN chat_type DROP NOT NULL;
  ALTER TABLE chatapp.messages ALTER COLUMN chat_type SET DEFAULT 'DIRECT';
  ```

### Q3: WebSocket / WSS 連線中斷或延遲
- **機制保障**：前端 `chatStore` 採用雙重保險機制：
  1. 發送時優先經由 REST API (`/api/messages/direct`) 立即持久化並將訊息注入畫面。
  2. 後端同時透過 WebSocket 向對方推播。
  3. 即便 WebSocket 處於重連狀態，訊息發送亦保證 100% 成功。

### Q4: 程式碼更新後未自動部署
- 進入 Render 控制台點擊對應服務，選擇 **「Manual Deploy」** -> **「Clear build cache & deploy」**。

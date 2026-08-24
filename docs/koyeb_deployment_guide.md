# TalkWeb 雲端部署至 Koyeb (https://app.koyeb.com/) 完整指南

本指南將指引您如何將 TalkWeb (企業即時通訊系統) 免費或低成本部署至 **Koyeb Serverless 容器雲端平台**，並取得全自動配置的全球 CDN 與專屬 HTTPS 網址（例如：`https://talkweb-xxxx.koyeb.app`），供外部團隊成員直接連線使用。

---

## 🏗️ 雲端架構概覽

- **應用程式容器 (App Service)**：由 Koyeb 直接依據專案根目錄的 `Dockerfile` 自動建置並運行（Spring Boot 3 + Vue 3 前後端整合包）。
- **資料庫 (PostgreSQL)**：
  - 可直接使用 **Koyeb 內建 PostgreSQL Service**（推薦）。
  - 或使用外部免費雲端資料庫（如 **Neon.tech** 或 **Supabase**）。

---

## 🚀 部署三步驟詳細指引

### 第一步：建立雲端 PostgreSQL 資料庫並初始化

#### 選擇 A：使用 Koyeb 內建 Database
1. 登入 [https://app.koyeb.com/](https://app.koyeb.com/)。
2. 點擊頂部 **"Databases"** -> 點擊 **"Create Database"**。
3. 設定：
   - **Database Name**：`chatapp`
   - **Region**：選擇靠近台灣的區域（例如 `Singapore (sin)` 或 `Tokyo (tyo)`）。
   - **Engine**：PostgreSQL 14 或 16。
4. 建立完成後，Koyeb 會提供連線字串（Connection String）及下列資訊：
   - `Host`（主機名稱）
   - `Port`（預設 5432）
   - `Database`（`chatapp`）
   - `User`（資料庫帳號）
   - `Password`（資料庫密碼）
5. 使用 DBeaver、pgAdmin 或 Koyeb Web SQL 介面，連線至該資料庫並執行專案中的 [`docker/init.sql`](file:///c:/Users/G_ONE/AntigravityWorkspace/TalkWeb/docker/init.sql) 腳本，完成 Schema 與初始資料表建立。

---

### 第二步：將程式碼推送至 GitHub 倉庫

Koyeb 原生支援與 GitHub 連動自動部署：
```bash
# 1. 在 GitHub 上建立一個新的儲存庫 (例如 TalkWeb)
# 2. 在本地將程式碼推送到 GitHub：
git remote add origin https://github.com/<您的GitHub帳號>/TalkWeb.git
git branch -M main
git push -u origin main
```

---

### 第三步：在 Koyeb 建立並啟動 Web App 服務

1. 在 Koyeb 控制台點擊 **"Create Service"**。
2. 選擇 **"GitHub"** 作為部署來源，並授權選取剛才建立的 `TalkWeb` 儲存庫。
3. **Build & Deployment 設定**：
   - **Builder** 選擇 **`Dockerfile`**（Koyeb 會自動偵測根目錄下的 Multi-stage Dockerfile）。
   - **Instance Type**：選擇 `Free / Nano` 或 `Micro`（依需求選擇）。
   - **Regions**：選擇與資料庫相同的區域（例如 `Singapore` 或 `Tokyo`）。
4. **環境變數 (Environment Variables)** 設定：
   點擊 **"Add Environment Variable"**，依序新增以下變數：

   | 變數名稱 | 數值範例 | 說明 |
   |---|---|---|
   | `EXISTING_DB_HOST` | `ep-xyz.singapore.koyeb.app` | 第一步取得之 DB Host |
   | `EXISTING_DB_PORT` | `5432` | 資料庫連接埠 |
   | `DB_NAME` | `chatapp` | 資料庫名稱 |
   | `DB_USERNAME` | `koyeb-adm` | 資料庫使用者名稱 |
   | `DB_PASSWORD` | `您的資料庫密碼` | 資料庫連線密碼 |
   | `JWT_SECRET` | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` | JWT 加密金鑰 |
   | `SPRING_PROFILES_ACTIVE` | `prod` | 使用正式生產設定檔 |

5. **Exposed Ports & Protocol**：
   - **Port**：`8080`
   - **Protocol**：`HTTP`（Koyeb 邊緣節點會自動為您分配免費的全球 SSL/TLS 憑證與 HTTPS 網域）。
6. 點擊 **"Deploy"**！

---

## 🌐 外部存取與驗證

1. 部署開始後，Koyeb 會自動進行 Node.js 前端打包與 Maven 後端編譯，約需 2~3 分鐘。
2. 建置完成後，狀態將顯示為 🟢 **Healthy**。
3. Koyeb 提供的專屬公網網址形式如下：
   👉 **`https://talkweb-<您的App名稱>.koyeb.app`**
4. 外部人員即可直接以手機或電腦瀏覽器打開該網址，進行**建立帳號、搜尋同仁、加好友與即時對話**！

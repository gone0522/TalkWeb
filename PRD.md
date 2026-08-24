# 企業內部通訊工具 PRD（LINE風格網頁版）

- **版本**：v4.0
- **狀態**：待開發確認
- **目標平台**：Web / PWA（內網私有化部署）

---

## 一、專案概述

### 1.1 產品定位
開發一套具備即時通訊核心功能的企業內部 Web 應用系統，介面高度貼近 LINE 視覺風格與操作體驗。以 Docker 容器化方式封裝，支援單機快速私有化部署。

### 1.2 使用規模與環境
- **同時在線人數**：< 100 人
- **總用戶數**：< 200 人
- **部署環境**：企業內部伺服器（單機 Docker 部署）
- **網路環境**：企業純內網 HTTP（無需 HTTPS/TLS）

### 1.3 技術棧架構
- **前端 (Frontend)**：Vue 3 + Vite + Pinia + Vanilla CSS
- **後端 (Backend)**：Spring Boot 3 (Java 17+) + Spring Data JPA + Spring Security
- **即時通訊 (IM Engine)**：Spring WebSocket + STOMP 協議
- **資料庫 (Database)**：
  - **正式環境**：既有 PostgreSQL 14（新建專屬 Schema: `chatapp`）
  - **本地開發/測試**：使用本地 Docker 啟動 PostgreSQL 14 容器（包含自動初始化腳本）
- **圖片處理**：Thumbnailator (200x200px 裁切與壓縮)
- **封裝與部署**：Docker / Docker Compose（前後端整合單一 App 容器）

---

## 二、範圍確認事項與架構決策

| 項目 | 決策 | 說明 |
|---|---|---|
| **帳號系統** | 獨立帳號體系 | 不對接外部 AD/LDAP，由管理員在系統內建立 |
| **聊天訊息傳輸** | 純文字 + Emoji | MVP 第一期不支援檔案與圖片傳輸，僅支援純文字與 Unicode Emoji |
| **頭像儲存機制** | PostgreSQL `BYTEA` | 圖檔儲存於資料庫中，隨既有 DB 備份機制涵蓋 |
| **網路協議** | 純 HTTP / WS | 企業內部專屬網段運作，不處理 HTTPS/WSS 憑證 |
| **資料庫架構** | PostgreSQL 14 | 正式環境使用既有 DB（Schema: `chatapp`）；本地測試則透過本地 Docker 啟動 PostgreSQL 14 容器 |
| **UI 主題與風格** | LINE 經典淺色風格 | 高度還原 LINE 視覺，僅提供淺色模式 (Light Theme) |
| **通話與多媒體** | 不支援 | 排除語音、視訊通話及螢幕共享 |
| **用戶端形式** | Web / PWA | 不開發原生 App (iOS/Android)，支援桌面/行動瀏覽器存取 |
| **密碼安全機制** | 一次性密碼 + 強制改密 | 管理員建立帳號時由系統生成一次性密碼，首次登入強制修改 |
| **權限控管** | 單一登入入口 | 無獨立管理員後台，依用戶 `is_admin` 屬性於同一介面呈現管理選單 |

---

## 三、功能規劃

### 3.1 MVP（第一期核心功能）

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                MVP 功能架構                              │
├───────────────┬─────────────────────────┬───────────────────────────────┤
│   帳號與權限   │        即時通訊         │           通訊錄與管理        │
├───────────────┼─────────────────────────┼───────────────────────────────┤
│ • 帳號建立/停用│ • 一對一聊天            │ • 全成員通訊錄與關鍵字搜尋    │
│ • 一次性密碼  │ • 群組聊天 (建群/成員)  │ • 成員管理列表 (Admin 專屬)   │
│ • 首次登入改密│ • 純文字 + Emoji 訊息   │ • 個人資料設定 (暱稱/頭像)    │
│ • JWT 驗證機制│ • 即時推送 (WebSocket)  │ • 預設圖示指派 / 自訂上傳     │
│ • 權限狀態過濾│ • 訊息已讀/未讀狀態     │ • 未讀訊息數徽章提示          │
└───────────────┴─────────────────────────┴───────────────────────────────┘
```

| 模組 | 功能明細 | 驗收標準 |
|---|---|---|
| **帳號認證** | 管理員建帳號 → 系統回顯一次性密碼 → 用戶登入 → 首次登入強制改密 | 密碼雜湊儲存（BCrypt），一次性密碼不存明碼，改密完成前阻擋其他操作 |
| **一對一聊天** | 點擊好友即時對話、歷史訊息載入、已讀狀態標記、發送時間戳 | 雙方在線即時推送，離線者上線載入未讀，發送後即時反映送達與已讀狀態 |
| **群組聊天** | 建立群組、邀請成員、踢出成員、修改群名稱、發布群公告 | 群組成員變動即時通知，訊息全體廣播推送 |
| **訊息格式** | 純文字、標準 Unicode Emoji、URL 自動偵測與預覽卡片 | 防止 XSS 攻擊，正確渲染 Emoji；訊息中的 URL 自動轉為可點擊超連結（`target="_blank"`於新分頁開啟），並顯示 URL 預覽卡片（標題、描述、縮圖） |
| **頭像系統** | 自訂上傳（限制 500KB，後端 resize 200x200）、內建預設頭像 | 上傳前前端壓縮裁切，未上傳者依設定指派預設頭像 ID |
| **通訊錄** | 企業內部成員清單、即時關鍵字過濾搜尋 | 顯示在線狀態、頭像與暱稱 |
| **即時通訊引擎** | WebSocket + STOMP 雙向通訊、連線中斷自動重連機制 | 低延遲訊息收發，斷線時顯示重連狀態列 |
| **管理功能** | 管理員專屬「成員管理」入口（建立用戶、停用/啟用帳號） | 僅 `is_admin=true` 可見並可呼叫 Admin API |

### 3.2 後續規劃
- **第二期**：自建貼圖包庫、訊息撤回/編輯/引用回覆/轉發、群組已讀人數統計、全域訊息全文檢索
- **第三期**：聊天檔案/圖片傳輸、系統通知機器人/Webhook、進階審計日誌

### 3.3 明確排除項目
- 語音/視訊通話
- 原生 Mobile App (iOS / Android)
- 聊天訊息檔案與圖片傳輸
- HTTPS / TLS 加密配置（由反向代理或內網安全層負責）
- Active Directory / LDAP 目錄同步
- 深色模式 (Dark Mode)
- 獨立管理後台登入入口（採用統一登入 + 角色選單）

---

## 四、頭像儲存與處理機制

### 4.1 儲存方案評估
- **決策**：採用 PostgreSQL `BYTEA` 欄位儲存頭像二進位資料。
- **評估效益**：
  1. 企業內部規模小（< 200 人），總圖檔量 < 100MB，對資料庫負載極低。
  2. 隨 PostgreSQL 備份策略自動備份與還原，無需維護額外 Volume 或 S3/MinIO 物件儲存。
  3. 容器部署極度單純，無檔案路徑權限或持久化掛載相依問題。

### 4.2 處理規範與管線
```
[ 用戶選取圖片 ] 
       │
       ▼
[ 前端裁切與壓縮 (正方形, 最大 500KB) ]
       │
       ▼ (HTTP POST /api/users/me/avatar)
[ 後端 Thumbnailator 處理 (強制 Resize 為 200x200px, 壓縮品質 0.85) ]
       │
       ▼
[ 存入 users 表 BYTEA 欄位，記錄 avatar_mime_type ]
```

---

## 五、系統架構與資料流

### 5.1 架構總覽
```
┌─────────────────────────────────────────────────────────────┐
│                 客戶端瀏覽器 (Vue 3 + Vite)                 │
│         LINE 視覺風格 UI / 淺色主題 / Pinia 狀態管理         │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP REST API / WebSocket STOMP
┌──────────────────────────────▼──────────────────────────────┐
│           Spring Boot 3 整合應用 (單一 Docker 容器)          │
│ ├─ REST Controllers (認證/使用者/群組/訊息)                  │
│ ├─ WebSocket & STOMP Message Broker (/topic, /queue)        │
│ ├─ Security Filter (JWT Token, Role: USER / ADMIN)          │
│ ├─ Image Processing Engine (Thumbnailator)                  │
│ └─ Spring Data JPA Repositories                             │
└──────────────────────────────┬──────────────────────────────┘
                               │ JDBC (PostgreSQL Driver)
┌──────────────────────────────▼──────────────────────────────┐
│              既有 PostgreSQL 14 (Schema: chatapp)           │
│ └─ users, friendships, groups, group_members, messages ...  │
└─────────────────────────────────────────────────────────────┘
```

---

## 六、資料庫 Schema 設計 (Schema: `chatapp`)

### 6.1 資料表規格

```sql
CREATE SCHEMA IF NOT EXISTS chatapp;

-- 用戶表
CREATE TABLE chatapp.users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    avatar_data BYTEA,
    avatar_mime_type VARCHAR(50),
    avatar_default_icon INT DEFAULT 1,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, DISABLED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 好友/關聯表
CREATE TABLE chatapp.friendships (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES chatapp.users(id),
    friend_id BIGINT NOT NULL REFERENCES chatapp.users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_friend UNIQUE (user_id, friend_id)
);

-- 群組表
CREATE TABLE chatapp.groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    icon INT DEFAULT 1,
    announcement TEXT,
    created_by BIGINT NOT NULL REFERENCES chatapp.users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 群組成員表
CREATE TABLE chatapp.group_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES chatapp.groups(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES chatapp.users(id),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER', -- OWNER, ADMIN, MEMBER
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_group_user UNIQUE (group_id, user_id)
);

-- 訊息表
CREATE TABLE chatapp.messages (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES chatapp.users(id),
    receiver_id BIGINT REFERENCES chatapp.users(id),
    group_id BIGINT REFERENCES chatapp.groups(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'TEXT', -- TEXT, EMOJI, SYSTEM
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 訊息已讀狀態表
CREATE TABLE chatapp.message_read_status (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES chatapp.messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES chatapp.users(id),
    read_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_message_user UNIQUE (message_id, user_id)
);

-- 索引優化
CREATE INDEX idx_messages_sender ON chatapp.messages(sender_id);
CREATE INDEX idx_messages_receiver ON chatapp.messages(receiver_id);
CREATE INDEX idx_messages_group ON chatapp.messages(group_id);
CREATE INDEX idx_messages_created ON chatapp.messages(created_at);
CREATE INDEX idx_read_status_msg ON chatapp.message_read_status(message_id, user_id);
```

---

## 七、API 介面與 WebSocket 協議

### 7.1 RESTful API 清單

| 類別 | Method | Endpoint | 權限 | 說明 |
|---|---|---|---|---|
| **認證** | `POST` | `/api/auth/login` | 公開 | 登入並取得 JWT Token |
| | `PUT` | `/api/auth/password` | Authenticated | 修改密碼（首次改密或一般改密） |
| **使用者** | `GET` | `/api/users/me` | Authenticated | 取得當前使用者個人資訊 |
| | `PUT` | `/api/users/me` | Authenticated | 更新使用者個人暱稱/預設頭像 |
| | `POST` | `/api/users/me/avatar` | Authenticated | 上傳自訂頭像（Multipart/form-data） |
| | `GET` | `/api/users/{id}/avatar` | Authenticated | 讀取頭像圖檔（二進位串流） |
| **通訊錄** | `GET` | `/api/contacts` | Authenticated | 取得成員通訊錄與好感/在線狀態 |
| **群組** | `POST` | `/api/groups` | Authenticated | 建立新群組 |
| | `GET` | `/api/groups/{id}` | Authenticated | 取得群組詳情與成員清單 |
| | `PUT` | `/api/groups/{id}` | Group Admin | 更新群組名稱或公告 |
| | `POST` | `/api/groups/{id}/members` | Group Member | 邀請成員入群 |
| | `DELETE`| `/api/groups/{id}/members/{userId}` | Group Admin | 移除群組成員 / 自行退出 |
| **訊息** | `GET` | `/api/messages/direct/{userId}` | Authenticated | 取得一對一歷史訊息（分頁） |
| | `GET` | `/api/messages/group/{groupId}` | Authenticated | 取得群組歷史訊息（分頁） |
| | `POST` | `/api/messages/read` | Authenticated | 批次標記訊息為已讀 |
| **管理** | `POST` | `/api/admin/users` | Admin Only | 建立新用戶（回傳一次性密碼） |
| | `GET` | `/api/admin/users` | Admin Only | 取得全體用戶管理清單 |
| | `PUT` | `/api/admin/users/{id}/status` | Admin Only | 啟用/停用帳號 |
| | `POST` | `/api/admin/users/{id}/reset-password` | Admin Only | 重置密碼（生成新一次性密碼） |

### 7.2 WebSocket / STOMP 設計
- **連線端點**：`/ws/chat`（傳入 JWT Token 作為連線 Handshake Header）
- **發送路徑 (Send)**：
  - `/app/chat.sendDirect`：發送一對一訊息
  - `/app/chat.sendGroup`：發送群組訊息
  - `/app/chat.read`：發送已讀回執
- **訂閱路徑 (Subscribe)**：
  - `/user/queue/messages`：接收個人專屬訊息與已讀通知
  - `/topic/group.{groupId}`：訂閱所屬群組即時訊息
  - `/topic/announcements`：系統廣播通知

---

## 八、PwC Taiwan 視覺設計規範

### 8.1 品牌色票定義（資誠聯合會計師事務所）
```css
:root {
  --line-primary: #D04A02;        /* 主色（PwC 經典橘 PwC Orange） */
  --line-primary-hover: #B33D00;  /* PwC 深橘色 */
  --line-primary-light: #FDF1EB;  /* PwC 柔橘淺底 */
  --line-secondary: #EB8C00;      /* PwC 琥珀黃 (PwC Amber) */
  --line-accent-red: #E0301E;     /* PwC 經典紅 (PwC Red) */
  --line-bubble-me: #FEE7DD;      /* 我方訊息氣泡（PwC 暖橘調） */
  --line-bubble-other: #FFFFFF;   /* 對方訊息氣泡（純白） */
  --line-bubble-border: #E8E8E8;  /* 對方氣泡邊框 */
  --line-bg-chat: #F5F5F7;        /* 聊天室商務灰底 */
  --line-badge-red: #E0301E;      /* 未讀標記紅 (PwC Red) */
  --line-text-primary: #2D2D2D;   /* 主要文字（PwC 炭黑 Charcoal） */
  --line-text-secondary: #707070; /* 次要輔助文字 / 時間戳 */
  --line-border-light: #E8E8E8;   /* 分隔線與邊框 */
  --line-sidebar-bg: #1F1F1F;     /* 側邊導覽深炭黑背景 */
}
```

### 8.2 UI 版面佈局
```
┌──────────┬─────────────────────────────┬─────────────────────────────────┐
│ 功能導覽 │ 列表區域                    │ 聊天內容區                      │
│ (48px)   │ (280px)                     │ (彈性寬度)                      │
├──────────┼─────────────────────────────┼─────────────────────────────────┤
│ 👤 好友  │ 🔍 搜尋框                   │ 🏷️ 聊天室抬頭 (名稱 + 在線狀態) │
│ 💬 聊天  │ ─────────────────────────── ├─────────────────────────────────┤
│ ⚙️ 設定  │ 列表項目 (頭像+暱稱+最新訊息│ 💬 對話氣泡流                   │
│ 🛡️ 管理* │ +時間+未讀紅點Badge)        │    [對方氣泡 (白)] (時間)       │
│          │                             │              (已讀)(時間) [我方]│
│          │                             ├─────────────────────────────────┤
│          │                             │ 📝 輸入區 (Emoji選擇器+發送鍵)  │
└──────────┴─────────────────────────────┴─────────────────────────────────┘
* 🛡️ 管理選單僅 is_admin=true 之使用者可見
```

---

## 九、Docker 封裝與私有化部署

### 9.1 Multi-Stage Dockerfile
- **Stage 1 (Frontend)**: Node.js 20 建置 Vue 3 靜態資源至 `/dist`。
- **Stage 2 (Backend)**: Maven 3.9 + Java 17，將 `/dist` 靜態資源打包至 Spring Boot `src/main/resources/static`，編譯為單一 Runnable JAR。
- **Stage 3 (Runtime)**: Eclipse Temurin 17 JRE Alpine 精簡映像檔執行。

### 9.2 環境變數設定檔 (`.env`)
```ini
EXISTING_DB_HOST=192.168.1.50
EXISTING_DB_PORT=5432
DB_NAME=chatapp
DB_USERNAME=chatapp_user
DB_PASSWORD=your_secure_password
JWT_SECRET=your_jwt_secret_key_minimum_256_bits
AVATAR_MAX_SIZE_KB=500
```

### 9.3 本地開發與測試環境 (Local Docker PostgreSQL)
為了方便開發與整合測試，提供專屬的 `docker-compose.dev.yml`，一鍵在本地啟動 PostgreSQL 14 容器，並透過掛載 `init.sql` 自動完成 `chatapp` 資料庫與 Schema 初始化：

```yaml
# docker-compose.dev.yml
version: '3.8'
services:
  postgres-dev:
    image: postgres:14-alpine
    container_name: talkweb-postgres-dev
    restart: unless-stopped
    environment:
      POSTGRES_DB: chatapp
      POSTGRES_USER: chatapp_user
      POSTGRES_PASSWORD: dev_password_123
    ports:
      - "5432:5432"
    volumes:
      - pgdata_dev:/var/lib/postgresql/data
      - ./docker/init.sql:/docker-entrypoint-initdb.d/init.sql:ro

volumes:
  pgdata_dev:
```

**本地啟動指令**：
```bash
# 啟動本地測試資料庫
docker-compose -f docker-compose.dev.yml up -d
```

---

## 十、專案時程與里程碑 (約 8.5 週)

```
W1          W2 - W4           W5 - W7           W8         W8.5
┌───────────┬─────────────────┬─────────────────┬──────────┬──────────┐
│ UI/UX 設計│ 後端 API/WS 開發│ 前端介面與串接  │ 整合測試 │ Docker封裝│
│ (LINE風格)│ (Spring Boot 3) │ (Vue 3 + STOMP) │ & Bug修復│ & 上線交付│
└───────────┴─────────────────┴─────────────────┴──────────┴──────────┘
```

---

## 十一、待確認事項（決策建議）

以下為需要進一步確認的業務邏輯細節，並附上標準建議方案供討論確認：

1. **一次性密碼有效期限**：
   - *建議*：設定為 **24 小時** 或 **首次登入前永久有效（登入後立即失效並強制作廢）**。企業內網通常建議「首次登入前有效，直到管理員再次重置」。
2. **帳號命名規則 (Username)**：
   - *建議*：`^[a-zA-Z0-9._-]{3,30}$`（英文小寫、數字、點、底線、減號，長度 3~30 碼，如 `john.doe`、`it_admin`）。
3. **群組建立權限**：
   - *建議*：**全體成員皆可建立群組**（符合 LINE 協作習慣），建立者自動成為群主 (Owner)，可指派 Admin 或移交群主。
4. **頭像審核機制**：
   - *建議*：**無須審核**（內部系統信任機制，若有不妥由管理者直接於後台重置/替換）。
5. **多裝置/分頁同時登入**：
   - *建議*：**允許同帳號多分頁/多裝置同時在線**，WebSocket 廣播至所有該使用者的連線 Session，同步已讀狀態與最新訊息。

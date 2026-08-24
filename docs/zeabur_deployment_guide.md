# TalkWeb 雲端部署至 Zeabur (https://zeabur.com/) 完整指南

Zeabur 是由台灣團隊打造的現代化 Serverless 容器雲端平台，具備**原生繁體中文介面**、**台灣/亞太本地機房低延遲**，並支援一鍵從 GitHub 倉庫讀取 `Dockerfile` 自動建置與配置免費 HTTPS 網域名稱（`https://<您的名稱>.zeabur.app`）。

---

## 🏗️ 雲端架構概覽

在 Zeabur 同一個專案（Project）中建立兩個服務：
1. **PostgreSQL 資料庫服務**（由 Zeabur 市集一鍵建立，享有內網高速互通）。
2. **TalkWeb Web App 服務**（連動 GitHub `gone0522/TalkWeb` 倉庫，自動根據 `Dockerfile` 建置）。

---

## 🚀 部署 3 步驟詳細指引

### 第一步：登入並建立專案與 PostgreSQL 資料庫

1. 開啟 **[https://zeabur.com/](https://zeabur.com/)**，點擊右上角 **「使用 GitHub 登入」**。
2. 點擊 **「建立專案 (Create Project)」**，區域選擇靠近台灣的節點（例如 **`Taiwan (Hinet)`** 或 **`Tokyo`**）。
3. 在專案內點擊 **「建立服務 (Create Service)」**：
   - 選擇 **「服務市場 (Marketplace)」**。
   - 搜尋並選擇 **`PostgreSQL`**。
   - Zeabur 會在幾秒內為您啟動專屬的 PostgreSQL 資料庫。
4. 點擊進入剛建立的 `PostgreSQL` 服務：
   - 切換至 **「連線 (Networking)」** 或 **「環境變數 (Variables)」** 頁籤。
   - 點擊 **「網頁終端 / Web SQL」** 或使用資料庫工具連線，將專案中的 [`docker/init.sql`](file:///c:/Users/G_ONE/AntigravityWorkspace/TalkWeb/docker/init.sql) 內容貼上執行，完成 Schema 與初始管理員資料建立。

---

### 第二步：建立 TalkWeb 應用程式服務 (Web App)

1. 回到專案畫面，再次點擊 **「建立服務 (Create Service)」**。
2. 選擇 **「Git」** -> 選取您的 GitHub 儲存庫：**`gone0522/TalkWeb`**（分支選擇 `main`）。
3. Zeabur 會自動辨識專案根目錄的 **`Dockerfile`** 並開始自動化多階段建置。
4. 點擊進入 `TalkWeb` 服務，切換至 **「環境變數 (Variables)」** 頁籤，新增以下變數：

   | 變數名稱 (Key) | 數值 (Value) | 說明 |
   |---|---|---|
   | **`EXISTING_DB_HOST`** | `${POSTGRES_HOST}` 或貼上 PostgreSQL 內網主機名稱 | Zeabur 支援變數引用 |
   | **`EXISTING_DB_PORT`** | `${POSTGRES_PORT}` 或 `5432` | 資料庫連接埠 |
   | **`DB_NAME`** | `${POSTGRES_DATABASE}` 或 `chatapp` | 資料庫名稱 |
   | **`DB_USERNAME`** | `${POSTGRES_USERNAME}` | 資料庫帳號 |
   | **`DB_PASSWORD`** | `${POSTGRES_PASSWORD}` | 資料庫密碼 |
   | **`JWT_SECRET`** | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` | JWT 金鑰 |
   | **`SPRING_PROFILES_ACTIVE`** | `prod` | 生產環境設定檔 |

   > 💡 **提示**：Zeabur 支援 `${POSTGRES_HOST}` 等自動引用語法，或者直接從 PostgreSQL 服務的「環境變數」中複製實際帳密與主機字串填入即可。

---

### 第三步：綁定免費公開域名 (Domain)

1. 在 `TalkWeb` 服務頁面，切換至 **「網路 (Networking)」** 頁籤。
2. 在 **「公開網域名稱 (Public Endpoints)」** 區塊：
   - 點擊 **「產生網域名稱 (Generate Domain)」**。
   - 自訂您喜歡的名稱（例如：`pwc-talkweb.zeabur.app` 或 `talkweb-company.zeabur.app`）。
   - Port 預設設定為 **`8080`**。
3. 點擊儲存！Zeabur 會在數秒內為您自動簽發全球 SSL/TLS 憑證與 HTTPS 網域。

---

## 🌐 外部人員連線使用

部署完成後，外部同仁即可直接透過手機或電腦瀏覽器打開您的專屬網址：
👉 **`https://<您設定的名稱>.zeabur.app`**

可直接享受：
- 🏢 **PwC Taiwan 專業品牌企業視覺風格**
- 👤 **登入頁自由註冊新帳號**
- 🔍 **搜尋同仁帳號並加好友**
- 💬 **一對一與群組即時通訊**
- 🔗 **URL 網址預覽卡片與 Emoji 貼圖**

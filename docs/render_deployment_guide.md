# TalkWeb 雲端部署至 Render (https://render.com/) 完整指南

Render 是國際知名的全託管雲端平台，提供**永久免費方案（Free Tier）**、**免綁信用卡**，並支援透過 `render.yaml` Blueprint 規格進行**一鍵全自動化部署**（自動建立 PostgreSQL 資料庫與 TalkWeb Web 服務並自動串接連線變數）。

---

## 🚀 2 步驟一鍵全自動部署

### 第一步：登入 Render 並使用 Blueprint 一鍵建立

1. 開啟 **[https://dashboard.render.com/](https://dashboard.render.com/)**，點擊右上角 **「Sign in with GitHub」**。
2. 點擊頂部右上角藍色按鈕 **「New +」** -> 選擇 **「Blueprint」**。
3. 在倉庫清單中選取 **`gone0522/TalkWeb`**（若未看到，點擊「Configure access on GitHub」授權選取）。
4. Render 會自動讀取專案中的 `render.yaml`，並自動為您列出兩項待建立的資源：
   - 🟢 **`talkweb-db`**：免費 PostgreSQL 資料庫（區域：Singapore）
   - 🟢 **`talkweb-app`**：免費 Web Service 容器（區域：Singapore，自動讀取 Dockerfile 並自動帶入資料庫連線參數）
5. 點擊 **「Apply」**！

---

### 第二步：初始化資料庫結構 (僅需執行一次)

1. 部署開始後，Render 會在約 1 分鐘內先完成 `talkweb-db` 資料庫啟動。
2. 點擊左側導覽列的 **`talkweb-db`** 進入資料庫管理頁面。
3. 點擊右側的 **「Connect」** -> 選擇 **「External Connection」** 或使用網頁終端：
   - 將專案中的 [`docker/init.sql`](file:///c:/Users/G_ONE/AntigravityWorkspace/TalkWeb/docker/init.sql) 腳本內容貼上執行一次，完成 Schema 與初始管理員資料建立。

---

## 🌐 外部人員存取與使用

約 2~3 分鐘後，`talkweb-app` 建置完成，狀態顯示為 🟢 **Live**！
Render 會自動配發專屬的公開 HTTPS 網址：
👉 **`https://talkweb-app-xxxx.onrender.com`**

外部團隊成員即可直接透過手機或電腦瀏覽器打開該網址，立即使用：
- 🏢 **PwC Taiwan 品牌專業配色**
- 👤 **自由註冊新帳號**
- 🔍 **搜尋同仁帳號並加好友**
- 💬 **即時文字與群組聊天**
- 🔗 **網址預覽卡片與 Emoji 貼圖**

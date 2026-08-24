<template>
  <div class="login-wrapper">
    <div class="login-card">
      <!-- PwC Brand Header -->
      <div class="logo-header">
        <div class="brand-logo">
          <svg viewBox="0 0 100 100" width="60" height="60">
            <defs>
              <linearGradient id="pwcGrad1" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#D04A02" />
                <stop offset="100%" stop-color="#B33D00" />
              </linearGradient>
              <linearGradient id="pwcGrad2" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#EB8C00" />
                <stop offset="100%" stop-color="#FFB600" />
              </linearGradient>
            </defs>
            <rect width="100" height="100" rx="22" fill="#2D2D2D" />
            <!-- PwC Interlocking Warm Color Geometry + Chat Bubble -->
            <rect x="20" y="24" width="26" height="26" rx="4" fill="url(#pwcGrad1)" />
            <rect x="42" y="24" width="22" height="26" rx="4" fill="#E0301E" opacity="0.9" />
            <rect x="20" y="46" width="44" height="28" rx="6" fill="url(#pwcGrad2)" />
            <path d="M60 48 C60 48 76 48 76 60 C76 66 70 70 64 71 L66 78 L58 72 Z" fill="#FFFFFF" />
          </svg>
        </div>
        <h1 class="brand-title">PwC TalkWeb</h1>
        <p class="brand-subtitle">資誠聯合會計師事務所 內部即時通訊</p>
      </div>

      <!-- Mode Switcher Tabs -->
      <div class="auth-tabs">
        <button
          :class="['auth-tab-btn', { active: isLoginMode }]"
          @click="switchMode(true)"
        >
          登入系統
        </button>
        <button
          :class="['auth-tab-btn', { active: !isLoginMode }]"
          @click="switchMode(false)"
        >
          建立新帳號
        </button>
      </div>

      <!-- 1. Login Form -->
      <form v-if="isLoginMode" @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label>帳號 (Username)</label>
          <input
            v-model="loginUsername"
            type="text"
            class="input-control"
            placeholder="請輸入帳號"
            autocomplete="username"
            required
          />
        </div>

        <div class="form-group">
          <label>密碼 (Password)</label>
          <input
            v-model="loginPassword"
            type="password"
            class="input-control"
            placeholder="請輸入密碼或一次性密碼"
            autocomplete="current-password"
            required
          />
        </div>

        <div v-if="authStore.error" class="login-error">
          ⚠️ {{ authStore.error }}
        </div>

        <button
          type="submit"
          class="btn btn-primary login-btn"
          :disabled="authStore.loading || !loginUsername || !loginPassword"
        >
          {{ authStore.loading ? '登入驗證中...' : '登入' }}
        </button>

        <div class="switch-mode-link" @click="switchMode(false)">
          尚未建立帳號？<span>立即註冊新帳號 →</span>
        </div>

        <div class="login-footer-tips">
          由管理員建立之帳號，首次登入請使用一次性密碼。
        </div>
      </form>

      <!-- 2. Register Form (建立帳號) -->
      <form v-else @submit.prevent="handleRegister" class="login-form">
        <div class="form-group">
          <label>帳號 (Username) <span class="required">*</span></label>
          <input
            v-model="regUsername"
            type="text"
            class="input-control"
            placeholder="3~30 碼英數或 . _ - (例如: alex.chen)"
            pattern="^[a-zA-Z0-9._-]{3,30}$"
            required
          />
        </div>

        <div class="form-group">
          <label>姓名 / 顯示暱稱 (Nickname) <span class="required">*</span></label>
          <input
            v-model="regNickname"
            type="text"
            class="input-control"
            placeholder="請輸入您的姓名或暱稱 (例如: 陳大明)"
            required
          />
        </div>

        <div class="form-group">
          <label>密碼 (Password) <span class="required">*</span></label>
          <input
            v-model="regPassword"
            type="password"
            class="input-control"
            placeholder="請輸入密碼 (至少 6 碼)"
            minlength="6"
            required
          />
        </div>

        <div class="form-group">
          <label>確認密碼 <span class="required">*</span></label>
          <input
            v-model="regConfirmPassword"
            type="password"
            class="input-control"
            placeholder="請再次輸入密碼"
            minlength="6"
            required
          />
        </div>

        <div v-if="regError" class="login-error">
          ⚠️ {{ regError }}
        </div>

        <button
          type="submit"
          class="btn btn-primary login-btn"
          :disabled="authStore.loading || !regUsername || !regNickname || !regPassword || regPassword !== regConfirmPassword"
        >
          {{ authStore.loading ? '建立帳號中...' : '確認建立帳號並登入' }}
        </button>

        <div class="switch-mode-link" @click="switchMode(true)">
          已有帳號？<span>返回登入頁面 →</span>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useAuthStore } from '../stores/authStore';

const authStore = useAuthStore();

const isLoginMode = ref(true);

// Login Fields
const loginUsername = ref('');
const loginPassword = ref('');

// Register Fields
const regUsername = ref('');
const regNickname = ref('');
const regPassword = ref('');
const regConfirmPassword = ref('');
const regError = ref('');

const switchMode = (loginMode) => {
  isLoginMode.value = loginMode;
  authStore.error = null;
  regError.value = '';
};

const handleLogin = async () => {
  if (!loginUsername.value || !loginPassword.value) return;
  try {
    await authStore.login(loginUsername.value.trim(), loginPassword.value);
  } catch (err) {
    // Handled in store
  }
};

const handleRegister = async () => {
  if (regPassword.value !== regConfirmPassword.value) {
    regError.value = '兩次輸入的密碼不一致';
    return;
  }
  if (regPassword.value.length < 6) {
    regError.value = '密碼長度需至少 6 碼';
    return;
  }

  regError.value = '';
  try {
    await authStore.register(regUsername.value.trim(), regNickname.value.trim(), regPassword.value);
  } catch (err) {
    regError.value = err.message || '建立帳號失敗';
  }
};
</script>

<style scoped>
.login-wrapper {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FBF4F0 0%, #EAECEF 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background-color: #FFFFFF;
  border-radius: 16px;
  box-shadow: 0 12px 36px rgba(45, 45, 45, 0.12);
  padding: 32px 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.logo-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.brand-logo {
  margin-bottom: 10px;
  filter: drop-shadow(0 4px 10px rgba(208, 74, 2, 0.25));
}

.brand-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--line-text-primary);
  letter-spacing: -0.5px;
}

.brand-subtitle {
  font-size: 13px;
  color: var(--line-text-secondary);
  margin-top: 3px;
}

/* Tabs */
.auth-tabs {
  display: flex;
  width: 100%;
  border-bottom: 1px solid var(--line-border);
  margin-bottom: 18px;
}

.auth-tab-btn {
  flex: 1;
  padding: 10px;
  background: none;
  border: none;
  font-size: 14px;
  font-weight: 600;
  color: var(--line-text-secondary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s ease;
}

.auth-tab-btn.active {
  color: var(--line-primary);
  border-bottom-color: var(--line-primary);
}

.login-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-group label {
  font-size: 12px;
  font-weight: 600;
  color: var(--line-text-primary);
}

.required {
  color: var(--line-badge-red);
}

.login-error {
  background-color: #FEEBEB;
  color: var(--line-badge-red);
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.4;
}

.login-btn {
  width: 100%;
  padding: 11px;
  font-size: 14px;
  border-radius: 8px;
  margin-top: 4px;
}

.switch-mode-link {
  text-align: center;
  font-size: 12px;
  color: var(--line-text-secondary);
  cursor: pointer;
  margin-top: 6px;
  transition: color 0.15s ease;
}

.switch-mode-link span {
  color: var(--line-primary);
  font-weight: 600;
}

.switch-mode-link:hover span {
  text-decoration: underline;
}

.login-footer-tips {
  font-size: 11px;
  color: var(--line-text-muted);
  text-align: center;
  line-height: 1.4;
  margin-top: 6px;
}
</style>

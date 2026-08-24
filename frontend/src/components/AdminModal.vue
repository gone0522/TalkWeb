<template>
  <div class="modal-backdrop" @click="$emit('close')">
    <div class="modal-content admin-dialog" @click.stop>
      <div class="modal-header">
        <h3>系統管理員控制台</h3>
        <button class="modal-close" @click="$emit('close')">✕</button>
      </div>

      <!-- Admin Tabs -->
      <div class="admin-tabs">
        <button
          :class="['admin-tab-btn', { active: currentTab === 'create' }]"
          @click="currentTab = 'create'"
        >
          ➕ 建立新使用者
        </button>
        <button
          :class="['admin-tab-btn', { active: currentTab === 'users' }]"
          @click="loadUsersList"
        >
          👥 成員管理清單 ({{ userList.length }})
        </button>
      </div>

      <div class="modal-body">
        <!-- Tab 1: Create User -->
        <div v-if="currentTab === 'create'">
          <form @submit.prevent="handleCreateUser" class="admin-form">
            <div class="form-group">
              <label>登入帳號 (Username) <span class="required">*</span></label>
              <input
                v-model="newUser.username"
                type="text"
                class="input-control"
                placeholder="例如: john.doe (3~30 碼英數及點/底線)"
                pattern="^[a-zA-Z0-9._-]{3,30}$"
                required
              />
            </div>

            <div class="form-group">
              <label>顯示暱稱 (Nickname) <span class="required">*</span></label>
              <input
                v-model="newUser.nickname"
                type="text"
                class="input-control"
                placeholder="例如: 王小明"
                required
              />
            </div>

            <div class="form-group checkbox-row">
              <label class="checkbox-label">
                <input v-model="newUser.isAdmin" type="checkbox" />
                <span>賦予系統管理員權限 (is_admin)</span>
              </label>
            </div>

            <div v-if="createError" class="error-text">
              {{ createError }}
            </div>

            <div class="admin-footer-btn">
              <button
                type="submit"
                class="btn btn-primary"
                :disabled="creating || !newUser.username || !newUser.nickname"
              >
                {{ creating ? '建立中...' : '建立使用者並生成一次性密碼' }}
              </button>
            </div>
          </form>
        </div>

        <!-- Tab 2: User List & Management -->
        <div v-else-if="currentTab === 'users'" class="users-management-tab">
          <div class="user-table-wrap">
            <table class="user-table">
              <thead>
                <tr>
                  <th>帳號</th>
                  <th>暱稱</th>
                  <th>權限</th>
                  <th>狀態</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="u in userList" :key="u.id">
                  <td><strong>{{ u.username }}</strong></td>
                  <td>{{ u.nickname }}</td>
                  <td>
                    <span v-if="u.admin" class="role-badge admin">管理員</span>
                    <span v-else class="role-badge">一般</span>
                  </td>
                  <td>
                    <span :class="['status-badge', u.status?.toLowerCase()]">
                      {{ u.status === 'ACTIVE' ? '正常' : '已停用' }}
                    </span>
                  </td>
                  <td class="actions-cell">
                    <button
                      v-if="u.id !== authStore.user?.id"
                      class="action-btn toggle-status-btn"
                      @click="toggleUserStatus(u)"
                    >
                      {{ u.status === 'ACTIVE' ? '停用' : '啟用' }}
                    </button>
                    <button
                      class="action-btn reset-pwd-btn"
                      title="重置密碼為一次性密碼"
                      @click="resetUserPassword(u)"
                    >
                      重置密碼
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- OTP Success Popup Modal Overlay -->
      <div v-if="generatedOtp" class="otp-dialog-overlay">
        <div class="otp-dialog">
          <div class="otp-title">🎉 用戶建立 / 重置密碼成功！</div>
          <div class="otp-desc">
            請將以下登入帳號與<strong>一次性密碼</strong>提供給該使用者。使用者首次登入時系統將要求立即設定新密碼：
          </div>

          <div class="otp-credential-box">
            <div class="cred-row">
              <span class="cred-label">帳號:</span>
              <span class="cred-value">{{ generatedUsername }}</span>
            </div>
            <div class="cred-row">
              <span class="cred-label">一次性密碼:</span>
              <span class="cred-value otp-code">{{ generatedOtp }}</span>
            </div>
          </div>

          <div class="otp-actions">
            <button class="btn btn-secondary" @click="copyCredentials">
              {{ copied ? '✅ 已複製到剪貼簿' : '📋 複製帳號與密碼' }}
            </button>
            <button class="btn btn-primary" @click="generatedOtp = ''">
              完成
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { adminApi } from '../services/api';
import { useAuthStore } from '../stores/authStore';
import { useContactsStore } from '../stores/contactsStore';

defineEmits(['close']);

const authStore = useAuthStore();
const contactsStore = useContactsStore();

const currentTab = ref('create');
const userList = ref([]);
const creating = ref(false);
const createError = ref('');

const newUser = ref({
  username: '',
  nickname: '',
  isAdmin: false
});

const generatedOtp = ref('');
const generatedUsername = ref('');
const copied = ref(false);

const loadUsersList = async () => {
  currentTab.value = 'users';
  try {
    const res = await adminApi.getAllUsers();
    userList.value = res.data || [];
  } catch (err) {
    alert(err.message || '載入成員清單失敗');
  }
};

onMounted(() => {
  if (currentTab.value === 'users') {
    loadUsersList();
  }
});

const handleCreateUser = async () => {
  creating.value = true;
  createError.value = '';
  try {
    const res = await adminApi.createUser({
      username: newUser.value.username.trim(),
      nickname: newUser.value.nickname.trim(),
      admin: newUser.value.isAdmin
    });

    generatedUsername.value = res.data.username;
    generatedOtp.value = res.data.oneTimePassword;
    copied.value = false;

    // Reset form
    newUser.value = { username: '', nickname: '', isAdmin: false };
    contactsStore.fetchContacts();
  } catch (err) {
    createError.value = err.message || '建立使用者失敗';
  } finally {
    creating.value = false;
  }
};

const toggleUserStatus = async (user) => {
  const newStatus = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const actionText = newStatus === 'ACTIVE' ? '啟用' : '停用';
  if (!confirm(`確定要${actionText}使用者「${user.nickname}」(@${user.username}) 嗎？`)) return;

  try {
    await adminApi.updateUserStatus(user.id, newStatus);
    user.status = newStatus;
    contactsStore.fetchContacts();
  } catch (err) {
    alert(err.message || '操作失敗');
  }
};

const resetUserPassword = async (user) => {
  if (!confirm(`確定要重置「${user.nickname}」的登入密碼為新的一次性密碼嗎？`)) return;

  try {
    const res = await adminApi.resetPassword(user.id);
    generatedUsername.value = user.username;
    generatedOtp.value = res.data.newOneTimePassword;
    copied.value = false;
  } catch (err) {
    alert(err.message || '重置密碼失敗');
  }
};

const copyCredentials = () => {
  const text = `TalkWeb 企業通訊軟體帳號資訊\n帳號：${generatedUsername.value}\n一次性密碼：${generatedOtp.value}\n（首次登入時請依指示修改密碼）`;
  navigator.clipboard.writeText(text);
  copied.value = true;
  setTimeout(() => copied.value = false, 3000);
};
</script>

<style scoped>
.admin-dialog {
  max-width: 640px;
  min-height: 480px;
  position: relative;
}

.admin-tabs {
  display: flex;
  border-bottom: 1px solid var(--line-border);
  background-color: #F8F9FA;
}

.admin-tab-btn {
  flex: 1;
  padding: 12px;
  background: none;
  border: none;
  font-size: 14px;
  font-weight: 600;
  color: var(--line-text-secondary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.15s ease;
}

.admin-tab-btn.active {
  color: var(--line-primary);
  border-bottom-color: var(--line-primary);
  background-color: #FFFFFF;
}

.admin-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 10px 0;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
}

.required {
  color: #FF3B30;
}

.checkbox-row {
  flex-direction: row;
  align-items: center;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 13px;
  user-select: none;
}

.admin-footer-btn {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.error-text {
  color: #FF3B30;
  font-size: 13px;
}

/* User Table */
.user-table-wrap {
  overflow-x: auto;
  max-height: 340px;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.user-table th, .user-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #ECECEC;
}

.user-table th {
  background-color: #FAFAFA;
  font-weight: 600;
  color: var(--line-text-secondary);
}

.role-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background-color: #F0F0F0;
  color: #666;
}

.role-badge.admin {
  background-color: #E8F8EE;
  color: var(--line-primary-hover);
  font-weight: 600;
}

.status-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
}

.status-badge.active {
  background-color: #E8F8EE;
  color: #05A648;
}

.status-badge.disabled {
  background-color: #FFEEEE;
  color: #FF3B30;
}

.actions-cell {
  display: flex;
  gap: 6px;
}

.action-btn {
  padding: 3px 8px;
  font-size: 11px;
  border-radius: 4px;
  border: 1px solid var(--line-border);
  background-color: #FFFFFF;
  cursor: pointer;
  transition: all 0.15s ease;
}

.toggle-status-btn:hover {
  background-color: #FFF3CD;
  border-color: #FFEBAA;
}

.reset-pwd-btn:hover {
  background-color: #E8F8EE;
  border-color: var(--line-primary);
  color: var(--line-primary-hover);
}

/* OTP Popup */
.otp-dialog-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.otp-dialog {
  background: #FFFFFF;
  padding: 24px;
  border-radius: 12px;
  width: 85%;
  max-width: 440px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.otp-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--line-primary-hover);
}

.otp-desc {
  font-size: 13px;
  color: var(--line-text-primary);
  line-height: 1.4;
}

.otp-credential-box {
  background-color: #F7F9FA;
  border: 1px dashed var(--line-primary);
  border-radius: 8px;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cred-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.cred-label {
  color: var(--line-text-secondary);
}

.cred-value {
  font-weight: 600;
  color: var(--line-text-primary);
}

.otp-code {
  font-size: 18px;
  letter-spacing: 2px;
  color: var(--line-primary-hover);
  font-family: monospace;
}

.otp-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

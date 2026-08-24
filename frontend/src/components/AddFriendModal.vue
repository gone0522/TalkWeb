<template>
  <div class="modal-backdrop" @click="$emit('close')">
    <div class="modal-content add-friend-dialog" @click.stop>
      <div class="modal-header">
        <h3>新增好友 (檢查帳號)</h3>
        <button class="modal-close" @click="$emit('close')">✕</button>
      </div>

      <div class="modal-body">
        <div class="search-account-form">
          <label>請輸入欲新增的使用者帳號 (Username)：</label>
          <div class="input-with-btn">
            <input
              v-model="searchUsername"
              type="text"
              class="input-control"
              placeholder="例如: alex.chen / user1"
              @keydown.enter.prevent="handleCheckUser"
            />
            <button
              class="btn btn-primary check-btn"
              :disabled="checking || !searchUsername.trim()"
              @click="handleCheckUser"
            >
              {{ checking ? '檢查中...' : '檢查帳號' }}
            </button>
          </div>
        </div>

        <!-- Error / Not Found Message -->
        <div v-if="errorMessage" class="error-box">
          ⚠️ {{ errorMessage }}
        </div>

        <!-- Success User Card -->
        <div v-if="foundUser" class="found-user-card">
          <div class="card-left">
            <DefaultAvatar
              :user-id="foundUser.id"
              :nickname="foundUser.nickname"
              :has-custom-avatar="foundUser.hasCustomAvatar"
              :icon-index="foundUser.avatarDefaultIcon"
              :size="52"
            />
            <div class="card-info">
              <div class="card-nickname">{{ foundUser.nickname }}</div>
              <div class="card-username">@{{ foundUser.username }}</div>
              <div class="card-presence">
                <span :class="['presence-dot-inline', { online: foundUser.online }]"></span>
                <span>{{ foundUser.online ? '在線中' : '離線' }}</span>
              </div>
            </div>
          </div>

          <div class="card-action">
            <button class="btn btn-primary start-chat-btn" @click="startChatWithUser">
              💬 開始對話
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { userApi } from '../services/api';
import { useChatStore } from '../stores/chatStore';
import { useContactsStore } from '../stores/contactsStore';
import DefaultAvatar from './DefaultAvatar.vue';

const emit = defineEmits(['close']);

const chatStore = useChatStore();
const contactsStore = useContactsStore();

const searchUsername = ref('');
const checking = ref(false);
const errorMessage = ref('');
const foundUser = ref(null);

const handleCheckUser = async () => {
  const username = searchUsername.value.trim();
  if (!username) return;

  checking.value = true;
  errorMessage.value = '';
  foundUser.value = null;

  try {
    const res = await userApi.checkContact(username);
    foundUser.value = res.data;
  } catch (err) {
    errorMessage.value = err.message || '找不到此帳號，請確認帳號是否正確';
  } finally {
    checking.value = false;
  }
};

const startChatWithUser = () => {
  if (!foundUser.value) return;
  chatStore.selectDirectChat(foundUser.value);
  contactsStore.fetchContacts(); // Refresh list
  emit('close');
};
</script>

<style scoped>
.add-friend-dialog {
  max-width: 440px;
}

.search-account-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.search-account-form label {
  font-size: 13px;
  font-weight: 600;
  color: var(--line-text-primary);
}

.input-with-btn {
  display: flex;
  gap: 8px;
}

.check-btn {
  flex-shrink: 0;
  padding: 8px 16px;
}

.error-box {
  background-color: #FEEBEB;
  color: var(--line-badge-red);
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 13px;
  margin-top: 8px;
  line-height: 1.4;
}

.found-user-card {
  background: #FAFAFA;
  border: 1px solid var(--line-primary-light);
  border-left: 4px solid var(--line-primary);
  border-radius: 8px;
  padding: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  animation: fadeIn 0.2s ease;
}

.card-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.card-nickname {
  font-size: 15px;
  font-weight: 600;
  color: var(--line-text-primary);
}

.card-username {
  font-size: 12px;
  color: var(--line-text-secondary);
}

.card-presence {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--line-text-secondary);
  margin-top: 2px;
}

.presence-dot-inline {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #C7C7CC;
}

.presence-dot-inline.online {
  background-color: #05A648;
}

.start-chat-btn {
  padding: 6px 14px;
  font-size: 13px;
}
</style>

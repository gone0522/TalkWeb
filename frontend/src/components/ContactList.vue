<template>
  <div class="contact-list-panel">
    <div class="panel-header">
      <div class="panel-title-row">
        <div class="panel-title">
          好友 <span class="count-badge">({{ contactsStore.filteredContacts.length }})</span>
        </div>
        <button class="add-friend-header-btn" title="搜尋帳號新增好友" @click="showAddFriendModal = true">
          ➕ 新增好友
        </button>
      </div>
      <div class="search-box">
        <span class="search-icon">🔍</span>
        <input
          v-model="contactsStore.searchQuery"
          type="text"
          class="search-input"
          placeholder="搜尋聯絡人或帳號"
        />
        <button
          v-if="contactsStore.searchQuery"
          class="clear-search-btn"
          @click="contactsStore.searchQuery = ''"
        >
          ✕
        </button>
      </div>
    </div>

    <!-- Contact Item List -->
    <div class="contacts-scroll-area">
      <div v-if="contactsStore.loading" class="list-loading">
        載入通訊錄中...
      </div>

      <div
        v-else-if="contactsStore.filteredContacts.length === 0"
        class="empty-contacts"
      >
        <p v-if="contactsStore.searchQuery">查無符合的成員</p>
        <div v-else class="empty-contacts-guide">
          <p>尚無好友</p>
          <span>請點擊上方「➕ 新增好友」透過帳號搜尋並加入同仁</span>
        </div>
      </div>

      <div
        v-for="contact in contactsStore.filteredContacts"
        :key="contact.id"
        :class="['contact-item', { active: isCurrentActive(contact.id) }]"
        @click="selectContact(contact)"
      >
        <div class="avatar-col">
          <DefaultAvatar
            :user-id="contact.id"
            :nickname="contact.nickname"
            :has-custom-avatar="contact.hasCustomAvatar"
            :icon-index="contact.avatarDefaultIcon"
            :size="44"
          />
          <span :class="['presence-dot', { online: contact.online }]"></span>
        </div>

        <div class="info-col">
          <div class="name-row">
            <span class="nickname">{{ contact.nickname }}</span>
            <span v-if="contact.admin" class="admin-tag">管理員</span>
          </div>
          <div class="username-sub">@{{ contact.username }}</div>
        </div>

        <div v-if="getUnread(contact.id) > 0" class="badge-col">
          <span class="badge-unread">{{ getUnread(contact.id) }}</span>
        </div>
      </div>
    </div>

    <!-- Add Friend Modal -->
    <AddFriendModal
      v-if="showAddFriendModal"
      @close="showAddFriendModal = false"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useChatStore } from '../stores/chatStore';
import { useContactsStore } from '../stores/contactsStore';
import AddFriendModal from './AddFriendModal.vue';
import DefaultAvatar from './DefaultAvatar.vue';

const contactsStore = useContactsStore();
const chatStore = useChatStore();

const showAddFriendModal = ref(false);

onMounted(() => {
  contactsStore.fetchContacts();
});

const isCurrentActive = (userId) => {
  return chatStore.chatType === 'DIRECT' && chatStore.chatTarget?.id === userId;
};

const getUnread = (userId) => {
  return chatStore.directUnread[userId] || 0;
};

const selectContact = (contact) => {
  chatStore.selectDirectChat(contact);
};
</script>

<style scoped>
.contact-list-panel {
  width: 280px;
  height: 100%;
  background-color: var(--line-panel-bg);
  border-right: 1px solid var(--line-border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.panel-header {
  padding: 16px 14px 12px;
  border-bottom: 1px solid var(--line-border);
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: var(--line-panel-header-bg);
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.add-friend-header-btn {
  background: var(--line-primary-light);
  color: var(--line-primary);
  border: 1px solid var(--line-primary);
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.add-friend-header-btn:hover {
  background: var(--line-primary);
  color: #FFFFFF;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--line-text-primary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.count-badge {
  font-size: 13px;
  color: var(--line-text-secondary);
  font-weight: 500;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  font-size: 12px;
  color: var(--line-text-secondary);
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 7px 28px 7px 28px;
  font-size: 13px;
  border: 1px solid #DFDFDF;
  border-radius: 16px;
  background-color: #F2F2F2;
  outline: none;
  transition: all 0.2s ease;
}

.search-input:focus {
  background-color: #FFFFFF;
  border-color: var(--line-primary);
  box-shadow: 0 0 0 2px rgba(6, 199, 85, 0.15);
}

.clear-search-btn {
  position: absolute;
  right: 8px;
  background: none;
  border: none;
  font-size: 12px;
  color: #999;
  cursor: pointer;
}

.contacts-scroll-area {
  flex: 1;
  overflow-y: auto;
}

.list-loading, .empty-contacts {
  padding: 24px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--line-text-secondary);
}

.contact-item {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid #F5F5F5;
  transition: background-color 0.15s ease;
}

.contact-item:hover {
  background-color: #F7F9FA;
}

.contact-item.active {
  background-color: var(--line-active-chat);
}

.avatar-col {
  position: relative;
}

.info-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nickname {
  font-size: 14px;
  font-weight: 600;
  color: var(--line-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-tag {
  font-size: 10px;
  background-color: #E8F8EE;
  color: var(--line-primary-hover);
  padding: 1px 4px;
  border-radius: 4px;
  font-weight: 600;
}

.username-sub {
  font-size: 12px;
  color: var(--line-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.badge-col {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .contact-list-panel {
    width: 100%;
    border-right: none;
  }
}
</style>

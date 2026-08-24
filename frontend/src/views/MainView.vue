<template>
  <div class="talkweb-layout-container" :class="{ 'chat-open': Boolean(chatStore.chatTarget) }">
    <!-- Left Navigation Bar (Desktop: 56px left bar, Mobile: bottom bar) -->
    <Sidebar
      class="app-sidebar"
      :current-tab="currentTab"
      @switch-tab="handleSwitchTab"
      @open-profile="showProfileModal = true"
      @open-admin="showAdminModal = true"
    />

    <!-- Middle List (Desktop: 280px, Mobile: Full Width) -->
    <div class="app-list-container">
      <ContactList
        v-if="currentTab === 'contacts'"
      />
      <GroupList
        v-else-if="currentTab === 'groups'"
        @open-create-group="showCreateGroupModal = true"
      />
    </div>

    <!-- Right Chat Area (Desktop: Flexible Width, Mobile: Full Screen when selected) -->
    <main class="chat-main-panel">
      <ChatArea
        v-if="chatStore.chatTarget"
        @open-group-detail="handleOpenGroupDetail"
        @back="handleBackToList"
      />
      <div v-else class="no-chat-selected">
        <div class="welcome-badge-icon">
          <svg viewBox="0 0 100 100" width="80" height="80">
            <defs>
              <linearGradient id="pwcWelGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#D04A02" />
                <stop offset="100%" stop-color="#B33D00" />
              </linearGradient>
            </defs>
            <rect width="100" height="100" rx="24" fill="#2D2D2D" />
            <rect x="20" y="24" width="26" height="26" rx="4" fill="url(#pwcWelGrad)" />
            <rect x="42" y="24" width="22" height="26" rx="4" fill="#E0301E" />
            <rect x="20" y="46" width="44" height="28" rx="6" fill="#EB8C00" />
            <path d="M60 48 C60 48 76 48 76 60 C76 66 70 70 64 71 L66 78 L58 72 Z" fill="#FFFFFF" />
          </svg>
        </div>
        <h2 class="welcome-title">歡迎使用 PwC TalkWeb</h2>
        <p class="welcome-desc">從左側通訊錄選擇同仁或群組開始即時商務對話</p>
      </div>
    </main>

    <!-- Modals -->
    <ChangePasswordModal
      v-if="authStore.mustChangePassword || showChangePasswordModal"
      :is-forced="authStore.mustChangePassword"
      @close="showChangePasswordModal = false"
      @success="handlePasswordChanged"
    />

    <UserProfileModal
      v-if="showProfileModal"
      @close="showProfileModal = false"
      @open-change-password="showChangePasswordModal = true; showProfileModal = false"
    />

    <CreateGroupModal
      v-if="showCreateGroupModal"
      @close="showCreateGroupModal = false"
      @created="handleGroupCreated"
    />

    <GroupDetailModal
      v-if="showGroupDetailModal && selectedGroupForDetail"
      :group="selectedGroupForDetail"
      @close="showGroupDetailModal = false"
      @left="handleGroupLeft"
    />

    <AdminModal
      v-if="showAdminModal"
      @close="showAdminModal = false"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import AdminModal from '../components/AdminModal.vue';
import ChangePasswordModal from '../components/ChangePasswordModal.vue';
import ChatArea from '../components/ChatArea.vue';
import ContactList from '../components/ContactList.vue';
import CreateGroupModal from '../components/CreateGroupModal.vue';
import GroupDetailModal from '../components/GroupDetailModal.vue';
import GroupList from '../components/GroupList.vue';
import Sidebar from '../components/Sidebar.vue';
import UserProfileModal from '../components/UserProfileModal.vue';
import wsService from '../services/websocket';
import { useAuthStore } from '../stores/authStore';
import { useChatStore } from '../stores/chatStore';
import { useContactsStore } from '../stores/contactsStore';
import { useGroupStore } from '../stores/groupStore';

const authStore = useAuthStore();
const chatStore = useChatStore();
const contactsStore = useContactsStore();
const groupStore = useGroupStore();

const currentTab = ref('contacts'); // 'contacts' | 'groups'
const showProfileModal = ref(false);
const showChangePasswordModal = ref(false);
const showCreateGroupModal = ref(false);
const showGroupDetailModal = ref(false);
const showAdminModal = ref(false);
const selectedGroupForDetail = ref(null);

onMounted(() => {
  // Ensure WebSocket is connected
  if (authStore.token) {
    wsService.connect(authStore.token);
  }

  // Register global STOMP handlers
  wsService.onMessageReceived = (msg) => {
    chatStore.receiveMessage(msg);
  };

  wsService.onReadReceiptReceived = (receipt) => {
    chatStore.handleDirectReadReceipt(receipt);
  };

  wsService.onPresenceChanged = (presence) => {
    contactsStore.updatePresence(presence.userId, presence.online);
  };
});

const handleSwitchTab = (tab) => {
  currentTab.value = tab;
};

const handleBackToList = () => {
  chatStore.chatTarget = null;
  chatStore.chatType = null;
};

const handleOpenGroupDetail = async (groupId) => {
  try {
    const detail = await groupStore.fetchGroupDetails(groupId);
    selectedGroupForDetail.value = detail;
    showGroupDetailModal.value = true;
  } catch (err) {
    alert(err.message || '無法載入群組資訊');
  }
};

const handleGroupCreated = (newGroup) => {
  chatStore.selectGroupChat(newGroup);
};

const handleGroupLeft = () => {
  if (chatStore.chatType === 'GROUP') {
    chatStore.chatType = null;
    chatStore.chatTarget = null;
  }
};

const handlePasswordChanged = () => {
  // If first time login password changed, reload user
  authStore.fetchCurrentUser();
};
</script>

<style scoped>
.talkweb-layout-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  background-color: #FFFFFF;
  overflow: hidden;
  position: relative;
}

.app-list-container {
  display: flex;
  height: 100%;
  flex-shrink: 0;
}

.chat-main-panel {
  flex: 1;
  height: 100%;
  overflow: hidden;
  background-color: var(--line-bg-chat);
  display: flex;
  flex-direction: column;
}

.no-chat-selected {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  background-color: #F8FAF9;
  user-select: none;
}

.welcome-badge-icon {
  filter: drop-shadow(0 6px 14px rgba(6, 199, 85, 0.2));
}

.welcome-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--line-text-primary);
}

.welcome-desc {
  font-size: 13px;
  color: var(--line-text-secondary);
}

/* Responsive Web Design for Mobile */
@media (max-width: 768px) {
  .talkweb-layout-container {
    flex-direction: column;
  }

  /* When NO chat is open on mobile: show list on top, navigation bar at bottom */
  .talkweb-layout-container:not(.chat-open) .app-list-container {
    flex: 1;
    width: 100%;
    height: calc(100vh - 56px);
    display: flex;
  }

  .talkweb-layout-container:not(.chat-open) .app-sidebar {
    width: 100%;
    height: 56px;
    flex-shrink: 0;
    order: 2;
  }

  .talkweb-layout-container:not(.chat-open) .chat-main-panel {
    display: none;
  }

  /* When CHAT IS OPEN on mobile: hide sidebar and list, show chat in full screen */
  .talkweb-layout-container.chat-open .app-sidebar {
    display: none;
  }

  .talkweb-layout-container.chat-open .app-list-container {
    display: none;
  }

  .talkweb-layout-container.chat-open .chat-main-panel {
    width: 100vw;
    height: 100vh;
    display: flex;
  }
}
</style>

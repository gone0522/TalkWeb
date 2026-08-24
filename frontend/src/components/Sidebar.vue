<template>
  <aside class="line-sidebar">
    <div class="sidebar-top">
      <div class="user-avatar-wrap" @click="$emit('openProfile')" title="個人設定">
        <DefaultAvatar
          :user-id="authStore.user?.id"
          :nickname="authStore.user?.nickname"
          :has-custom-avatar="authStore.user?.hasCustomAvatar"
          :icon-index="authStore.user?.avatarDefaultIcon"
          :size="36"
        />
        <span class="presence-dot online"></span>
      </div>

      <!-- Navigation Tabs -->
      <nav class="sidebar-nav">
        <button
          :class="['nav-item', { active: currentTab === 'contacts' }]"
          title="好友與通訊錄"
          @click="$emit('switchTab', 'contacts')"
        >
          <span class="nav-icon">👤</span>
          <span class="nav-label">好友</span>
        </button>

        <button
          :class="['nav-item', { active: currentTab === 'groups' }]"
          title="群組與聊天"
          @click="$emit('switchTab', 'groups')"
        >
          <span class="nav-icon">👥</span>
          <span class="nav-label">群組</span>
          <span v-if="chatStore.totalUnreadCount > 0" class="badge-unread nav-badge">
            {{ chatStore.totalUnreadCount > 99 ? '99+' : chatStore.totalUnreadCount }}
          </span>
        </button>

        <button
          v-if="authStore.isAdmin"
          :class="['nav-item', { active: currentTab === 'admin' }]"
          title="管理員面板"
          @click="$emit('openAdmin')"
        >
          <span class="nav-icon">🛡️</span>
          <span class="nav-label">管理</span>
        </button>
      </nav>
    </div>

    <!-- Bottom Actions -->
    <div class="sidebar-bottom">
      <button class="nav-item" title="個人設定" @click="$emit('openProfile')">
        <span class="nav-icon">⚙️</span>
        <span class="nav-label">設定</span>
      </button>
      <button class="nav-item logout-btn" title="登出" @click="handleLogout">
        <span class="nav-icon">🚪</span>
        <span class="nav-label">登出</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { useAuthStore } from '../stores/authStore';
import { useChatStore } from '../stores/chatStore';
import DefaultAvatar from './DefaultAvatar.vue';

defineProps({
  currentTab: {
    type: String,
    default: 'contacts'
  }
});

const emit = defineEmits(['switchTab', 'openProfile', 'openAdmin']);

const authStore = useAuthStore();
const chatStore = useChatStore();

const handleLogout = () => {
  if (confirm('確定要登出系統嗎？')) {
    authStore.logout();
    window.location.reload();
  }
};
</script>

<style scoped>
.line-sidebar {
  width: 56px;
  height: 100%;
  background-color: var(--line-sidebar-bg);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0 12px;
  flex-shrink: 0;
  user-select: none;
}

.sidebar-top {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  width: 100%;
}

.user-avatar-wrap {
  position: relative;
  cursor: pointer;
  transition: transform 0.15s ease;
}

.user-avatar-wrap:hover {
  transform: scale(1.05);
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.nav-item {
  width: 100%;
  height: 48px;
  background: none;
  border: none;
  color: #8E9BAE;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  transition: all 0.15s ease;
}

.nav-icon {
  font-size: 20px;
}

.nav-label {
  display: none;
}

.nav-item:hover {
  color: #FFFFFF;
  background-color: var(--line-sidebar-hover);
}

.nav-item.active {
  color: #FFFFFF;
  background-color: var(--line-sidebar-hover);
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  background-color: var(--line-primary);
  border-top-right-radius: 2px;
  border-bottom-right-radius: 2px;
}

.nav-badge {
  position: absolute;
  top: 6px;
  right: 8px;
}

.sidebar-bottom {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.logout-btn:hover {
  color: #FF6B6B;
}

/* Mobile Bottom Navigation Bar */
@media (max-width: 768px) {
  .line-sidebar {
    width: 100%;
    height: 56px;
    flex-direction: row;
    justify-content: space-around;
    padding: 0 6px;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.15);
    z-index: 50;
  }

  .sidebar-top {
    flex-direction: row;
    align-items: center;
    gap: 0;
    flex: 1;
    height: 100%;
    justify-content: space-around;
  }

  .user-avatar-wrap {
    display: none;
  }

  .sidebar-nav {
    flex-direction: row;
    gap: 0;
    flex: 1;
    height: 100%;
    justify-content: space-around;
  }

  .sidebar-bottom {
    flex-direction: row;
    gap: 0;
    width: auto;
    flex: 0.7;
    height: 100%;
    justify-content: space-around;
  }

  .nav-item {
    width: auto;
    flex: 1;
    height: 100%;
    flex-direction: column;
    gap: 2px;
    padding: 4px 0;
  }

  .nav-icon {
    font-size: 18px;
    line-height: 1;
  }

  .nav-label {
    display: block;
    font-size: 10px;
    font-weight: 500;
    color: #8E9BAE;
    line-height: 1.2;
  }

  .nav-item.active .nav-label {
    color: #FFFFFF;
    font-weight: 600;
  }

  .nav-item.active::before {
    left: 20%;
    right: 20%;
    top: 0;
    bottom: auto;
    width: 60%;
    height: 3px;
    border-radius: 0 0 2px 2px;
  }

  .nav-badge {
    top: 2px;
    right: calc(50% - 18px);
  }
}
</style>

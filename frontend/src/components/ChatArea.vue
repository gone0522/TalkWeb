<template>
  <div class="chat-area-container">
    <!-- Top Header -->
    <header class="chat-header">
      <div class="header-left">
        <DefaultAvatar
          v-if="isDirect"
          :user-id="chatStore.chatTarget?.id"
          :nickname="chatStore.chatTarget?.nickname"
          :has-custom-avatar="chatStore.chatTarget?.hasCustomAvatar"
          :icon-index="chatStore.chatTarget?.avatarDefaultIcon"
          :size="38"
        />
        <div v-else class="group-header-icon">
          👥
        </div>

        <div class="header-title-wrap">
          <div class="header-title">
            {{ isDirect ? chatStore.chatTarget?.nickname : chatStore.chatTarget?.name }}
            <span v-if="!isDirect && groupMemberCount" class="member-count">({{ groupMemberCount }})</span>
          </div>
          <div class="header-subtitle">
            <span v-if="isDirect" :class="['status-dot', { online: isTargetOnline }]"></span>
            <span v-if="isDirect">{{ isTargetOnline ? '在線' : '離線' }}</span>
            <span v-else>{{ chatStore.chatTarget?.announcement || '群組聊天室' }}</span>
          </div>
        </div>
      </div>

      <div class="header-actions">
        <button
          v-if="!isDirect"
          class="header-btn"
          title="群組資訊與成員"
          @click="$emit('openGroupDetail', chatStore.chatTarget?.id)"
        >
          ⚙️
        </button>
      </div>
    </header>

    <!-- Message List -->
    <div ref="messageContainer" class="message-list-viewport" @scroll="onScroll">
      <div v-if="chatStore.loadingHistory" class="loading-history-indicator">
        載入歷史訊息中...
      </div>

      <div v-if="chatStore.messages.length === 0" class="empty-chat-state">
        <div class="empty-icon">💬</div>
        <div class="empty-text">尚無任何訊息，開始對話吧！</div>
      </div>

      <MessageBubble
        v-for="msg in chatStore.messages"
        :key="msg.id"
        :message="msg"
        :current-user-id="authStore.user?.id"
        :show-sender-name="!isDirect"
      />
    </div>

    <!-- Bottom Input Bar -->
    <footer class="chat-input-bar">
      <!-- Emoji Picker Popup -->
      <div v-if="showEmojiPicker" class="emoji-picker-anchor">
        <EmojiPicker @select="onEmojiSelect" />
      </div>

      <div class="input-tools">
        <button
          type="button"
          class="tool-btn"
          :class="{ active: showEmojiPicker }"
          title="插入表情符號"
          @click.stop="showEmojiPicker = !showEmojiPicker"
        >
          😊
        </button>
      </div>

      <textarea
        ref="inputTextarea"
        v-model="inputText"
        class="chat-textarea"
        placeholder="輸入訊息... (Enter 發送, Shift+Enter 換行)"
        rows="2"
        @keydown.enter.exact.prevent="handleSend"
        @click="showEmojiPicker = false"
      ></textarea>

      <div class="input-actions">
        <button
          type="button"
          class="btn btn-primary send-btn"
          :disabled="!inputText.trim()"
          @click="handleSend"
        >
          發送
        </button>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue';
import { useAuthStore } from '../stores/authStore';
import { useChatStore } from '../stores/chatStore';
import { useContactsStore } from '../stores/contactsStore';
import DefaultAvatar from './DefaultAvatar.vue';
import EmojiPicker from './EmojiPicker.vue';
import MessageBubble from './MessageBubble.vue';

defineEmits(['openGroupDetail']);

const authStore = useAuthStore();
const chatStore = useChatStore();
const contactsStore = useContactsStore();

const inputText = ref('');
const showEmojiPicker = ref(false);
const messageContainer = ref(null);
const inputTextarea = ref(null);

const isDirect = computed(() => chatStore.chatType === 'DIRECT');

const isTargetOnline = computed(() => {
  if (!isDirect.value || !chatStore.chatTarget) return false;
  const contact = contactsStore.contacts.find(c => c.id === chatStore.chatTarget.id);
  return contact ? Boolean(contact.online) : Boolean(chatStore.chatTarget.online);
});

const groupMemberCount = computed(() => {
  return chatStore.chatTarget?.members?.length || 0;
});

const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
    }
  });
};

// Scroll to bottom when new messages arrive or conversation switches
watch(() => chatStore.messages.length, () => {
  scrollToBottom();
});

watch(() => chatStore.chatTarget?.id, () => {
  showEmojiPicker.value = false;
  inputText.value = '';
  scrollToBottom();
});

const onScroll = () => {
  if (!messageContainer.value) return;
  if (messageContainer.value.scrollTop === 0 && chatStore.hasMoreHistory && !chatStore.loadingHistory) {
    const firstMsg = chatStore.messages[0];
    if (firstMsg) {
      const prevHeight = messageContainer.value.scrollHeight;
      if (isDirect.value) {
        chatStore.loadDirectHistory(chatStore.chatTarget.id, firstMsg.id).then(() => {
          nextTick(() => {
            messageContainer.value.scrollTop = messageContainer.value.scrollHeight - prevHeight;
          });
        });
      } else {
        chatStore.loadGroupHistory(chatStore.chatTarget.id, firstMsg.id).then(() => {
          nextTick(() => {
            messageContainer.value.scrollTop = messageContainer.value.scrollHeight - prevHeight;
          });
        });
      }
    }
  }
};

const onEmojiSelect = (emoji) => {
  inputText.value += emoji;
  if (inputTextarea.value) {
    inputTextarea.value.focus();
  }
};

const handleSend = () => {
  const text = inputText.value.trim();
  if (!text) return;
  chatStore.sendMessage(text, 'TEXT');
  inputText.value = '';
  showEmojiPicker.value = false;
  scrollToBottom();
};
</script>

<style scoped>
.chat-area-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--line-bg-chat);
  position: relative;
}

.chat-header {
  height: 56px;
  background-color: #FFFFFF;
  border-bottom: 1px solid var(--line-border);
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.group-header-icon {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background-color: #E8F8EE;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.header-title-wrap {
  display: flex;
  flex-direction: column;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--line-text-primary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.member-count {
  font-size: 13px;
  color: var(--line-text-secondary);
  font-weight: normal;
}

.header-subtitle {
  font-size: 11px;
  color: var(--line-text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background-color: #C7C7CC;
}

.status-dot.online {
  background-color: var(--line-primary);
}

.header-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  color: var(--line-text-secondary);
  transition: background-color 0.15s ease;
}

.header-btn:hover {
  background-color: #F0F0F0;
}

.message-list-viewport {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
}

.loading-history-indicator {
  text-align: center;
  font-size: 11px;
  color: var(--line-text-secondary);
  padding: 6px 0;
}

.empty-chat-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--line-text-secondary);
  gap: 10px;
  user-select: none;
}

.empty-icon {
  font-size: 42px;
  opacity: 0.6;
}

.empty-text {
  font-size: 13px;
}

/* Chat Input Bar */
.chat-input-bar {
  background-color: #FFFFFF;
  border-top: 1px solid var(--line-border);
  padding: 8px 16px 12px;
  display: flex;
  flex-direction: column;
  position: relative;
}

.emoji-picker-anchor {
  position: absolute;
  bottom: 85px;
  left: 16px;
  z-index: 100;
}

.input-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.tool-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.15s ease;
}

.tool-btn:hover, .tool-btn.active {
  background-color: #F0F0F0;
}

.chat-textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 14px;
  font-family: inherit;
  line-height: 1.4;
  color: var(--line-text-primary);
  max-height: 100px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}

.send-btn {
  padding: 6px 18px;
  font-size: 13px;
  border-radius: 16px;
}
</style>

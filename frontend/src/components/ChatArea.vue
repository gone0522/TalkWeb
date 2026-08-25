<template>
  <div class="chat-area-container">
    <!-- Top Header -->
    <header class="chat-header">
      <div class="header-left">
        <!-- Mobile Back Button -->
        <button
          class="mobile-back-btn"
          title="返回通訊錄"
          @click="$emit('back')"
        >
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
        </button>

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
        @preview-image="handlePreviewImage"
      />
    </div>

    <!-- Uploading / Compressing Indicator -->
    <div v-if="isCompressing" class="compressing-banner">
      <span class="spinner-small"></span>
      <span>圖片壓縮與傳送中...</span>
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

        <button
          type="button"
          class="tool-btn"
          title="傳送圖片"
          :disabled="isCompressing"
          @click="triggerImageUpload"
        >
          🖼️
        </button>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          class="hidden-file-input"
          @change="onImageFileChange"
        />
      </div>

      <textarea
        ref="inputTextarea"
        v-model="inputText"
        class="chat-textarea"
        placeholder="輸入訊息... (Enter 發送, Shift+Enter 換行, 支援直接貼上圖片)"
        rows="2"
        @keydown.enter.exact.prevent="handleSend"
        @click="showEmojiPicker = false"
        @paste="handlePaste"
      ></textarea>

      <div class="input-actions">
        <button
          type="button"
          class="btn btn-primary send-btn"
          :disabled="!inputText.trim() && !isCompressing"
          @click="handleSend"
        >
          發送
        </button>
      </div>
    </footer>

    <!-- Image Lightbox Modal -->
    <ImageLightbox
      v-if="previewImageUrl"
      :src="previewImageUrl"
      @close="previewImageUrl = null"
    />
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue';
import { useAuthStore } from '../stores/authStore';
import { useChatStore } from '../stores/chatStore';
import { useContactsStore } from '../stores/contactsStore';
import DefaultAvatar from './DefaultAvatar.vue';
import EmojiPicker from './EmojiPicker.vue';
import ImageLightbox from './ImageLightbox.vue';
import MessageBubble from './MessageBubble.vue';

defineEmits(['openGroupDetail', 'back']);

const authStore = useAuthStore();
const chatStore = useChatStore();
const contactsStore = useContactsStore();

const inputText = ref('');
const showEmojiPicker = ref(false);
const messageContainer = ref(null);
const inputTextarea = ref(null);
const fileInputRef = ref(null);
const isCompressing = ref(false);
const previewImageUrl = ref(null);

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
  previewImageUrl.value = null;
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

const triggerImageUpload = () => {
  if (fileInputRef.value) {
    fileInputRef.value.click();
  }
};

// Canvas-based client-side image compression
const compressImage = (file, maxWidth = 1280, maxHeight = 1280, quality = 0.82) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        let { width, height } = img;
        if (width > maxWidth || height > maxHeight) {
          if (width / height > maxWidth / maxHeight) {
            height = Math.round((height * maxWidth) / width);
            width = maxWidth;
          } else {
            width = Math.round((width * maxHeight) / height);
            height = maxHeight;
          }
        }
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);
        const dataUrl = canvas.toDataURL('image/jpeg', quality);
        resolve(dataUrl);
      };
      img.onerror = reject;
      img.src = e.target.result;
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
};

const sendImageFile = async (file) => {
  if (!file || !file.type.startsWith('image/')) {
    alert('請選擇有效的圖片檔案');
    return;
  }
  isCompressing.value = true;
  try {
    const compressedDataUrl = await compressImage(file);
    await chatStore.sendMessage(compressedDataUrl, 'IMAGE');
    scrollToBottom();
  } catch (err) {
    console.error('圖片壓縮發送失敗:', err);
    alert('圖片壓縮處理失敗，請重試');
  } finally {
    isCompressing.value = false;
    if (fileInputRef.value) {
      fileInputRef.value.value = '';
    }
  }
};

const onImageFileChange = async (e) => {
  const file = e.target.files?.[0];
  if (file) {
    await sendImageFile(file);
  }
};

// Clipboard image paste support
const handlePaste = async (e) => {
  const items = e.clipboardData?.items;
  if (!items) return;
  for (const item of items) {
    if (item.type.indexOf('image') !== -1) {
      const file = item.getAsFile();
      if (file) {
        e.preventDefault();
        await sendImageFile(file);
        break;
      }
    }
  }
};

const handlePreviewImage = (imageUrl) => {
  previewImageUrl.value = imageUrl;
};
</script>

<style scoped>
.chat-area-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  height: 100dvh;
  max-height: 100%;
  background-color: var(--line-bg-chat);
  position: relative;
  overflow: hidden;
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
  flex-shrink: 0;
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
  flex: 1 1 0%;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior-y: contain;
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

.hidden-file-input {
  display: none !important;
}

.compressing-banner {
  background-color: #E8F8EE;
  color: var(--line-primary-hover);
  padding: 6px 16px;
  font-size: 12px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  border-top: 1px solid rgba(6, 199, 85, 0.2);
  animation: fadeIn 0.2s ease;
}

.spinner-small {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(6, 199, 85, 0.2);
  border-top-color: var(--line-primary);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Chat Input Bar */
.chat-input-bar {
  flex-shrink: 0;
  background-color: #FFFFFF;
  border-top: 1px solid var(--line-border);
  padding: 8px 16px 12px;
  padding-bottom: max(12px, env(safe-area-inset-bottom, 12px));
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 10;
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

.tool-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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
  min-height: 40px;
  max-height: 100px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}

.send-btn {
  padding: 6px 14px;
  font-size: 13px;
}

.mobile-back-btn {
  display: none;
  background: none;
  border: none;
  padding: 6px;
  margin-right: 2px;
  margin-left: -6px;
  cursor: pointer;
  color: var(--line-text-primary);
  border-radius: 50%;
  align-items: center;
  justify-content: center;
  transition: background-color 0.15s ease;
}

.mobile-back-btn:hover {
  background-color: #F0F0F0;
}

/* Mobile Responsive Styling */
@media (max-width: 768px) {
  .mobile-back-btn {
    display: flex;
  }

  .chat-header {
    height: 52px;
    padding: 0 12px;
  }

  .message-list-viewport {
    padding: 10px 12px;
  }

  .chat-input-bar {
    padding: 6px 10px;
    padding-bottom: max(10px, env(safe-area-inset-bottom, 10px));
  }

  .chat-textarea {
    font-size: 14px;
    min-height: 36px;
    max-height: 76px;
  }

  .emoji-picker-anchor {
    left: 8px;
    right: 8px;
    bottom: 75px;
  }
}
</style>

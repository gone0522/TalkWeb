<template>
  <div :class="['message-row', isMe ? 'me' : 'other']">
    <!-- Avatar (only for other users) -->
    <div v-if="!isMe" class="message-avatar-wrap">
      <DefaultAvatar
        :user-id="message.senderId"
        :nickname="message.senderNickname || message.senderUsername"
        :has-custom-avatar="message.senderHasCustomAvatar"
        :icon-index="message.senderDefaultAvatarIcon"
        :size="36"
      />
    </div>

    <!-- Message Content & Meta -->
    <div class="message-body-wrap">
      <!-- Sender Nickname (for group chat or others) -->
      <div v-if="!isMe && showSenderName" class="sender-nickname">
        {{ message.senderNickname || message.senderUsername }}
      </div>

      <div class="bubble-with-meta">
        <!-- Status Meta for ME (on the left of bubble) -->
        <div v-if="isMe" class="meta-col me-meta">
          <span v-if="readStatusText" class="read-status">{{ readStatusText }}</span>
          <span class="message-time">{{ formattedTime }}</span>
        </div>

        <!-- The Message Bubble -->
        <div :class="['message-bubble', isMe ? 'bubble-me' : 'bubble-other', { 'pure-emoji': isPureEmoji, 'is-image-bubble': isImageMessage }]">
          <!-- Image Content -->
          <div v-if="isImageMessage" class="bubble-image-wrap" @click="$emit('previewImage', message.content)">
            <img
              :src="message.content"
              alt="聊天圖片"
              class="chat-image-content"
              loading="lazy"
            />
            <div class="image-zoom-hint" title="點擊放大檢視">
              <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.5" fill="none">
                <circle cx="11" cy="11" r="7"></circle>
                <line x1="21" y1="21" x2="16.5" y2="16.5"></line>
                <line x1="11" y1="8" x2="11" y2="14"></line>
                <line x1="8" y1="11" x2="14" y2="11"></line>
              </svg>
            </div>
          </div>

          <!-- Text Content with Auto-Link -->
          <div v-else class="bubble-text" v-html="renderedContent"></div>

          <!-- Link Preview Card if present -->
          <UrlPreviewCard
            v-if="!isImageMessage && message.linkPreview"
            :preview="message.linkPreview"
          />
        </div>

        <!-- Status Meta for OTHER (on the right of bubble) -->
        <div v-if="!isMe" class="meta-col other-meta">
          <span class="message-time">{{ formattedTime }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import DefaultAvatar from './DefaultAvatar.vue';
import UrlPreviewCard from './UrlPreviewCard.vue';

const props = defineProps({
  message: {
    type: Object,
    required: true
  },
  currentUserId: {
    type: Number,
    required: true
  },
  showSenderName: {
    type: Boolean,
    default: true
  }
});

defineEmits(['previewImage']);

const isMe = computed(() => {
  return props.message.senderId === props.currentUserId;
});

const isImageMessage = computed(() => {
  return props.message.type === 'IMAGE' || (props.message.content && props.message.content.startsWith('data:image/'));
});

const formattedTime = computed(() => {
  if (!props.message.createdAt) return '';
  const d = new Date(props.message.createdAt);
  const hours = d.getHours().toString().padStart(2, '0');
  const minutes = d.getMinutes().toString().padStart(2, '0');
  return `${hours}:${minutes}`;
});

const readStatusText = computed(() => {
  if (!isMe.value) return '';
  if (props.message.groupId) {
    return props.message.readCount > 0 ? `已讀 ${props.message.readCount}` : '';
  } else {
    return props.message.read ? '已讀' : '';
  }
});

// Escape HTML and replace URLs with safe clickable anchor tags
const renderedContent = computed(() => {
  const text = props.message.content || '';
  // 1. Escape HTML
  const div = document.createElement('div');
  div.textContent = text;
  const escaped = div.innerHTML;

  // 2. Replace URLs with link
  const urlRegex = /(https?:\/\/[^\s<]+)/g;
  return escaped.replace(urlRegex, (url) => {
    return `<a href="${url}" target="_blank" rel="noopener noreferrer" class="bubble-link">${url}</a>`;
  });
});

// Check if content is purely emojis (1-3 emojis)
const isPureEmoji = computed(() => {
  if (isImageMessage.value) return false;
  const text = props.message.content?.trim();
  if (!text) return false;
  // Emoji regex check
  const emojiRegex = /^(\p{Extended_Pictographic}|\u200d|\ufe0f){1,3}$/u;
  return emojiRegex.test(text);
});
</script>

<style scoped>
.message-row {
  display: flex;
  margin-bottom: 12px;
  gap: 8px;
  animation: fadeIn 0.15s ease;
}

.message-row.me {
  justify-content: flex-end;
}

.message-row.other {
  justify-content: flex-start;
}

.message-avatar-wrap {
  margin-top: 2px;
  flex-shrink: 0;
}

.message-body-wrap {
  display: flex;
  flex-direction: column;
  max-width: 75%;
}

.sender-nickname {
  font-size: 12px;
  color: var(--line-text-secondary);
  margin-bottom: 3px;
  margin-left: 4px;
}

.bubble-with-meta {
  display: flex;
  align-items: flex-end;
  gap: 6px;
}

.message-row.me .bubble-with-meta {
  flex-direction: row;
}

.meta-col {
  display: flex;
  flex-direction: column;
  font-size: 11px;
  color: var(--line-text-secondary);
  line-height: 1.2;
  user-select: none;
  flex-shrink: 0;
}

.me-meta {
  align-items: flex-end;
}

.other-meta {
  align-items: flex-start;
}

.read-status {
  color: var(--line-primary);
  font-size: 10px;
  font-weight: 600;
  margin-bottom: 1px;
}

.message-time {
  font-size: 10px;
  color: var(--line-text-muted);
}

/* Message Bubbles */
.message-bubble {
  position: relative;
  padding: 10px 14px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.45;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.message-bubble.is-image-bubble {
  padding: 4px;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

.bubble-me {
  background-color: var(--line-bubble-me);
  color: #111111;
  border-top-right-radius: 4px;
}

.bubble-other {
  background-color: var(--line-bubble-other);
  color: #1E1E1E;
  border: 1px solid var(--line-bubble-border);
  border-top-left-radius: 4px;
}

.bubble-text :deep(.bubble-link) {
  color: #0366D6;
  text-decoration: underline;
  word-break: break-all;
}

.bubble-me .bubble-text :deep(.bubble-link) {
  color: #0D47A1;
}

.pure-emoji {
  background: transparent !important;
  box-shadow: none !important;
  border: none !important;
  font-size: 40px !important;
  padding: 4px 8px !important;
}

/* Image Bubble Styling */
.bubble-image-wrap {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  cursor: zoom-in;
  display: inline-block;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  max-width: 260px;
  background-color: #E8ECEF;
}

.bubble-image-wrap:hover {
  transform: scale(1.01);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.18);
}

.chat-image-content {
  display: block;
  max-width: 100%;
  max-height: 320px;
  width: auto;
  height: auto;
  border-radius: 14px;
  object-fit: cover;
}

.image-zoom-hint {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.55);
  color: #FFFFFF;
  border-radius: 50%;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
  pointer-events: none;
}

.bubble-image-wrap:hover .image-zoom-hint {
  opacity: 1;
}

@media (max-width: 768px) {
  .message-body-wrap {
    max-width: 86%;
  }

  .message-bubble {
    padding: 8px 12px;
    font-size: 14px;
  }

  .bubble-image-wrap {
    max-width: 220px;
  }

  .image-zoom-hint {
    opacity: 0.75;
  }
}
</style>

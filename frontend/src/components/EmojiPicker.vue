<template>
  <div class="emoji-picker-container" @click.stop>
    <div class="emoji-tabs">
      <button
        v-for="(tab, index) in tabs"
        :key="index"
        :class="['tab-btn', { active: currentTab === index }]"
        @click="currentTab = index"
      >
        {{ tab.icon }}
      </button>
    </div>
    <div class="emoji-grid">
      <span
        v-for="emoji in tabs[currentTab].emojis"
        :key="emoji"
        class="emoji-item"
        @click="selectEmoji(emoji)"
      >
        {{ emoji }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const emit = defineEmits(['select']);

const currentTab = ref(0);

const tabs = [
  {
    name: '表情',
    icon: '😀',
    emojis: [
      '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '😉', '😊', '😇',
      '🥰', '😍', '🤩', '😘', '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗', '🤭',
      '🤫', '🤔', '🤐', '🤨', '😐', '😑', '😶', '😏', '😒', '🙄', '😬', '🤥',
      '😌', '😔', '😪', '🤤', '😴', '😷', '🤒', '🤕', '🤢', '🤮', '🤧', '🥵',
      '🥶', '🥴', '😵', '🤯', '🤠', '🥳', '😎', '🤓', '🧐', '😕', '😟', '🙁',
      '😮', '😯', '😲', '😳', '🥺', '😦', '😧', '😨', '😰', '😥', '😢', '😭',
      '😱', '😖', '😣', '😞', '😓', '😩', '😫', '🥱', '😤', '😡', '😠', '🤬'
    ]
  },
  {
    name: '手勢',
    icon: '👍',
    emojis: [
      '👍', '👎', '👌', '✌️', '🤞', '🤟', '🤘', '🤙', '👈', '👉', '👆', '🖕',
      '👇', '☝️', '👋', '🤚', '🖐️', '✋', '🖖', '👏', '🙌', '👐', '🤲', '🤝',
      '🙏', '✍️', '💪', '🦾', '👂', '👃', '👀', '👁️', '👅', '👄', '💋', '❤️',
      '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️', '💕', '💞',
      '💓', '💗', '💖', '💘', '💝', '💟', '☮️', '✝️', '🔥', '✨', '🌟', '💫'
    ]
  },
  {
    name: '動物與食物',
    icon: '🐱',
    emojis: [
      '🐶', '🐱', '🐭', '🐹', '🐰', '🦊', '🐻', '🐼', '🐨', '🐯', '🦁', '🐮',
      '🐷', '🐸', '🐵', '🐔', '🐧', '🐦', '🐤', '🦆', '🦅', '🦉', '🦇', '🐺',
      '🍏', '🍎', '🍐', '🍊', '🍋', '🍌', '🍉', '🍇', '🍓', '🍈', '🍒', '🍑',
      '🥭', '🍍', '🥥', '🥝', '🍅', '🥑', '🍔', '🍟', '🍕', '🌭', '🥪', '🌮',
      '🌯', '🍜', '🍲', '🍣', '🍱', '🍦', '🍧', '🍰', '🎂', '☕', '🍵', '🧋'
    ]
  },
  {
    name: '活動與符號',
    icon: '🎉',
    emojis: [
      '🎉', '🎊', '🎈', '🎁', '🏆', '🥇', '🥈', '🥉', '⚽', '🏀', '🏈', '⚾',
      '🎾', '🏐', '🏉', '🎱', '🎮', '🎯', '🎲', '🧩', '🚗', '🚕', '🚙', '🚌',
      '🚀', '🛸', '⛵', '⏰', '📱', '💻', '💡', '📌', '📎', '🔑', '🔒', '🔔',
      '📢', '📣', '✅', '❌', '⚠️', '🚫', '💯', '🆗', '🆒', '🆕', '🆓', '⭐'
    ]
  }
];

const selectEmoji = (emoji) => {
  emit('select', emoji);
};
</script>

<style scoped>
.emoji-picker-container {
  width: 280px;
  background: #FFFFFF;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  border: 1px solid var(--line-border);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  user-select: none;
  z-index: 100;
}

.emoji-tabs {
  display: flex;
  border-bottom: 1px solid var(--line-border);
  background-color: #F8F8F8;
}

.tab-btn {
  flex: 1;
  padding: 8px 0;
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.15s ease;
}

.tab-btn.active {
  border-bottom-color: var(--line-primary);
  background-color: #FFFFFF;
}

.emoji-grid {
  height: 200px;
  overflow-y: auto;
  padding: 8px;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 4px;
}

.emoji-item {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  padding: 4px;
  border-radius: 6px;
  cursor: pointer;
  transition: transform 0.1s ease, background-color 0.1s ease;
}

.emoji-item:hover {
  background-color: #E8F8EE;
  transform: scale(1.2);
}
</style>

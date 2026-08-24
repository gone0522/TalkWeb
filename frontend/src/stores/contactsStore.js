import { defineStore } from 'pinia';
import { userApi } from '../services/api';

export const useContactsStore = defineStore('contacts', {
  state: () => ({
    contacts: [],
    searchQuery: '',
    loading: false
  }),

  getters: {
    filteredContacts: (state) => {
      if (!state.searchQuery.trim()) {
        return state.contacts;
      }
      const q = state.searchQuery.toLowerCase();
      return state.contacts.filter(c => 
        (c.nickname && c.nickname.toLowerCase().includes(q)) ||
        (c.username && c.username.toLowerCase().includes(q))
      );
    }
  },

  actions: {
    async fetchContacts() {
      this.loading = true;
      try {
        const response = await userApi.getContacts();
        this.contacts = response.data || [];
      } catch (err) {
        console.error('取得通訊錄失敗:', err);
      } finally {
        this.loading = false;
      }
    },

    updatePresence(userId, isOnline) {
      const contact = this.contacts.find(c => c.id === userId);
      if (contact) {
        contact.online = isOnline;
      }
    }
  }
});

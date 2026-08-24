import { defineStore } from 'pinia';
import { groupApi } from '../services/api';

export const useGroupStore = defineStore('group', {
  state: () => ({
    groups: [],
    activeGroup: null,
    loading: false
  }),

  actions: {
    async fetchGroups() {
      this.loading = true;
      try {
        const response = await groupApi.getUserGroups();
        this.groups = response.data || [];
      } catch (err) {
        console.error('取得群組列表失敗:', err);
      } finally {
        this.loading = false;
      }
    },

    async createGroup(name, memberIds, icon = 1, announcement = '') {
      const response = await groupApi.createGroup({ name, memberIds, icon, announcement });
      const newGroup = response.data;
      this.groups.unshift(newGroup);
      return newGroup;
    },

    async fetchGroupDetails(groupId) {
      try {
        const response = await groupApi.getGroupDetails(groupId);
        this.activeGroup = response.data;
        // Update in list
        const idx = this.groups.findIndex(g => g.id === groupId);
        if (idx !== -1) {
          this.groups[idx] = response.data;
        }
        return response.data;
      } catch (err) {
        console.error('取得群組詳情失敗:', err);
        throw err;
      }
    },

    async updateGroupInfo(groupId, name, announcement, icon) {
      const response = await groupApi.updateGroup(groupId, { name, announcement, icon });
      this.activeGroup = response.data;
      const idx = this.groups.findIndex(g => g.id === groupId);
      if (idx !== -1) {
        this.groups[idx] = response.data;
      }
      return response.data;
    },

    async addMembers(groupId, userIds) {
      await groupApi.addMembers(groupId, userIds);
      await this.fetchGroupDetails(groupId);
    },

    async leaveGroup(groupId, userId) {
      await groupApi.removeMember(groupId, userId);
      this.groups = this.groups.filter(g => g.id !== groupId);
      if (this.activeGroup && this.activeGroup.id === groupId) {
        this.activeGroup = null;
      }
    }
  }
});

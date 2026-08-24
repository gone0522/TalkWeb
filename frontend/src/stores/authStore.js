import { defineStore } from 'pinia';
import { authApi, userApi } from '../services/api';
import wsService from '../services/websocket';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('talkweb_token') || null,
    user: JSON.parse(localStorage.getItem('talkweb_user') || 'null'),
    mustChangePassword: JSON.parse(localStorage.getItem('talkweb_must_change_pwd') || 'false'),
    loading: false,
    error: null
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdmin: (state) => Boolean(state.user?.isAdmin || state.user?.admin)
  },

  actions: {
    async login(username, password) {
      this.loading = true;
      this.error = null;
      try {
        const response = await authApi.login({ username, password });
        const data = response.data;

        this.token = data.token;
        this.user = {
          id: data.id,
          username: data.username,
          nickname: data.nickname,
          hasCustomAvatar: data.hasCustomAvatar,
          avatarDefaultIcon: data.avatarDefaultIcon,
          isAdmin: data.admin,
          mustChangePassword: data.mustChangePassword
        };
        this.mustChangePassword = data.mustChangePassword;

        localStorage.setItem('talkweb_token', this.token);
        localStorage.setItem('talkweb_user', JSON.stringify(this.user));
        localStorage.setItem('talkweb_must_change_pwd', JSON.stringify(this.mustChangePassword));

        // Connect WebSocket
        wsService.connect(this.token);

        return data;
      } catch (err) {
        this.error = err.message || '登入失敗';
        throw err;
      } finally {
        this.loading = false;
      }
    },

    async register(username, nickname, password) {
      this.loading = true;
      this.error = null;
      try {
        const response = await authApi.register({ username, nickname, password });
        const data = response.data;

        this.token = data.token;
        this.user = {
          id: data.id,
          username: data.username,
          nickname: data.nickname,
          hasCustomAvatar: data.hasCustomAvatar,
          avatarDefaultIcon: data.avatarDefaultIcon,
          isAdmin: data.admin,
          mustChangePassword: false
        };
        this.mustChangePassword = false;

        localStorage.setItem('talkweb_token', this.token);
        localStorage.setItem('talkweb_user', JSON.stringify(this.user));
        localStorage.setItem('talkweb_must_change_pwd', 'false');

        // Connect WebSocket
        wsService.connect(this.token);

        return data;
      } catch (err) {
        this.error = err.message || '註冊失敗';
        throw err;
      } finally {
        this.loading = false;
      }
    },

    async changePassword(newPassword) {
      this.loading = true;
      this.error = null;
      try {
        await authApi.changePassword({ newPassword });
        this.mustChangePassword = false;
        if (this.user) {
          this.user.mustChangePassword = false;
          localStorage.setItem('talkweb_user', JSON.stringify(this.user));
        }
        localStorage.setItem('talkweb_must_change_pwd', 'false');
      } catch (err) {
        this.error = err.message || '修改密碼失敗';
        throw err;
      } finally {
        this.loading = false;
      }
    },

    async fetchCurrentUser() {
      if (!this.token) return;
      try {
        const response = await userApi.getMe();
        this.user = response.data;
        this.mustChangePassword = response.data.mustChangePassword;
        localStorage.setItem('talkweb_user', JSON.stringify(this.user));
        localStorage.setItem('talkweb_must_change_pwd', JSON.stringify(this.mustChangePassword));
      } catch (err) {
        console.error('更新使用者資料失敗:', err);
      }
    },

    logout() {
      this.token = null;
      this.user = null;
      this.mustChangePassword = false;
      localStorage.removeItem('talkweb_token');
      localStorage.removeItem('talkweb_user');
      localStorage.removeItem('talkweb_must_change_pwd');
      wsService.disconnect();
    }
  }
});

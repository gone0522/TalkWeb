import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request Interceptor: Attach JWT Token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('talkweb_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

// Response Interceptor: Handle 401 Unauthorized
api.interceptors.response.use((response) => {
  return response.data; // Returns ApiResponse object directly: { code, message, data }
}, (error) => {
  if (error.response) {
    if (error.response.status === 401) {
      localStorage.removeItem('talkweb_token');
      localStorage.removeItem('talkweb_user');
      if (window.location.pathname !== '/login') {
        window.location.reload();
      }
    }
    const message = error.response.data?.message || '伺服器請求發生錯誤';
    return Promise.reject(new Error(message));
  }
  return Promise.reject(new Error('網路連線失敗，請檢查伺服器連線'));
});

export const authApi = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (data) => api.post('/auth/register', data),
  changePassword: (data) => api.put('/auth/password', data)
};

export const userApi = {
  getMe: () => api.get('/users/me'),
  updateMe: (data) => api.put('/users/me', data),
  uploadAvatar: (formData) => api.post('/users/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  getAvatarUrl: (userId) => {
    const token = localStorage.getItem('talkweb_token');
    return `/api/users/${userId}/avatar${token ? `?token=${token}` : ''}`;
  },
  getContacts: (search) => api.get('/contacts', { params: { search } }),
  checkContact: (username) => api.get('/contacts/check', { params: { username } }),
  addFriend: (username) => api.post('/contacts/add', { username })
};

export const groupApi = {
  createGroup: (data) => api.post('/groups', data),
  getUserGroups: () => api.get('/groups'),
  getGroupDetails: (id) => api.get(`/groups/${id}`),
  updateGroup: (id, data) => api.put(`/groups/${id}`, data),
  addMembers: (id, userIds) => api.post(`/groups/${id}/members`, { userIds }),
  removeMember: (id, userId) => api.delete(`/groups/${id}/members/${userId}`)
};

export const messageApi = {
  sendDirect: (data) => api.post('/messages/direct', data),
  sendGroup: (data) => api.post('/messages/group', data),
  getDirectHistory: (userId, beforeId, limit = 50) => 
    api.get(`/messages/direct/${userId}`, { params: { before: beforeId, limit } }),
  getGroupHistory: (groupId, beforeId, limit = 50) => 
    api.get(`/messages/group/${groupId}`, { params: { before: beforeId, limit } }),
  markAsRead: (data) => api.post('/messages/read', data)
};

export const adminApi = {
  createUser: (data) => api.post('/admin/users', data),
  getAllUsers: () => api.get('/admin/users'),
  updateUserStatus: (id, status) => api.put(`/admin/users/${id}/status`, { status }),
  resetPassword: (id) => api.post(`/admin/users/${id}/reset-password`)
};

export const utilApi = {
  getLinkPreview: (url) => api.get('/utils/link-preview', { params: { url } })
};

export default api;

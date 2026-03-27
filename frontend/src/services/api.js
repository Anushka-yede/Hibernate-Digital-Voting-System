import axios from 'axios';
import { clearSession, getRefreshToken, getSession, setSession } from './session';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json'
  }
});

api.interceptors.request.use((config) => {
  const token = getSession()?.token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error?.config;
    if (error?.response?.status === 401 && !originalRequest?._retry) {
      originalRequest._retry = true;
      const refreshToken = getRefreshToken();
      if (!refreshToken) {
        clearSession();
        return Promise.reject(error);
      }

      try {
        const { data } = await axios.post(`${API_BASE}/api/auth/refresh`, { refreshToken });
        setSession(data);
        originalRequest.headers.Authorization = `Bearer ${data.token}`;
        return api(originalRequest);
      } catch {
        clearSession();
        return Promise.reject(error);
      }
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  register: (payload) => api.post('/api/auth/register', payload),
  login: (payload) => api.post('/api/auth/login', payload),
  logout: (refreshToken) => api.post('/api/auth/logout', { refreshToken })
};

export const electionApi = {
  listActive: (params) => api.get('/api/elections', { params }),
  listUpcoming: (params) => api.get('/api/elections', { params: { ...params, scope: 'upcoming' } }),
  listAdmin: (params) => api.get('/api/admin/elections', { params }),
  create: (payload) => api.post('/api/admin/elections', payload),
  update: (electionId, payload) => api.put(`/api/admin/elections/${electionId}`, payload),
  remove: (electionId) => api.delete(`/api/admin/elections/${electionId}`),
  addCandidate: (electionId, payload) => api.post(`/api/admin/elections/${electionId}/candidates`, payload),
  addCandidatesBulk: (electionId, payload) => api.post(`/api/admin/elections/${electionId}/candidates/bulk`, payload),
  updateCandidate: (candidateId, payload) => api.put(`/api/admin/candidates/${candidateId}`, payload),
  removeCandidate: (candidateId) => api.delete(`/api/admin/candidates/${candidateId}`),
  searchCandidates: (params) => api.get('/api/candidates/search', { params }),
  candidateRegions: () => api.get('/api/candidates/regions'),
  electionRegions: () => api.get('/api/elections/regions'),
  results: (electionId) => api.get(`/api/admin/elections/${electionId}/results`),
  audit: (electionId) => api.get(`/api/admin/elections/${electionId}/audit`),
  analytics: (electionId) => api.get(`/api/admin/elections/${electionId}/analytics`),
  auditLogs: (params) => api.get('/api/admin/audit-logs', { params }),
  vote: (payload) => api.post('/api/votes', payload),
  voteStatus: (electionId) => api.get('/api/votes/status', { params: { electionId } }),
  anomalySummary: () => api.get('/api/monitoring/anomaly-summary'),
  alerts: (params) => api.get('/api/monitoring/alerts', { params })
};

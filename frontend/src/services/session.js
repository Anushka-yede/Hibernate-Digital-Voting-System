function decodeJwtPayload(token) {
  try {
    const payloadPart = token.split('.')[1];
    if (!payloadPart) return null;
    const base64 = payloadPart.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => `%${(`00${c.charCodeAt(0).toString(16)}`).slice(-2)}`)
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

export function getSession() {
  try {
    const token = localStorage.getItem('token');
    const refreshToken = localStorage.getItem('refreshToken');
    const rawUser = localStorage.getItem('user');
    if (!token || !refreshToken || !rawUser) return null;

    const payload = decodeJwtPayload(token);
    if (!payload?.exp) return null;
    if (Date.now() >= payload.exp * 1000) {
      clearSession();
      return null;
    }

    const user = JSON.parse(rawUser);
    if (!user?.role || !['ADMIN', 'USER'].includes(user.role)) {
      clearSession();
      return null;
    }

    return { token, refreshToken, user };
  } catch {
    clearSession();
    return null;
  }
}

export function setSession(authResponse) {
  localStorage.setItem('token', authResponse.token);
  localStorage.setItem('refreshToken', authResponse.refreshToken);
  localStorage.setItem('user', JSON.stringify({
    userId: authResponse.userId,
    username: authResponse.username,
    role: authResponse.role
  }));
}

export function getRefreshToken() {
  return localStorage.getItem('refreshToken');
}

export function clearSession() {
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
}

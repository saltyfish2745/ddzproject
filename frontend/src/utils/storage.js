
const TOKEN_KEY = 'token';

// Get token from local storage
export const getToken = () => {
  return localStorage.getItem(TOKEN_KEY) || null;
}
// Set token to local storage
export const setToken = (token) => {
  localStorage.setItem(TOKEN_KEY, token);
}
// Remove token from local storage
export const removeToken = () => {
  localStorage.removeItem('token');
}

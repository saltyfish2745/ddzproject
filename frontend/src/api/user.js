import request from '@/utils/request.js';

export const postLogin = (account, password) => {
  return request.post('/login', { account, password });
}

export const postRegister = (username, account, password, email, emailcode) => {
  if (email === null || email === "") {
    return request.post('/register', { username, account, password });
  } else {
    return request.post('/register', { username, account, password, email }, { params: { emailcode } });
  }
}

export const getIsAccountExist = (account) => {
  return request.get('/isAccountExist', { params: { account } });
}

export const getApplyForEmailcode = (email) => {
  return request.get('/applyForEmailcode', { params: { email } });
}
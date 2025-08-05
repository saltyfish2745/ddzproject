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

export const PostRetrieve = (account, password, email, emailcode) => {
  return request.post('/retrieve', { account, password, email }, { params: { emailcode } });
}

export const getIsAccountExist = (account) => {
  return request.get('/isAccountExist', { params: { account } });
}

export const getApplyForEmailcode = (email) => {
  return request.get('/applyForEmailcode', { params: { email } });
}

export const getViewUserInfo = (token) => {
  return request.get('/viewUserInfo', {
    headers: {
      'token': token
    }
  });
}

export const getPageBeanHistory = (token, page, pageSize) => {
  return request.get('/pageBeanHistory', {
    headers: {
      'token': token
    },
    params: {
      page,
      pageSize
    }
  });
}

export const getClockIn = (token) => { 
  return request.get('/clockIn', {
    headers: {
      'token': token
    }
  });
}
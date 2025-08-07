import request from '@/utils/request.js';

// 登入
export const postLogin = (account, password) => {
  return request.post('/login', { account, password });
}

// 註冊
export const postRegister = (username, account, password, email, emailcode) => {
  if (email === null || email === "") {
    return request.post('/register', { username, account, password });
  } else {
    return request.post('/register', { username, account, password, email }, { params: { emailcode } });
  }
}

// 密码找回
export const PostRetrieve = (account, password, email, emailcode) => {
  return request.post('/retrieve', { account, password, email }, { params: { emailcode } });
}

// 检查账号是否存在
export const getIsAccountExist = (account) => {
  return request.get('/isAccountExist', { params: { account } });
}

// 申请邮箱验证码
export const getApplyForEmailcode = (email) => {
  return request.get('/applyForEmailcode', { params: { email } });
}

// 获取用户信息
export const getViewUserInfo = (token) => {
  return request.get('/viewUserInfo', {
    headers: {
      'token': token
    }
  });
}

// 分页查看用户豆币历史
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

// 签到
export const getClockIn = (token) => { 
  return request.get('/clockIn', {
    headers: {
      'token': token
    }
  });
}

// 更新用户名
export const putUpdateUsername = (token, username) => { 
  return request.put('/updateUsername', null, {
    headers: {
      'token': token
    },
    params: {
      username
    }
  });
}

// 更新密码
export const putUpdatePassword = (token, oldPassword, newPassword) => { 
  return request.put('/updatePassword', null, {
    headers: {
      'token': token
    },
    params: {
      oldPassword,
      newPassword
    }
  });
}

// 从绑定的邮箱申请邮箱验证码
export const getApplyForEmailcodeFrombindingEmail = (token)=> {
  return request.get('/applyForEmailcodeFrombindingEmail', {
    headers: {
      'token': token
    }
  });
}

// 更新邮箱
export const putUpdateEmail = (token, oldCode, newCode, newEmail) => {
  const params = {
    newCode,
    newEmail
  };
  
  if (oldCode !== null && oldCode !== "") {
    params.oldCode = oldCode;
  }
  
  return request.put('/updateEmail', null, {
    headers: {
      'token': token
    },
    params: params
  });
}


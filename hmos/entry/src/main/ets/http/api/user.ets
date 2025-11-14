import { AxiosResponse } from "@ohos/axios";
import { request } from "../request";

interface responseData<T>{
  code:number,
  msg:string,
  data: T
}


interface postLoginData {
  token: string;
}
export function postLogin(account:string,password:string){
  return request.post<postLoginData,responseData<postLoginData>>(//指定返回类型postLoginResponse为后续调用方便
    '/login',
    {
      account,
      password
    })
}

interface getViewUserInfoData {
  username: string;
  beanCount: number;
}
export function getViewUserInfo(token:string){
  return request.get<getViewUserInfoData,responseData<getViewUserInfoData>>(
    '/viewUserInfo',
    {
      headers:{
        token
      }
    }
  )
}

export function putUpdatePassword(token:string,oldPassword:string,newPassword:string){
  return request.put<null,responseData<null>>(
    '/updatePassword',
    {},
    {
      params:{
        oldPassword,
        newPassword
      },
      headers:{
        token
      }
    }
  )
}

export function getApplyForEmailcodeFrombindingEmail(token:string){
  return request.get<number,responseData<number>>(
    '/applyForEmailcodeFrombindingEmail',
    {
      headers:{
        token
      }
    }
  )
}

export function getApplyForEmailcode(email:string){
  return request.get<number,responseData<number>>(
    '/applyForEmailcode',
    {
      params:{
        email
      }
    }
  )
}

export function putUpdateEmail(token:string,newCode:string,newEmail:string,oldCode?:string){
  return request.put<null,responseData<null>>(
    '/updateEmail',{},
    {
      params:{
        oldCode,
        newCode,
        newEmail
      },headers:{
        token
    }
    }
  )
}

export function getClockIn(token:string){
  return request.get<null,responseData<null>>('/clockIn',
    {
      headers:{
        token
      }
    })
}

export interface getPageBeanHistoryList{
  changeType:string//更改类型
  changeAmount:number//更改数量
  currentBean:number//更改后的数量
  createTime:string//更改时间 例：'2025-10-11 21:45:27'
}

interface getPageBeanHistoryData{
  total:number//全部数据总量
  list:getPageBeanHistoryList[]
}
export function getPageBeanHistory(token:string,page:number,pageSize:number){
  return request.get<getPageBeanHistoryData,responseData<getPageBeanHistoryData>>('/pageBeanHistory',
    {
      params:{
        page,
        pageSize
      },headers:{
        token
    }
    })
}

export function postRegister(username:string,account:string,password:string,email?:string,emailcode?:string){
  return request.post<postLoginData,responseData<postLoginData>>('/register',
    {
      username,
      account,
      password,
      email:email?email:undefined
    },{
    params:{
      emailcode:email?emailcode:undefined
    }
    })
}

export function postRetrieve(account:string,password:string,email:string,emailcode:string){
  return request.post<null,responseData<null>>('/retrieve',
    {
      account,
      email,
      password
    },{
    params:{
      emailcode
    }
    })
}
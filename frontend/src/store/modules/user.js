import {getToken, setToken, removeToken} from '@/utils/storage';

export default {
    namespaced: true,
    state() {
        return {
            token: getToken(), // 用户令牌
            matchingStatus: {
                isMatching: false,
                count: 0
            }
        }
    },
    mutations: {
        // 定义状态变更方法
        setToken(state, token) {
            state.token = token;
            setToken(token); // 存储到本地
        },
        // 设置匹配状态
        setMatchingStatus(state, payload) {
            state.matchingStatus = {
                isMatching: payload.isMatching,
                count: payload.count
            };
        }
    },
    actions: {
        // 定义异步操作方法
    },
    getters: {
        // 定义计算属性
    }
}
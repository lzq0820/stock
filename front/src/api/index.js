import axios from 'axios'

const request = axios.create({
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json;charset=UTF-8'
    }
})

// 请求拦截器
request.interceptors.request.use(
    config => config,
    error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
    response => {
        const res = response.data
        if (res.code !== 200) {
            ElMessage.error(res.message || '请求失败')
            return Promise.reject(res)
        }
        return res
    },
    error => {
        ElMessage.error(error.message || '网络异常')
        return Promise.reject(error)
    }
)

export default request
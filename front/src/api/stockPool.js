// 股票池接口
import request from './index'

// 连板晋级接口
export const getLbjjData = (params) => {
    return request({
        url: '/baseInfo/baseInfo/stockPool/lbjj',
        method: 'GET',
        params
    })
}

// 股票池通用接口（涨停/跌停/强势股/炸板）
export const getStockPoolData = (params) => {
    return request({
        url: '/baseInfo/baseInfo/stockPool/query',
        method: 'GET',
        params
    })
}
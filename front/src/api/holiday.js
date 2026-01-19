// 节假日相关接口
import request from './index'

// 查询节假日
export const getHolidayList = (year) => {
    return request({
        url: '/baseInfo/api/holiday/sync/query',
        method: 'GET',
        params: { year }
    })
}
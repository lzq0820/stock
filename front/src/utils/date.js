// 日期处理工具
import dayjs from 'dayjs'
import { getHolidayList } from '@/api/holiday'

// 获取禁用日期列表（未来日期 + 节假日）
export const getDisabledDates = async () => {
    const currentYear = dayjs().year()
    const holidayRes = await getHolidayList(currentYear)
    const holidayDates = holidayRes.data.map(item => dayjs(item.holidayDate).format('YYYY-MM-DD'))

    // 未来日期
    const futureDates = []
    const today = dayjs()
    for (let i = 1; i < 365; i++) {
        futureDates.push(today.add(i, 'day').format('YYYY-MM-DD'))
    }

    return [...holidayDates, ...futureDates]
}

// 格式化日期
export const formatDate = (date) => {
    return dayjs(date).format('YYYY-MM-DD')
}

// 获取节假日名称
export const getHolidayName = (date, holidayList) => {
    const item = holidayList.find(item => item.holidayDate === date)
    return item ? item.holidayName : ''
}
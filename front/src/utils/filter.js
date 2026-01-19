// 筛选工具函数
// 筛选股票（reason多选 + 股票名称模糊 + 详情模糊）
export const filterStocks = (stockList, filterParams) => {
    const { reasonList = [], stockName = '', stockReason = '' } = filterParams
    if (!stockList.length) return stockList

    return stockList.map(grade => {
        const filteredChildren = grade.children.filter(stock => {
            // 1. 涨停原因筛选（多选，匹配一个即可）
            const reasonMatch = reasonList.length
                ? stock.reasonList.some(reason => reasonList.includes(reason))
                : true

            // 2. 股票名称模糊匹配
            const nameMatch = stockName
                ? stock.stockName.includes(stockName)
                : true

            // 3. 详情模糊匹配
            const reasonDetailMatch = stockReason
                ? stock.stockReason.includes(stockReason)
                : true

            // AND关系：全部匹配才命中
            const isMatch = reasonMatch && nameMatch && reasonDetailMatch
            stock.isMatch = isMatch // 标记是否命中，用于高亮
            return true // 不隐藏，只标记
        })

        return { ...grade, children: filteredChildren }
    })
}

// 提取所有reasonList去重
export const extractAllReasons = (stockList) => {
    const reasons = new Set()
    stockList.forEach(grade => {
        grade.children.forEach(stock => {
            stock.reasonList.forEach(reason => reasons.add(reason))
        })
    })
    return Array.from(reasons)
}
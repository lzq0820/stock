// 菜单配置
export const MENU_CONFIG = {
    DEFAULT_ACTIVE: 'gupiao-yanzhen',  // 默认激活的菜单项
    MENU_ITEMS: {
        'gupiao-yanzhen': {
            component: 'StockGradientView',
            title: '涨停雁阵图'
        },
        'panmian-zixuan': {
            component: 'MyStocksView',
            title: '自选'
        }
    }
};

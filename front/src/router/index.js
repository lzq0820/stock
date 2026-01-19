// 路由配置
import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/components/Layout/MainLayout.vue'
import OptionalStock from '@/components/StockPool/OptionalStock.vue'
import StockPool from '@/components/StockPool/StockPool.vue'
import DragonTigerList from '@/components/StockPool/DragonTigerList.vue'

const routes = [
    {
        path: '/',
        component: MainLayout,
        children: [
            { path: '', redirect: '/optional' },
            { path: 'optional', name: 'Optional', component: OptionalStock, meta: { title: '自选' } },
            { path: 'stockPool', name: 'StockPool', component: StockPool, meta: { title: '盘面-股票池' } },
            { path: 'dragonTiger', name: 'DragonTiger', component: DragonTigerList, meta: { title: '盘面-龙虎榜' } }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
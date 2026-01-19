package com.liuzhq.stock.BaseInfo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liuzhq.stock.BaseInfo.dto.StockPoolDto;
import com.liuzhq.stock.BaseInfo.entity.StockPool;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.List;

public interface StockPoolService extends IService<StockPool> {

    boolean syncStockPoolData(String poolKey) throws UnsupportedEncodingException;

    boolean syncStockPoolData(String poolKey, LocalDate tradeDate) throws UnsupportedEncodingException;

    List<StockPoolDto> queryByDateAndPoolType(LocalDate tradeDate, String poolType, Integer notShowSt) throws UnsupportedEncodingException;

    boolean syncAllStockPoolData();

    List<StockPoolDto> lbjjStockPool(LocalDate tradeDate, Integer notShowSt);
}
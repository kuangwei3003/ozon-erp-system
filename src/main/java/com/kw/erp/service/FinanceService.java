package com.kw.erp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.dto.finance.AnalyticsReq;
import com.kw.erp.client.dto.finance.TransactionReq;
import com.kw.erp.client.dto.finance.TransactionResp;

/**
 * 财务与分析数据
 */
public interface FinanceService {

    /** 交易流水（直接分页查询 Ozon） */
    TransactionResp transactions(TransactionReq req);

    /** 分析指标数据 */
    JsonNode analytics(AnalyticsReq req);
}

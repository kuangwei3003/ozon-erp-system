package com.kw.erp.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.dto.product.ImportProductReq;
import com.kw.erp.entity.Product;

/**
 * 商品管理：上传/更新商品、同步商品列表
 */
public interface ProductService {

    /**
     * 从 Ozon 拉取商品并写入本地表
     *
     * @return 同步的商品数量
     */
    int sync();

    /**
     * 本地商品分页查询
     *
     * @param keyword 按名称或 offer_id 模糊搜索
     */
    IPage<Product> page(long current, long size, String keyword, String status);

    Product detail(Long id);

    /** 上传/更新商品（/v3/product/import），返回原始响应（含 task_id） */
    JsonNode upload(ImportProductReq req);

    /** 查询上传任务状态 */
    JsonNode uploadStatus(Long taskId);
}

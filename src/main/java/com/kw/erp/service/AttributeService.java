package com.kw.erp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.dto.attr.AttributeReq;
import com.kw.erp.client.dto.attr.AttributeValuesReq;
import com.kw.erp.entity.ProductAttribute;

import java.util.List;

/**
 * 属性与特征数据：类目树、类目属性模板、字典值、商品已填属性
 */
public interface AttributeService {

    /** 类目树原始报文 */
    JsonNode categoryTree();

    /** 类目属性模板原始报文（用于生成上传表单与特征展示） */
    JsonNode categoryAttributes(AttributeReq req);

    /** 字典属性值原始报文 */
    JsonNode attributeValues(AttributeValuesReq req);

    /** 拉取商品已填属性并落地本地表 */
    int syncProductAttributes(List<Long> skus);

    /** 某商品的属性列表（按 product_id 或 sku 匹配） */
    List<ProductAttribute> attributesOfProduct(Long productIdOrSku);
}

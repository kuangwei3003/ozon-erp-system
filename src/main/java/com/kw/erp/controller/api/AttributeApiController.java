package com.kw.erp.controller.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.dto.attr.AttributeReq;
import com.kw.erp.client.dto.attr.AttributeValuesReq;
import com.kw.erp.common.Result;
import com.kw.erp.entity.ProductAttribute;
import com.kw.erp.service.AttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 属性与特征接口
 */
@RestController
@RequestMapping("/api/attributes")
@RequiredArgsConstructor
public class AttributeApiController {

    private final AttributeService attributeService;

    /** 类目树 */
    @GetMapping("/category-tree")
    public Result<JsonNode> categoryTree() {
        return Result.ok(attributeService.categoryTree());
    }

    /** 类目属性模板 */
    @GetMapping("/category/{categoryId}/{typeId}")
    public Result<JsonNode> categoryAttributes(@PathVariable Long categoryId,
                                               @PathVariable Long typeId,
                                               @RequestParam(defaultValue = "RU") String language) {
        AttributeReq req = new AttributeReq();
        req.setDescriptionCategoryId(categoryId);
        req.setTypeId(typeId);
        req.setLanguage(language);
        return Result.ok(attributeService.categoryAttributes(req));
    }

    /** 字典属性值（查询参数与 Ozon 报文一致，采用 snake_case） */
    @GetMapping("/values")
    public Result<JsonNode> attributeValues(@RequestParam("attr_id") Long attrId,
                                            @RequestParam("category_id") Long categoryId,
                                            @RequestParam("type_id") Long typeId,
                                            @RequestParam(value = "last_id", defaultValue = "0") Integer lastId) {
        AttributeValuesReq req = new AttributeValuesReq();
        req.setAttrId(attrId);
        req.setDescriptionCategoryId(categoryId);
        req.setTypeId(typeId);
        req.setLastId(lastId);
        return Result.ok(attributeService.attributeValues(req));
    }

    /** 同步商品已填属性与特征 */
    @PostMapping("/sync")
    public Result<Integer> sync(@RequestBody(required = false) List<Long> skus) {
        return Result.ok(attributeService.syncProductAttributes(skus));
    }

    /** 某商品的属性列表 */
    @GetMapping("/product/{productId}")
    public Result<List<ProductAttribute>> ofProduct(@PathVariable Long productId) {
        return Result.ok(attributeService.attributesOfProduct(productId));
    }
}

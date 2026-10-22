package com.kw.erp.client.dto.attr;

import lombok.Data;

/**
 * /v1/description-category/attribute/values 请求（字典属性值）
 */
@Data
public class AttributeValuesReq {

    private Long attrId;

    private Long descriptionCategoryId;

    private Long typeId;

    private String language = "RU";

    private Integer limit = 100;

    private Integer lastId = 0;
}

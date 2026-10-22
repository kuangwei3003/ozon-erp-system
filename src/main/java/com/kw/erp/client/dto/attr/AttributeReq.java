package com.kw.erp.client.dto.attr;

import lombok.Data;

/**
 * /v1/description-category/attribute 请求
 */
@Data
public class AttributeReq {

    /** 类目 ID（description_category_id） */
    private Long descriptionCategoryId;

    /** 类型 ID */
    private Long typeId;

    /** 语言：DEFAULT/RU/EN/CN/TR */
    private String language = "RU";
}

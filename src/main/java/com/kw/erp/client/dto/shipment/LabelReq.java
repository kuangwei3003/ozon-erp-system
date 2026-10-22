package com.kw.erp.client.dto.shipment;

import lombok.Data;

import java.util.List;

/**
 * /v2/posting/fbs/package-labeling 请求（打包发货/获取标签）
 */
@Data
public class LabelReq {

    private List<String> postingNumber;

    private Boolean mustPrintQr;
}

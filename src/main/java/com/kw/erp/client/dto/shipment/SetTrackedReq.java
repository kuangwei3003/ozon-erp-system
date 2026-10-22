package com.kw.erp.client.dto.shipment;

import lombok.Data;

import java.util.List;

/**
 * /v5/posting/fbs/set-tracked 请求（录入运单号）
 */
@Data
public class SetTrackedReq {

    private List<Tracking> postings;

    @Data
    public static class Tracking {
        private String postingNumber;
        private String trackingNumber;
    }
}

package com.kw.erp.client.dto.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * /v3/posting/fbs/list 响应
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FbsListResp {

    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private List<PostingId> postings;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PostingId {
        private Long orderId;
        private String postingNumber;
    }
}

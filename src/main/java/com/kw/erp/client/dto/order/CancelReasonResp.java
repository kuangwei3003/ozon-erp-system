package com.kw.erp.client.dto.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * /v3/posting/fbs/cancel-reason 响应
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelReasonResp {

    private List<Reason> result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Reason {
        private Integer id;
        private String title;
        private String description;
        private Integer type;
        private Integer pipeline;
        private Boolean isDelayDeliveryDate;
        private Boolean isEditableProduct;
        private Boolean needCommentForSeller;
        private Boolean needPhoto;
    }
}

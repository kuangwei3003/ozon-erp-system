package com.kw.erp.client.dto.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * /v3/posting/fbs/get 响应
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FbsPosting {

    private FbsPostingResult result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FbsPostingResult {
        private String postingNumber;
        private Long orderId;
        private String scheme;
        private String status;
        private String substatus;
        private String trackingNumber;
        private String shipmentDate;
        private String createdAt;
        private String shipmentPlanDate;
        private String deadlineOnAggregator;
        private String inProcessAt;
        private String updatedOn;
        private String anyAtDelivery;
        private Boolean isMultiPackage;
        private Boolean isAutoPayment;
        private CancelInfo cancel;
        private DeliveryMethod deliveryMethod;
        private List<Product> products;
        private FinancialData financialData;
        private Customer customer;
        private List<String> barcodes;
        private Requirements requirements;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CancelInfo {
        private boolean cancelled;
        private Integer reasonId;
        private String reason;
        private Long claimId;
        private String dt;
        private String cancelerId;
        private String comment;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeliveryMethod {
        private Long warehouseId;
        private Long stationId;
        private String name;
        private String clause;
        private String template;
        private String vendorName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {
        private Long price;
        private Long quantity;
        private String offerId;
        private Long sku;
        private String name;
        private Boolean planUnit;
        private Long density;
        private List<String> productLicenses;
        private Boolean isBlister;
        private Boolean isBulk;
        private String barcode;
        private Size size;
        private Long weight;
        private Boolean isNds;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Size {
        private Long length;
        private Long width;
        private Long height;
        private String weightUnit;
        private String dimensionUnit;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FinancialData {
        private CustomerRegion customerRegion;
        private Long partnerRate;
        private BigDecimal productsRate;
        private List<ProductAmount> productAmounts;
        private List<TotalAmount> total;
        private List<Commission> commission;
        private List<Tariff> tariff;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CustomerRegion {
            private Long id;
            private String name;
            private String fullName;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ProductAmount {
            private Long price;
            private Long quantity;
            private String offerId;
            private Long sku;
            private String name;
            private Long payoutPrice;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TotalAmount {
            private String type;
            private BigDecimal amount;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Commission {
            private String type;
            private BigDecimal amount;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Tariff {
            private String type;
            private BigDecimal amount;
            private String value;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Customer {
        private String name;
        private String phone;
        private String email;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Requirements {
        private Long createdAt;
        private String hidden;
        private String legalNameToCustomer;
    }
}

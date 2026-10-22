package com.kw.erp.client.dto.shipment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * /v1/warehouse/list 响应（卖家仓库列表）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WarehouseListResp {

    private List<Warehouse> warehouses;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Warehouse {
        private Long warehouseId;
        private String name;
        private Boolean isVwh;
        private Boolean isPlto;
        private Boolean status;
        private String warehouseType;
        private Address address;
        private String firstMileType;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Address {
            private String address;
            private String city;
            private String country;
            private String zipCode;
        }
    }
}

package com.kw.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.OzonApiClient;
import com.kw.erp.client.dto.shipment.LabelReq;
import com.kw.erp.client.dto.shipment.SetTrackedReq;
import com.kw.erp.entity.Posting;
import com.kw.erp.exception.DomainException;
import com.kw.erp.mapper.PostingMapper;
import com.kw.erp.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 运单处理实现：仓库列表、发货标签、运单号录入
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private final OzonApiClient client;
    private final PostingMapper postingMapper;

    @Override
    public JsonNode warehouses() {
        return client.warehouseList();
    }

    @Override
    public JsonNode setTracking(SetTrackedReq req) {
        if (req == null || CollectionUtils.isEmpty(req.getPostings())) {
            throw new DomainException("请至少填写一个运单号");
        }
        req.getPostings().forEach(p -> {
            if (!StringUtils.hasText(p.getPostingNumber()) || !StringUtils.hasText(p.getTrackingNumber())) {
                throw new DomainException("订单号与运单号都不能为空");
            }
        });
        JsonNode resp = client.setTracked(req);
        req.getPostings().forEach(p -> updateLocalTracking(p.getPostingNumber(), p.getTrackingNumber()));
        log.info("已录入 {} 个运单号", req.getPostings().size());
        return resp;
    }

    private void updateLocalTracking(String postingNumber, String trackingNumber) {
        LambdaQueryWrapper<Posting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Posting::getPostingNumber, postingNumber).last("LIMIT 1");
        Posting posting = postingMapper.selectOne(wrapper);
        if (posting != null) {
            posting.setTrackingNumber(trackingNumber);
            postingMapper.updateById(posting);
        }
    }

    @Override
    public JsonNode label(LabelReq req) {
        if (req == null || CollectionUtils.isEmpty(req.getPostingNumber())) {
            throw new DomainException("请选择需要打印标签的订单");
        }
        List<String> invalid = req.getPostingNumber().stream().filter(n -> !StringUtils.hasText(n)).toList();
        if (!invalid.isEmpty()) {
            throw new DomainException("订单号不能为空");
        }
        return client.packageLabeling(req);
    }
}

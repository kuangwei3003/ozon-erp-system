package com.kw.erp.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kw.erp.client.OzonApiClient;
import com.kw.erp.client.dto.shipment.LabelReq;
import com.kw.erp.client.dto.shipment.SetTrackedReq;
import com.kw.erp.entity.Posting;
import com.kw.erp.exception.DomainException;
import com.kw.erp.mapper.PostingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 运单处理测试：运单号录入校验与本地订单同步更新
 */
@ExtendWith(MockitoExtension.class)
class ShipmentServiceImplTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private OzonApiClient client;
    @Mock
    private PostingMapper postingMapper;

    private ShipmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ShipmentServiceImpl(client, postingMapper);
    }

    private SetTrackedReq trackedReq(String postingNumber, String trackingNumber) {
        SetTrackedReq req = new SetTrackedReq();
        SetTrackedReq.Tracking tracking = new SetTrackedReq.Tracking();
        tracking.setPostingNumber(postingNumber);
        tracking.setTrackingNumber(trackingNumber);
        req.setPostings(List.of(tracking));
        return req;
    }

    @Test
    void setTrackingRejectsEmptyRequest() {
        assertThrows(DomainException.class, () -> service.setTracking(null));

        SetTrackedReq empty = new SetTrackedReq();
        empty.setPostings(List.of());
        assertThrows(DomainException.class, () -> service.setTracking(empty));

        DomainException ex = assertThrows(DomainException.class,
                () -> service.setTracking(trackedReq("01223344-0001-1", "  ")));
        assertEquals("订单号与运单号都不能为空", ex.getMessage());

        verify(client, never()).setTracked(any());
    }

    @Test
    void setTrackingUpdatesApiAndLocalPosting() throws Exception {
        Posting posting = new Posting();
        posting.setId(7L);
        posting.setPostingNumber("01223344-0001-1");
        posting.setStatus("delivering");
        when(postingMapper.selectOne(any())).thenReturn(posting);
        when(client.setTracked(any(SetTrackedReq.class))).thenReturn(MAPPER.readTree("{\"result\":true}"));

        JsonNode resp = service.setTracking(trackedReq("01223344-0001-1", "RR123456789RU"));

        assertEquals("true", resp.path("result").asText());
        verify(client).setTracked(any(SetTrackedReq.class));
        ArgumentCaptor<Posting> captor = ArgumentCaptor.forClass(Posting.class);
        verify(postingMapper).updateById(captor.capture());
        assertEquals("RR123456789RU", captor.getValue().getTrackingNumber());
    }

    /** 本地还没有该订单时只调用 API，不能因为找不到记录而报错 */
    @Test
    void setTrackingIgnoresUnknownLocalPosting() throws Exception {
        when(postingMapper.selectOne(any())).thenReturn(null);
        when(client.setTracked(any(SetTrackedReq.class))).thenReturn(MAPPER.readTree("{\"result\":true}"));

        service.setTracking(trackedReq("NOT-LOCAL-0001-1", "RR000000000RU"));

        verify(postingMapper, never()).updateById(any(Posting.class));
    }

    @Test
    void labelRejectsEmptySelection() {
        assertThrows(DomainException.class, () -> service.label(null));

        LabelReq empty = new LabelReq();
        empty.setPostingNumber(List.of());
        DomainException ex = assertThrows(DomainException.class, () -> service.label(empty));
        assertEquals("请选择需要打印标签的订单", ex.getMessage());

        LabelReq blank = new LabelReq();
        blank.setPostingNumber(List.of("01223344-0001-1", ""));
        DomainException ex2 = assertThrows(DomainException.class, () -> service.label(blank));
        assertEquals("订单号不能为空", ex2.getMessage());

        verify(client, never()).packageLabeling(any());
    }

    @Test
    void labelReturnsRawResponse() throws Exception {
        LabelReq req = new LabelReq();
        req.setPostingNumber(List.of("01223344-0001-1"));
        when(client.packageLabeling(req)).thenReturn(MAPPER.readTree("{\"result\":{\"pdf\":\"JVBERi0xLjQ=\"}}"));

        JsonNode resp = service.label(req);

        assertEquals("JVBERi0xLjQ=", resp.path("result").path("pdf").asText());
    }

    @Test
    void warehousesDelegatesToClient() throws Exception {
        when(client.warehouseList()).thenReturn(MAPPER.readTree("{\"result\":[{\"warehouse_id\":11,\"name\":\"main\"}]}"));

        JsonNode resp = service.warehouses();

        assertEquals(1, resp.path("result").size());
        assertEquals(11, resp.path("result").get(0).path("warehouse_id").asInt());
    }
}

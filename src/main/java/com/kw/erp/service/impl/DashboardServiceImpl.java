package com.kw.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kw.erp.entity.ChatRoom;
import com.kw.erp.entity.Posting;
import com.kw.erp.entity.PriceStock;
import com.kw.erp.entity.ReturnItem;
import com.kw.erp.mapper.ChatRoomMapper;
import com.kw.erp.mapper.PostingMapper;
import com.kw.erp.mapper.PriceStockMapper;
import com.kw.erp.mapper.ProductMapper;
import com.kw.erp.mapper.ReturnItemMapper;
import com.kw.erp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计实现：全部基于本地已同步数据，避免打开首页就打爆 Ozon 配额
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    /** 与订单模块保持一致的待发货状态口径 */
    private static final List<String> PENDING_STATUSES =
            List.of("awaiting_packaging", "awaiting_deliver", "awaiting_registration");

    private static final int RECENT_LIMIT = 5;

    private final ProductMapper productMapper;
    private final PriceStockMapper priceStockMapper;
    private final PostingMapper postingMapper;
    private final ReturnItemMapper returnItemMapper;
    private final ChatRoomMapper chatRoomMapper;

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productCount", productMapper.selectCount(null));
        data.put("pendingCount", postingMapper.selectCount(
                new LambdaQueryWrapper<Posting>().in(Posting::getStatus, PENDING_STATUSES)));
        data.put("returnCount", returnItemMapper.selectCount(null));
        data.put("zeroStockCount", priceStockMapper.selectCount(
                new LambdaQueryWrapper<PriceStock>().eq(PriceStock::getStock, 0)));

        LambdaQueryWrapper<ChatRoom> unreadWrapper = new LambdaQueryWrapper<>();
        unreadWrapper.gt(ChatRoom::getUnread, 0);
        data.put("unreadRooms", chatRoomMapper.selectCount(unreadWrapper));
        data.put("unreadMessages", chatRoomMapper.selectList(unreadWrapper).stream()
                .mapToLong(room -> room.getUnread() == null ? 0 : room.getUnread()).sum());

        data.put("recentPostings", recentPostings());
        data.put("recentReturns", recentReturns());
        return data;
    }

    private List<Posting> recentPostings() {
        LambdaQueryWrapper<Posting> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Posting::getStatus, PENDING_STATUSES)
                .orderByAsc(Posting::getShipmentPlanDate)
                .last("LIMIT " + RECENT_LIMIT);
        return postingMapper.selectList(wrapper);
    }

    private List<ReturnItem> recentReturns() {
        LambdaQueryWrapper<ReturnItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ReturnItem::getId).last("LIMIT " + RECENT_LIMIT);
        return returnItemMapper.selectList(wrapper);
    }
}

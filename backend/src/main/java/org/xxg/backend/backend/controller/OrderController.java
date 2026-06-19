package org.xxg.backend.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.dto.CreateOrderRequest;
import org.xxg.backend.backend.dto.UpdateOrderStatusRequest;
import org.xxg.backend.backend.entity.Order;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.UserRepository;
import org.xxg.backend.backend.service.OrderService;

import java.util.List;
import java.util.Map;

/**
 * 订单管理接口
 * <p>提供订单的创建、查询、状态更新及统计功能。</p>
 * <p>基础路径：/orders</p>
 * <p>用户接口需认证，管理接口需管理员权限。</p>
 */
@RestController
@RequestMapping("/orders")
@Tag(name = "订单管理", description = "订单创建、查询、状态更新")
public class OrderController {
    private final OrderService orderService;
    private final UserRepository userRepository;
    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    /**
     * 创建新订单
     * <p>POST /orders</p>
     * <p>权限：已认证用户</p>
     * @param request 创建订单请求，包含卡密类型、数量等信息
     * @return 新创建的订单信息
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Order>> createOrder(@Valid @RequestBody CreateOrderRequest request, Authentication auth) {
        // 使用认证用户的ID和用户名，忽略请求体中的userId/username，防止越权创建订单
        Integer userId = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BusinessException("用户不存在")).getId();
        request.setUserId(userId);
        request.setUsername(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("下单成功", orderService.createOrder(request)));
    }

    /**
     * 获取当前用户的订单列表（分页）
     * <p>GET /orders</p>
     * <p>权限：已认证用户（仅返回自己的订单）</p>
     * @param auth Spring Security认证对象，用于获取当前用户名
     * @param page 页码，默认0
     * @param size 每页条数，默认20
     * @return 当前用户的订单分页列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Order>>> getMyOrders(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Integer userId = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BusinessException("用户不存在")).getId();
        size = Math.min(size, 100); // 防止过大的分页请求导致 OOM
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrdersByUserId(userId, PageRequest.of(page, size))));
    }

    /**
     * 管理员获取所有订单（分页）
     * <p>GET /orders/admin</p>
     * <p>权限：管理员</p>
     * <p>注意：此端点仅支持分页查询，如需按状态筛选请使用 GET /orders/admin/all（支持可选的 status 参数）</p>
     * @param page 页码，默认0
     * @param size 每页条数，默认20
     * @return 分页订单列表
     */
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<Page<Order>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100); // 防止过大的分页请求导致 OOM
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders(PageRequest.of(page, size))));
    }

    /**
     * 管理员获取所有订单（支持多条件筛选，分页）
     * <p>GET /orders/admin/all</p>
     * <p>权限：管理员</p>
     * @param page 页码，默认0
     * @param size 每页条数，默认20
     * @param status 可选参数，按订单状态筛选
     * @param orderNo 可选参数，按订单号模糊搜索
     * @param username 可选参数，按用户名模糊搜索
     * @param startDate 可选参数，按创建时间范围起始（格式 yyyy-MM-dd）
     * @param endDate 可选参数，按创建时间范围结束（格式 yyyy-MM-dd）
     * @return 分页订单列表
     */
    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse<Page<Order>>> adminAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        size = Math.min(size, 100); // 防止过大的分页请求导致 OOM
        // 限制搜索参数长度，防止超长字符串导致数据库 LIKE 查询性能问题
        if (orderNo != null && orderNo.length() > 64) orderNo = orderNo.substring(0, 64);
        if (username != null && username.length() > 50) username = username.substring(0, 50);
        if (status != null && status.length() > 20) status = status.substring(0, 20);
        PageRequest pageRequest = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(
                orderService.searchOrders(status, orderNo, username, startDate, endDate, pageRequest)));
    }

    /**
     * 管理员更新订单状态
     * <p>POST /orders/admin/updateStatus</p>
     * <p>权限：管理员</p>
     * @param body 请求体，包含 orderNo（订单号）和 status（目标状态：completed/failed）
     * @return 操作结果
     */
    @PostMapping("/admin/updateStatus")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(@Valid @RequestBody UpdateOrderStatusRequest request) {
        if ("completed".equals(request.getStatus())) {
            orderService.completeOrder(request.getOrderNo(), null);
        } else {
            orderService.failOrder(request.getOrderNo());
        }
        return ResponseEntity.ok(ApiResponse.ok("订单状态已更新"));
    }

    /**
     * 根据订单号查询订单详情（需验证订单归属）
     * <p>GET /orders/{orderNo}</p>
     * <p>权限：已认证用户（仅能查看自己的订单）</p>
     * @param orderNo 订单号
     * @param auth 当前认证信息
     * @return 订单详情，不存在时返回错误信息
     */
    @GetMapping("/{orderNo}")
    public ResponseEntity<ApiResponse<Order>> getOrderByNo(@PathVariable String orderNo, Authentication auth) {
        Integer userId = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BusinessException("用户不存在")).getId();
        Order order = orderService.getOrderByNo(orderNo);
        if (order == null) {
            return ResponseEntity.ok(ApiResponse.error("订单不存在"));
        }
        // 验证订单归属：仅订单所有者可查看
        if (!order.getUserId().equals(userId)) {
            return ResponseEntity.ok(ApiResponse.error("无权查看此订单"));
        }
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    /**
     * 完成指定订单 — 已禁用。
     * <p>订单完成只能通过支付回调（/payment/notify）触发，
     * 防止用户绕过支付直接标记订单为已完成。</p>
     */
    @PutMapping("/{orderNo}/complete")
    public ResponseEntity<ApiResponse<Void>> completeOrder(@PathVariable String orderNo, Authentication auth) {
        // 安全修复：禁止用户直接完成订单，订单只能通过支付回调完成
        return ResponseEntity.status(403).body(ApiResponse.error("不允许直接完成订单，请通过正常支付流程"));
    }

    /**
     * 获取订单统计数据
     * <p>GET /orders/stats</p>
     * <p>权限：管理员</p>
     * @return 订单统计信息（如总订单数、完成数、收入等）
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderStats()));
    }
}

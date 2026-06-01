package org.xxg.backend.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.dto.ApiResponse;
import org.xxg.backend.backend.dto.CreateOrderRequest;
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
    public ResponseEntity<ApiResponse<Order>> createOrder(@RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("下单成功", orderService.createOrder(request)));
    }

    /**
     * 获取当前用户的订单列表
     * <p>GET /orders</p>
     * <p>权限：已认证用户（仅返回自己的订单）</p>
     * @param auth Spring Security认证对象，用于获取当前用户名
     * @return 当前用户的订单列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Order>>> getMyOrders(Authentication auth) {
        Integer userId = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BusinessException("用户不存在")).getId();
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrdersByUserId(userId)));
    }

    /**
     * 管理员获取所有订单（分页）
     * <p>GET /orders/admin</p>
     * <p>权限：管理员</p>
     * @param page 页码，默认0
     * @param size 每页条数，默认20
     * @return 分页订单列表
     */
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<Page<Order>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders(PageRequest.of(page, size))));
    }

    /**
     * 管理员获取所有订单（支持状态筛选，分页）
     * <p>GET /orders/admin/all</p>
     * <p>权限：管理员</p>
     * @param page 页码，默认0
     * @param size 每页条数，默认20
     * @param status 可选参数，按订单状态筛选
     * @return 分页订单列表
     */
    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse<Page<Order>>> adminAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders(PageRequest.of(page, size))));
    }

    /**
     * 管理员更新订单状态
     * <p>POST /orders/admin/updateStatus</p>
     * <p>权限：管理员</p>
     * @param body 请求体，包含 orderNo（订单号）和 status（目标状态：completed/failed）
     * @return 操作结果
     */
    @PostMapping("/admin/updateStatus")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(@RequestBody Map<String, String> body) {
        String orderNo = body.get("orderNo");
        String status = body.get("status");
        if ("completed".equals(status)) {
            orderService.completeOrder(orderNo, null);
        } else if ("failed".equals(status)) {
            orderService.failOrder(orderNo);
        }
        return ResponseEntity.ok(ApiResponse.ok("订单状态已更新"));
    }

    /**
     * 根据订单号查询订单详情
     * <p>GET /orders/{orderNo}</p>
     * <p>权限：已认证用户</p>
     * @param orderNo 订单号
     * @return 订单详情，不存在时返回错误信息
     */
    @GetMapping("/{orderNo}")
    public ResponseEntity<ApiResponse<Order>> getOrderByNo(@PathVariable String orderNo) {
        Order order = orderService.getOrderByNo(orderNo);
        return order != null ? ResponseEntity.ok(ApiResponse.ok(order))
                : ResponseEntity.ok(ApiResponse.error("订单不存在"));
    }

    /**
     * 完成指定订单
     * <p>PUT /orders/{orderNo}/complete</p>
     * <p>权限：已认证用户</p>
     * @param orderNo 订单号
     * @return 操作结果
     */
    @PutMapping("/{orderNo}/complete")
    public ResponseEntity<ApiResponse<Void>> completeOrder(@PathVariable String orderNo) {
        orderService.completeOrder(orderNo, null);
        return ResponseEntity.ok(ApiResponse.ok("订单已完成"));
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

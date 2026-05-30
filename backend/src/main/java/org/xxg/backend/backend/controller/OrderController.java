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

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;
    private final UserRepository userRepository;
    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Order>> createOrder(@RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("下单成功", orderService.createOrder(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Order>>> getMyOrders(Authentication auth) {
        Integer userId = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BusinessException("用户不存在")).getId();
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrdersByUserId(userId)));
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<Page<Order>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders(PageRequest.of(page, size))));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse<Page<Order>>> adminAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders(PageRequest.of(page, size))));
    }

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

    @GetMapping("/{orderNo}")
    public ResponseEntity<ApiResponse<Order>> getOrderByNo(@PathVariable String orderNo) {
        Order order = orderService.getOrderByNo(orderNo);
        return order != null ? ResponseEntity.ok(ApiResponse.ok(order))
                : ResponseEntity.ok(ApiResponse.error("订单不存在"));
    }

    @PutMapping("/{orderNo}/complete")
    public ResponseEntity<ApiResponse<Void>> completeOrder(@PathVariable String orderNo) {
        orderService.completeOrder(orderNo, null);
        return ResponseEntity.ok(ApiResponse.ok("订单已完成"));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderStats()));
    }
}

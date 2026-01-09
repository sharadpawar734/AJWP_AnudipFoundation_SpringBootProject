package com.Anudip.FinalProject.service;

import com.Anudip.FinalProject.model.Order;
import com.Anudip.FinalProject.model.OrderItem;
import com.Anudip.FinalProject.model.User;
import com.Anudip.FinalProject.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private UserService userService;

    public List<Order> getAllOrders() {
        // Use the optimized query that eagerly loads order items with products
        return orderRepository.findAllWithItems();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByUserId(Long userId) {
        // Use the optimized query with JOIN FETCH to eagerly load order items and products
        return orderRepository.findByUserIdWithItems(userId);
    }

    @Transactional
    public Order createOrder(User user, List<OrderItem> orderItems, BigDecimal totalAmount) {
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");
        
        // First save order to get the ID
        order = orderRepository.save(order);
        
        // Link order items to the order and save them
        for (OrderItem item : orderItems) {
            item.setOrder(order);
            orderItemService.saveOrderItem(item);
        }
        
        // Set the order items list
        order.setOrderItems(orderItems);
        
        // Save order again to persist the cascade
        order = orderRepository.save(order);

        return order;
    }

    public Order updateOrderStatus(Long orderId, String status) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(status);
            return orderRepository.save(order);
        }
        return null;
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }
}


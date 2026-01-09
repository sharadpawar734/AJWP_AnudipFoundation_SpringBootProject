package com.Anudip.FinalProject.service;

import com.Anudip.FinalProject.model.Order;
import com.Anudip.FinalProject.model.OrderItem;
import com.Anudip.FinalProject.repository.OrderItemRepository;
import com.Anudip.FinalProject.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderItemService {

    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private OrderRepository orderRepository;

    public List<OrderItem> getAllOrderItems() {
        return orderItemRepository.findAll();
    }

    public Optional<OrderItem> getOrderItemById(Long id) {
        return orderItemRepository.findById(id);
    }

    public List<OrderItem> getOrderItemsByOrderId(Long orderId) {
        // First try to fetch from database using order_id
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            // Return items directly from the order entity (loaded via cascade)
            if (order.getOrderItems() != null) {
                return order.getOrderItems();
            }
        }
        
        // Fallback: filter from all items (should not happen in normal circumstances)
        return orderItemRepository.findAll().stream()
                .filter(item -> item.getOrder() != null && item.getOrder().getId().equals(orderId))
                .toList();
    }

    public OrderItem saveOrderItem(OrderItem orderItem) {
        return orderItemRepository.save(orderItem);
    }

    public void deleteOrderItem(Long id) {
        orderItemRepository.deleteById(id);
    }
}


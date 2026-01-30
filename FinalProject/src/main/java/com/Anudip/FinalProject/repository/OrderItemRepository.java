package com.Anudip.FinalProject.repository;

import com.Anudip.FinalProject.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    /**
     * Delete all order items associated with a specific product.
     * This is used when deleting a product to prevent foreign key constraint violations.
     */
    void deleteByProductId(Long productId);
    
    /**
     * Delete all order items associated with a specific order.
     */
    void deleteByOrderId(Long orderId);
}

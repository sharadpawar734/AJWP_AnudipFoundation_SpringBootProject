package com.Anudip.FinalProject.repository;

import com.Anudip.FinalProject.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // Custom queries can be added here if needed
}

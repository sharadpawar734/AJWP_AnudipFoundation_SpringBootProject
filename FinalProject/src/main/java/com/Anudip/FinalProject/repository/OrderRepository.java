package com.Anudip.FinalProject.repository;

import com.Anudip.FinalProject.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Find all orders for a specific user with order items and products eagerly loaded.
     * This prevents N+1 query issues and lazy loading problems.
     * Using LEFT JOIN FETCH to eagerly load order items and their products.
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.user.id = :userId ORDER BY o.orderDate DESC")
    List<Order> findByUserIdWithItems(@Param("userId") Long userId);
    
    /**
     * Find all orders with order items and products eagerly loaded for admin view.
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product ORDER BY o.orderDate DESC")
    List<Order> findAllWithItems();
}

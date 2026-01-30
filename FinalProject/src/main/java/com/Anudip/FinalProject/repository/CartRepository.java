package com.Anudip.FinalProject.repository;

import com.Anudip.FinalProject.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByUserId(Long userId);
    Optional<Cart> findByUserIdAndProductId(Long userId, Long productId);
    void deleteByUserIdAndProductId(Long userId, Long productId);
    void deleteByUserId(Long userId);
    long countByUserId(Long userId);
    
    /**
     * Delete all cart items associated with a specific product.
     * This is used when deleting a product to prevent foreign key constraint violations.
     */
    void deleteByProductId(Long productId);
}


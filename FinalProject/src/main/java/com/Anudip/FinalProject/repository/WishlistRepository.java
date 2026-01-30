package com.Anudip.FinalProject.repository;

import com.Anudip.FinalProject.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    
    /**
     * Find all wishlist items for a specific user with products eagerly loaded.
     * This prevents N+1 query issues and lazy loading problems.
     */
    @Query("SELECT w FROM Wishlist w LEFT JOIN FETCH w.product WHERE w.user.id = :userId ORDER BY w.addedAt DESC")
    List<Wishlist> findByUserIdWithProducts(@Param("userId") Long userId);
    
    List<Wishlist> findByUserId(Long userId);
    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);
    void deleteByUserIdAndProductId(Long userId, Long productId);
    void deleteByUserId(Long userId);
    long countByUserId(Long userId);
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * Delete all wishlist items associated with a specific product.
     * This is used when deleting a product to prevent foreign key constraint violations.
     */
    void deleteByProductId(Long productId);
}


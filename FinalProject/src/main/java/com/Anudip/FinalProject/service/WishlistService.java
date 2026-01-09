package com.Anudip.FinalProject.service;

import com.Anudip.FinalProject.model.Product;
import com.Anudip.FinalProject.model.User;
import com.Anudip.FinalProject.model.Wishlist;
import com.Anudip.FinalProject.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    public List<Wishlist> getWishlistByUserId(Long userId) {
        return wishlistRepository.findByUserId(userId);
    }

    public int getWishlistItemCount(Long userId) {
        return (int) wishlistRepository.countByUserId(userId);
    }

    public boolean isProductInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public Wishlist addToWishlist(User user, Product product) {
        // Check if product already in wishlist
        if (wishlistRepository.findByUserIdAndProductId(user.getId(), product.getId()).isPresent()) {
            return null; // Already in wishlist
        }
        
        Wishlist wishlist = new Wishlist(user, product);
        return wishlistRepository.save(wishlist);
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public void clearWishlist(Long userId) {
        wishlistRepository.deleteByUserId(userId);
    }
}


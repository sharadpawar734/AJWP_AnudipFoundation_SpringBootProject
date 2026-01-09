package com.Anudip.FinalProject.service;

import com.Anudip.FinalProject.model.Cart;
import com.Anudip.FinalProject.model.Product;
import com.Anudip.FinalProject.model.User;
import com.Anudip.FinalProject.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    public List<Cart> getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId);
    }

    public int getCartItemCount(Long userId) {
        return (int) cartRepository.countByUserId(userId);
    }

    public BigDecimal getCartTotal(Long userId) {
        List<Cart> cartItems = cartRepository.findByUserId(userId);
        return cartItems.stream()
                .map(Cart::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public Cart addToCart(User user, Product product, Integer quantity) {
        // Check if product already in cart
        Optional<Cart> existingCart = cartRepository.findByUserIdAndProductId(user.getId(), product.getId());
        
        if (existingCart.isPresent()) {
            // Update quantity
            Cart cart = existingCart.get();
            cart.setQuantity(cart.getQuantity() + quantity);
            return cartRepository.save(cart);
        } else {
            // Create new cart item
            Cart cart = new Cart(user, product);
            cart.setQuantity(quantity);
            return cartRepository.save(cart);
        }
    }

    @Transactional
    public Cart updateCartItem(Long cartId, Integer quantity) {
        Optional<Cart> cartOpt = cartRepository.findById(cartId);
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            if (quantity <= 0) {
                cartRepository.delete(cart);
                return null;
            }
            cart.setQuantity(quantity);
            return cartRepository.save(cart);
        }
        return null;
    }

    @Transactional
    public void removeFromCart(Long userId, Long productId) {
        cartRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartRepository.deleteByUserId(userId);
    }

    public boolean isProductInCart(Long userId, Long productId) {
        return cartRepository.findByUserIdAndProductId(userId, productId).isPresent();
    }
}


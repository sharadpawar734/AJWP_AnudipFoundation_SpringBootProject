package com.Anudip.FinalProject.service;

import com.Anudip.FinalProject.model.Product;
import com.Anudip.FinalProject.repository.CartRepository;
import com.Anudip.FinalProject.repository.OrderItemRepository;
import com.Anudip.FinalProject.repository.ProductRepository;
import com.Anudip.FinalProject.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    /**
     * Delete a product and all its related records to prevent foreign key constraint violations.
     * This method performs cascade deletion in the following order:
     * 1. Delete related order items
     * 2. Delete related cart items
     * 3. Delete related wishlist items
     * 4. Delete the product itself
     */
    @Transactional
    public void deleteProduct(Long id) {
        System.out.println("=== DELETING PRODUCT ===");
        System.out.println("Product ID: " + id);
        
        // Step 1: Delete related order items
        System.out.println("Deleting related order items...");
        orderItemRepository.deleteByProductId(id);
        
        // Step 2: Delete related cart items
        System.out.println("Deleting related cart items...");
        cartRepository.deleteByProductId(id);
        
        // Step 3: Delete related wishlist items
        System.out.println("Deleting related wishlist items...");
        wishlistRepository.deleteByProductId(id);
        
        // Step 4: Delete the product
        System.out.println("Deleting product...");
        productRepository.deleteById(id);
        
        System.out.println("Product " + id + " deleted successfully!");
        System.out.println("=======================");
    }
}

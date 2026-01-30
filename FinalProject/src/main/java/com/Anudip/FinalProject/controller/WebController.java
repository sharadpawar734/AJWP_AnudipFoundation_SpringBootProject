package com.Anudip.FinalProject.controller;

import com.Anudip.FinalProject.model.*;
import com.Anudip.FinalProject.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;

@Controller
public class WebController {

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private CartService cartService;

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private OTPService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ResourceLoader resourceLoader;

    // ============ SESSION MANAGEMENT ============

    private User getLoggedInUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            return userService.getUserById(userId).orElse(null);
        }
        return null;
    }

    private void setLoggedInUser(HttpSession session, User user) {
        if (user != null) {
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("email", user.getEmail());
        }
    }

    private void clearSession(HttpSession session) {
        session.removeAttribute("userId");
        session.removeAttribute("username");
        session.removeAttribute("email");
    }

    private boolean isAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    }

    // Helper method to save uploaded file to disk and return URL
    private String saveFile(MultipartFile file, String folder, Long id, String prefix) {
        try {
            if (file == null || file.isEmpty()) {
                return null;
            }
            
            // Get the root directory of the project
            String projectRoot = System.getProperty("user.dir");
            
            // Create upload directory path in src/main/resources/static
            Path uploadDir = Paths.get(projectRoot, "src", "main", "resources", "static", "uploads", folder);
            
            // Also create upload directory in target/classes/static for serving
            Path targetUploadDir = Paths.get(projectRoot, "target", "classes", "static", "uploads", folder);
            
            // Create directories if they don't exist
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            if (!Files.exists(targetUploadDir)) {
                Files.createDirectories(targetUploadDir);
            }
            
            // Get file extension
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // Generate unique filename with timestamp
            String filename = prefix + "_" + System.currentTimeMillis() + "_" + id + extension;
            
            // Save to src directory
            Path srcFilePath = uploadDir.resolve(filename);
            // Save to target directory (for Spring Boot to serve)
            Path targetFilePath = targetUploadDir.resolve(filename);
            
            // Copy file to both locations
            Files.copy(file.getInputStream(), srcFilePath, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(file.getInputStream(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Return URL path that can be served statically
            String urlPath = "/uploads/" + folder + "/" + filename;
            System.out.println("File saved: " + urlPath + " (src: " + srcFilePath + ", target: " + targetFilePath + ")");
            
            return urlPath;
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ============ HOME & NAVIGATION ============

    @GetMapping({"/", "/home", "/index"})
    public String home(Model model, HttpSession session) {
        User user = getLoggedInUser(session);
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("user", user);
        model.addAttribute("isAdmin", isAdmin(session));
        
        if (user != null) {
            model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));
            model.addAttribute("wishlistCount", wishlistService.getWishlistItemCount(user.getId()));
        } else {
            model.addAttribute("cartCount", 0);
            model.addAttribute("wishlistCount", 0);
        }
        
        return "Index";
    }

    @GetMapping("/about")
    public String about(Model model, HttpSession session) {
        model.addAttribute("user", getLoggedInUser(session));
        model.addAttribute("isAdmin", isAdmin(session));
        return "About";
    }

    // ============ AUTHENTICATION ============

    @GetMapping("/login")
    public String login(Model model, HttpSession session) {
        if (session.getAttribute("userId") != null) {
            if (isAdmin(session)) {
                return "redirect:/admin";
            }
            return "redirect:/home";
        }
        return "Login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam String email, @RequestParam String password, 
                             Model model, HttpSession session) {
        // Check for admin first
        if ("admin".equals(email) && "admin123".equals(password)) {
            session.setAttribute("isAdmin", true);
            session.setAttribute("adminUsername", "Super Admin");
            return "redirect:/admin";
        }
        
        // Regular user login
        User user = userService.findByEmail(email);
        if (user != null && user.getPassword().equals(password)) {
            setLoggedInUser(session, user);
            session.removeAttribute("isAdmin");
            return "redirect:/home";
        }
        model.addAttribute("error", "Invalid credentials");
        return "Login";
    }

    @GetMapping("/signup")
    public String signup(Model model, HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/home";
        }
        return "Signup";
    }

    @PostMapping("/signup")
    public String signupSubmit(@RequestParam String username, @RequestParam String email, 
                               @RequestParam String password, 
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) String address,
                               @RequestParam(required = false) String city,
                               @RequestParam(required = false) String state,
                               @RequestParam(required = false) String pincode,
                               @RequestParam(value = "profilePhoto", required = false) MultipartFile profilePhoto,
                               Model model, HttpSession session) {
        if (userService.findByEmail(email) != null) {
            model.addAttribute("error", "Email already registered");
            return "Signup";
        }
        
        // Check if email verification is required and completed
        Boolean emailVerified = (Boolean) session.getAttribute("emailVerified");
        if (emailVerified == null || !emailVerified) {
            model.addAttribute("error", "Please verify your email with OTP before signing up.");
            return "Signup";
        }
        
        // Check if phone verification is required and completed (if phone provided)
        if (phone != null && !phone.trim().isEmpty()) {
            Boolean phoneVerified = (Boolean) session.getAttribute("phoneVerified");
            if (phoneVerified == null || !phoneVerified) {
                model.addAttribute("error", "Please verify your phone number with OTP before signing up.");
                return "Signup";
            }
        }
        
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setPhone(phone);
        user.setAddress(address);
        user.setCity(city);
        user.setState(state);
        user.setPincode(pincode);
        
        // Set default avatar first
        String defaultAvatar = "https://ui-avatars.com/api/?name=" + username + "&background=667eea&color=fff&size=150";
        
        // Handle profile photo upload
        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            // Save user first to get ID
            userService.saveUser(user);
            String photoUrl = saveFile(profilePhoto, "user", user.getId(), "photo");
            if (photoUrl != null) {
                user.setProfilePhoto(photoUrl);
            } else {
                user.setProfilePhoto(defaultAvatar);
            }
        } else {
            user.setProfilePhoto(defaultAvatar);
        }
        
        // Save user with photo URL
        userService.saveUser(user);
        setLoggedInUser(session, user);
        
        // Clear OTP verification session attributes
        session.removeAttribute("emailVerified");
        session.removeAttribute("phoneVerified");
        session.removeAttribute("pendingEmail");
        session.removeAttribute("pendingPhone");
        
        System.out.println("✅ User registered: " + username + " with photo: " + user.getProfilePhoto());
        
        // Send welcome email
        try {
            emailService.sendWelcomeEmail(email, username);
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
        
        return "redirect:/home";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        clearSession(session);
        session.removeAttribute("isAdmin");
        session.removeAttribute("adminUsername");
        return "redirect:/login";
    }

    // ============ USER PROFILE ============

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("orders", orderService.getOrdersByUserId(user.getId()));
        model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));
        return "Profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String username, @RequestParam String email,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) String city,
                                @RequestParam(required = false) String state,
                                @RequestParam(required = false) String pincode,
                                @RequestParam(value = "profilePhoto", required = false) MultipartFile profilePhoto,
                                Model model, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        
        String existingPhotoUrl = user.getProfilePhoto();
        
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAddress(address);
        user.setCity(city);
        user.setState(state);
        user.setPincode(pincode);
        
        // Handle profile photo upload
        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            String photoUrl = saveFile(profilePhoto, "user", user.getId(), "photo");
            if (photoUrl != null) {
                user.setProfilePhoto(photoUrl);
            }
        }
        
        // Keep existing photo if no new one uploaded
        if (user.getProfilePhoto() == null || user.getProfilePhoto().isEmpty()) {
            user.setProfilePhoto(existingPhotoUrl);
        }
        
        userService.saveUser(user);
        
        // Update session
        session.setAttribute("username", username);
        session.setAttribute("email", email);
        
        model.addAttribute("user", user);
        model.addAttribute("orders", orderService.getOrdersByUserId(user.getId()));
        model.addAttribute("message", "Profile updated successfully!");
        return "Profile";
    }

    // ============ PRODUCTS ============

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Long id, Model model, HttpSession session) {
        User user = getLoggedInUser(session);
        Product product = productService.getProductById(id).orElse(null);
        model.addAttribute("product", product);
        model.addAttribute("user", user);
        
        if (user != null) {
            model.addAttribute("isInCart", cartService.isProductInCart(user.getId(), id));
            model.addAttribute("isInWishlist", wishlistService.isProductInWishlist(user.getId(), id));
            model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));
        }
        
        return "product-detail";
    }

    // ============ CART ============

    @GetMapping("/cart")
    public String cart(Model model, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        
        List<Cart> cartItems = cartService.getCartByUserId(user.getId());
        BigDecimal cartTotal = cartService.getCartTotal(user.getId());
        
        model.addAttribute("user", user);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));
        return "Cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId, 
                           @RequestParam(defaultValue = "1") Integer quantity,
                           HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        
        Product product = productService.getProductById(productId).orElse(null);
        if (product != null) {
            cartService.addToCart(user, product, quantity);
        }
        
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestParam Long cartId, 
                            @RequestParam Integer quantity,
                            HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        
        cartService.updateCartItem(cartId, quantity);
        return "redirect:/cart";
    }

    @GetMapping("/cart/remove/{id}")
    public String removeFromCart(@PathVariable Long id, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        
        cartService.removeFromCart(user.getId(), id);
        return "redirect:/cart";
    }

    @GetMapping("/cart/clear")
    public String clearCart(HttpSession session) {
        User user = getLoggedInUser(session);
        if (user != null) {
            cartService.clearCart(user.getId());
        }
        return "redirect:/cart";
    }

    // ============ WISHLIST ============

    @GetMapping("/wishlist")
    public String wishlist(Model model, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        
        List<Wishlist> wishlistItems = wishlistService.getWishlistByUserId(user.getId());
        
        model.addAttribute("user", user);
        model.addAttribute("wishlistItems", wishlistItems);
        model.addAttribute("wishlistCount", wishlistItems.size());
        model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));
        return "Wishlist";
    }

    @GetMapping("/wishlist/add/{productId}")
    public String addToWishlist(@PathVariable Long productId, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        
        Product product = productService.getProductById(productId).orElse(null);
        if (product != null) {
            wishlistService.addToWishlist(user, product);
        }
        
        return "redirect:/wishlist";
    }

    @GetMapping("/wishlist/remove/{id}")
    public String removeFromWishlist(@PathVariable Long id, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        
        wishlistService.removeFromWishlist(user.getId(), id);
        return "redirect:/wishlist";
    }

    // ============ CHECKOUT ============

    @GetMapping("/checkout")
    public String checkout(Model model, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        
        List<Cart> cartItems = cartService.getCartByUserId(user.getId());
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }
        
        BigDecimal cartTotal = cartService.getCartTotal(user.getId());
        
        model.addAttribute("user", user);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("cartCount", cartItems.size());
        return "Checkout";
    }

    @PostMapping("/checkout")
    public String checkoutSubmit(@RequestParam String fullName, @RequestParam String phone,
                                  @RequestParam String address, @RequestParam String city,
                                  @RequestParam String state, @RequestParam String pincode,
                                  @RequestParam String paymentMethod, 
                                  @RequestParam(required = false) String cardNumber,
                                  @RequestParam(required = false) String cardExpiry,
                                  @RequestParam(required = false) String cardCvv,
                                  Model model, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        
        List<Cart> cartItems = cartService.getCartByUserId(user.getId());
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }
        
        BigDecimal totalAmount = cartService.getCartTotal(user.getId());
        
        boolean paymentSuccess = processPayment(paymentMethod, totalAmount, cardNumber, cardExpiry, cardCvv);
        
        if (!paymentSuccess) {
            model.addAttribute("error", "Payment failed. Please try again.");
            model.addAttribute("user", user);
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("cartTotal", totalAmount);
            return "Checkout";
        }
        
        List<OrderItem> orderItems = new ArrayList<>();
        for (Cart cartItem : cartItems) {
            OrderItem item = new OrderItem();
            item.setProduct(cartItem.getProduct());
            item.setQuantity(cartItem.getQuantity());
            item.setPrice(cartItem.getProduct().getPrice());
            orderItems.add(item);
        }
        
        // Create order with PENDING status
        Order order = orderService.createOrder(user, orderItems, totalAmount);
        
        // Update status to PROCESSING
        order = orderService.updateOrderStatus(order.getId(), "PROCESSING");
        
        // Update user shipping info
        user.setPhone(phone);
        user.setAddress(address);
        user.setCity(city);
        user.setState(state);
        user.setPincode(pincode);
        userService.saveUser(user);
        
        // Clear cart after successful order
        cartService.clearCart(user.getId());
        
        // Add success message and redirect
        session.setAttribute("orderSuccess", "Your order #ORD-" + order.getId() + " has been placed successfully!");
        
        return "redirect:/orders";
    }

    private boolean processPayment(String paymentMethod, BigDecimal amount, String cardNumber, String cardExpiry, String cardCvv) {
        switch (paymentMethod) {
            case "COD":
                return true;
            case "UPI":
                return amount.compareTo(BigDecimal.ZERO) > 0;
            case "CARD":
                if (cardNumber != null && cardNumber.length() >= 16 && cardCvv != null && cardCvv.length() == 3) {
                    return amount.compareTo(BigDecimal.ZERO) > 0;
                }
                return false;
            default:
                return false;
        }
    }

    // ============ ORDERS ============

    @GetMapping("/orders")
    public String orders(Model model, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        
        List<Order> orders = orderService.getOrdersByUserId(user.getId());
        
        // Calculate dynamic statistics
        long shippedCount = orders.stream().filter(o -> "SHIPPED".equals(o.getStatus())).count();
        long processingCount = orders.stream().filter(o -> "PROCESSING".equals(o.getStatus())).count();
        long deliveredCount = orders.stream().filter(o -> "DELIVERED".equals(o.getStatus())).count();
        BigDecimal totalSpent = orders.stream()
                .filter(o -> "DELIVERED".equals(o.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        model.addAttribute("user", user);
        model.addAttribute("orders", orders);
        model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));
        model.addAttribute("shippedCount", shippedCount);
        model.addAttribute("processingCount", processingCount);
        model.addAttribute("deliveredCount", deliveredCount);
        model.addAttribute("totalSpent", totalSpent);
        return "Orders";
    }

    @GetMapping("/order/{id}")
    public String orderDetail(@PathVariable Long id, Model model, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        
        Order order = orderService.getOrderById(id).orElse(null);
        if (order != null && order.getUser() != null && order.getUser().getId().equals(user.getId())) {
            model.addAttribute("order", order);
            model.addAttribute("orderItems", orderItemService.getOrderItemsByOrderId(id));
            model.addAttribute("user", user);
            return "order-detail";
        }
        return "redirect:/orders";
    }

    // ============ ADMIN PAGES ============

    @GetMapping("/admin")
    public String admin(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/admin-login";
        }
        model.addAttribute("adminUsername", session.getAttribute("adminUsername"));
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("orders", orderService.getAllOrders());
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("totalRevenue", orderService.getTotalRevenue());
        return "Admin";
    }

    @GetMapping("/admin-login")
    public String adminLogin(Model model, HttpSession session) {
        if (isAdmin(session)) {
            return "redirect:/admin";
        }
        return "admin-login";
    }

    @PostMapping("/admin-login")
    public String adminLoginSubmit(@RequestParam String username, @RequestParam String password, 
                                   Model model, HttpSession session) {
        if ("admin".equalsIgnoreCase(username.trim()) && "admin123".equals(password.trim())) {
            session.setAttribute("isAdmin", true);
            session.setAttribute("adminUsername", "Admin");
            return "redirect:/admin";
        }
        model.addAttribute("error", "Invalid admin credentials. Use: admin / admin123");
        return "admin-login";
    }

    // ============ ADMIN PRODUCT MANAGEMENT ============

    @GetMapping("/admin/products")
    public String adminProducts(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/admin-login";
        }
        model.addAttribute("products", productService.getAllProducts());
        return "Admin";
    }

    @PostMapping("/admin/product/add")
    public String addProduct(@RequestParam String name, @RequestParam String description,
                             @RequestParam BigDecimal price, @RequestParam Integer stock,
                             @RequestParam(value = "image", required = false) MultipartFile image,
                             Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/admin-login";
        }
        
        System.out.println("=== ADDING PRODUCT ===");
        System.out.println("Name: " + name);
        System.out.println("Description: " + description);
        System.out.println("Price: " + price);
        System.out.println("Stock: " + stock);
        System.out.println("Image provided: " + (image != null ? !image.isEmpty() : false));
        
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStock(stock);
        
        // First save product to get ID
        product = productService.saveProduct(product);
        System.out.println("Product saved with ID: " + product.getId());
        
        // Handle product image upload
        if (image != null && !image.isEmpty()) {
            String imageUrl = saveFile(image, "book", product.getId(), "book");
            if (imageUrl != null) {
                product.setImageUrl(imageUrl);
                System.out.println("Image saved: " + imageUrl);
            }
        }
        
        // Set default image if no upload
        if (product.getImageUrl() == null || product.getImageUrl().isEmpty()) {
            product.setImageUrl("https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400&h=300&fit=crop");
        }
        
        // Save product again with image URL
        product = productService.saveProduct(product);
        System.out.println("Product saved with image: " + product.getName() + " (ID: " + product.getId() + ")");
        System.out.println("Image URL: " + product.getImageUrl());
        System.out.println("=====================");
        
        return "redirect:/admin";
    }

    @GetMapping("/admin/product/delete/{id}")
    public String deleteProduct(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/admin-login";
        }
        productService.deleteProduct(id);
        return "redirect:/admin";
    }

    // ============ ADMIN ORDER MANAGEMENT ============

    @GetMapping("/admin/orders")
    public String adminOrders(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/admin-login";
        }
        model.addAttribute("orders", orderService.getAllOrders());
        return "Admin";
    }

    @PostMapping("/admin/order/update")
    public String updateOrderStatus(@RequestParam Long orderId, @RequestParam String status, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/admin-login";
        }
        orderService.updateOrderStatus(orderId, status);
        return "redirect:/admin";
    }

    // ============ ADMIN USER MANAGEMENT ============

    @GetMapping("/admin/users")
    public String adminUsers(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/admin-login";
        }
        model.addAttribute("users", userService.getAllUsers());
        return "Admin";
    }

    // ============ SEARCH ============

    @GetMapping("/search")
    public String search(@RequestParam String query, Model model, HttpSession session) {
        User user = getLoggedInUser(session);
        List<Product> allProducts = productService.getAllProducts();
        List<Product> searchResults = allProducts.stream()
                .filter(p -> p.getName().toLowerCase().contains(query.toLowerCase()) ||
                            (p.getDescription() != null && p.getDescription().toLowerCase().contains(query.toLowerCase())))
                .toList();
        model.addAttribute("products", searchResults);
        model.addAttribute("searchQuery", query);
        model.addAttribute("user", user);
        
        if (user != null) {
            model.addAttribute("cartCount", cartService.getCartItemCount(user.getId()));
        }
        return "Index";
    }

    // ============ OTP VERIFICATION ============

    /**
     * Send OTP to email
     */
    @PostMapping("/send-email-otp")
    @ResponseBody
    public Map<String, Object> sendEmailOTP(@RequestParam String email, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        // Check if email is already registered
        if (userService.findByEmail(email) != null) {
            response.put("success", false);
            response.put("message", "Email is already registered.");
            return response;
        }
        
        // Generate and send OTP
        String otp = otpService.generateOTP();
        otpService.sendOTPToEmail(email, otp);
        
        // Store pending email in session
        session.setAttribute("pendingEmail", email);
        
        response.put("success", true);
        response.put("message", "OTP sent to your email. Please check your inbox.");
        return response;
    }

    /**
     * Send OTP to phone
     */
    @PostMapping("/send-phone-otp")
    @ResponseBody
    public Map<String, Object> sendPhoneOTP(@RequestParam String phone, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate phone format
        if (phone == null || !phone.matches("^[0-9]{10}$")) {
            response.put("success", false);
            response.put("message", "Please enter a valid 10-digit phone number.");
            return response;
        }
        
        // Generate and send OTP
        String otp = otpService.generateOTP();
        otpService.sendOTPToPhone(phone, otp);
        
        // Store pending phone in session
        session.setAttribute("pendingPhone", phone);
        
        response.put("success", true);
        response.put("message", "OTP sent to your phone via SMS.");
        return response;
    }

    /**
     * Verify OTP
     */
    @PostMapping("/verify-otp")
    @ResponseBody
    public Map<String, Object> verifyOTP(@RequestParam String otp, 
                                          @RequestParam String type,
                                          HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        String key = type.equals("email") ? 
            (String) session.getAttribute("pendingEmail") : 
            (String) session.getAttribute("pendingPhone");
        
        if (key == null) {
            response.put("success", false);
            response.put("message", "No pending OTP found. Please request a new OTP.");
            return response;
        }
        
        OTPService.OTPVerificationResult result = otpService.verifyOTP(key, otp);
        
        if (result.isSuccess()) {
            // Mark as verified in session
            if (type.equals("email")) {
                session.setAttribute("emailVerified", true);
                session.removeAttribute("pendingEmail");
            } else {
                session.setAttribute("phoneVerified", true);
                session.removeAttribute("pendingPhone");
            }
            response.put("success", true);
            response.put("message", result.getMessage());
        } else {
            response.put("success", false);
            response.put("message", result.getMessage());
        }
        
        return response;
    }

    /**
     * Check if email is verified
     */
    @GetMapping("/check-email-verified")
    @ResponseBody
    public Map<String, Object> checkEmailVerified(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        response.put("verified", Boolean.TRUE.equals(session.getAttribute("emailVerified")));
        return response;
    }

    /**
     * Check if phone is verified
     */
    @GetMapping("/check-phone-verified")
    @ResponseBody
    public Map<String, Object> checkPhoneVerified(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        response.put("verified", Boolean.TRUE.equals(session.getAttribute("phoneVerified")));
        return response;
    }
}

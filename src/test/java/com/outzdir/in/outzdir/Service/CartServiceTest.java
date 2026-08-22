package com.outzdir.in.outzdir.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.outzdir.in.outzdir.DTO.CartItemRequestDTO;
import com.outzdir.in.outzdir.DTO.RemoveProductFromCartDTO;
import com.outzdir.in.outzdir.Entity.Cart;
import com.outzdir.in.outzdir.Entity.Cartitems;
import com.outzdir.in.outzdir.Entity.Discount;
import com.outzdir.in.outzdir.Entity.DiscountItem;
import com.outzdir.in.outzdir.Entity.Product;
import com.outzdir.in.outzdir.Entity.Users;
import com.outzdir.in.outzdir.Entity.TYPE.CartStatus;
import com.outzdir.in.outzdir.Entity.TYPE.ProductStatus;
import com.outzdir.in.outzdir.Entity.TYPE.DiscountStatus;
import com.outzdir.in.outzdir.Entity.TYPE.DiscountType;
import com.outzdir.in.outzdir.Repository.CartRepository;
import com.outzdir.in.outzdir.Repository.CartitemsRepository;
import com.outzdir.in.outzdir.Repository.DiscountRepository;
import com.outzdir.in.outzdir.Repository.ProductRepository;
import com.outzdir.in.outzdir.Repository.UsersRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartitemsRepository cartitemsRepository;

    @Mock
    private ProductRepository productsRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private DiscountRepository discountRepository;

    @InjectMocks
    private CartService cartService;

    private Users testUser;
    private Product testProduct;
    private CartItemRequestDTO cartItemRequestDTO;
    private String email = "test@example.com";

    @BeforeEach
    void setUp() {
        testUser = new Users();
        testUser.setId(1L);
        testUser.setEmail(email);
        testUser.setName("Test User");

        testProduct = new Product();
        testProduct.setId(10L);
        testProduct.setProduct_name("Test Product");
        testProduct.setPrice(100.0);
        testProduct.setQuantity(10L); // 10 in stock
        testProduct.setProductStatus(ProductStatus.ACTIVE);

        cartItemRequestDTO = new CartItemRequestDTO();
        cartItemRequestDTO.setProductId(10L);
        cartItemRequestDTO.setQuantity(2L);
    }

    @Test
    void testGetMyCart_UserNotFound() {
        when(usersRepository.findByEmail(email)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> cartService.getMyCart(email));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testGetMyCart_EmptyCart_CreatesNew() {
        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart cart = cartService.getMyCart(email);

        assertNotNull(cart);
        assertEquals(testUser, cart.getUser());
        assertEquals(CartStatus.ACTIVE, cart.getCartStatus());
        assertEquals(0.0, cart.getSubtotal());
        assertEquals(0.0, cart.getDiscountAmount());
        assertEquals(0.0, cart.getTotal());
        assertTrue(cart.getCartItems().isEmpty());
    }

    @Test
    void testAddProductToCart_Success_NewItem() {
        Cart cart = new Cart();
        cart.setUser(testUser);
        cart.setCartItems(new ArrayList<>());

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(productsRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));

        cartService.addProductToCart(cartItemRequestDTO, email);

        assertEquals(1, cart.getCartItems().size());
        Cartitems cartItem = cart.getCartItems().get(0);
        assertEquals(testProduct, cartItem.getProduct());
        assertEquals(2L, cartItem.getQuantity());
        assertEquals(100.0, cartItem.getUnitPrice());
        assertEquals(200.0, cart.getSubtotal());
        assertEquals(200.0, cart.getTotal());

        verify(cartRepository).save(cart);
    }

    @Test
    void testAddProductToCart_Success_SameItem_IncrementsQuantity() {
        Cart cart = new Cart();
        cart.setUser(testUser);
        cart.setCartItems(new ArrayList<>());

        Cartitems existingItem = new Cartitems();
        existingItem.setCart(cart);
        existingItem.setProduct(testProduct);
        existingItem.setQuantity(3L);
        existingItem.setUnitPrice(100.0);
        cart.getCartItems().add(existingItem);

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(productsRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));

        cartService.addProductToCart(cartItemRequestDTO, email);

        assertEquals(1, cart.getCartItems().size());
        Cartitems cartItem = cart.getCartItems().get(0);
        assertEquals(5L, cartItem.getQuantity()); // 3 + 2 = 5
        assertEquals(500.0, cart.getSubtotal());
        assertEquals(500.0, cart.getTotal());

        verify(cartRepository).save(cart);
    }

    @Test
    void testAddProductToCart_InvalidQuantity() {
        cartItemRequestDTO.setQuantity(0L);
        when(usersRepository.findByEmail(email)).thenReturn(testUser);

        assertThrows(IllegalArgumentException.class, () -> cartService.addProductToCart(cartItemRequestDTO, email));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testAddProductToCart_InactiveProduct() {
        testProduct.setProductStatus(ProductStatus.INACTIVE);

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(productsRepository.findById(10L)).thenReturn(Optional.of(testProduct));

        assertThrows(IllegalArgumentException.class, () -> cartService.addProductToCart(cartItemRequestDTO, email));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testAddProductToCart_QuantityAboveStock() {
        cartItemRequestDTO.setQuantity(11L); // Product has 10 in stock

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(productsRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(new Cart()));

        assertThrows(IllegalArgumentException.class, () -> cartService.addProductToCart(cartItemRequestDTO, email));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testUpdateCartItemQuantity_Success() {
        Cart cart = new Cart();
        cart.setUser(testUser);
        cart.setCartItems(new ArrayList<>());

        Cartitems existingItem = new Cartitems();
        existingItem.setCart(cart);
        existingItem.setProduct(testProduct);
        existingItem.setQuantity(3L);
        existingItem.setUnitPrice(100.0);
        cart.getCartItems().add(existingItem);

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));

        cartService.updateCartItemQuantity(10L, 5L, email);

        assertEquals(5L, existingItem.getQuantity());
        assertEquals(500.0, cart.getSubtotal());
        assertEquals(500.0, cart.getTotal());

        verify(cartRepository).save(cart);
    }

    @Test
    void testUpdateCartItemQuantity_QuantityAboveStock() {
        Cart cart = new Cart();
        cart.setUser(testUser);
        cart.setCartItems(new ArrayList<>());

        Cartitems existingItem = new Cartitems();
        existingItem.setCart(cart);
        existingItem.setProduct(testProduct);
        existingItem.setQuantity(3L);
        existingItem.setUnitPrice(100.0);
        cart.getCartItems().add(existingItem);

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));

        assertThrows(IllegalArgumentException.class, () -> cartService.updateCartItemQuantity(10L, 11L, email));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testUpdateCartItemQuantity_ProductNotInCart() {
        Cart cart = new Cart();
        cart.setUser(testUser);
        cart.setCartItems(new ArrayList<>()); // Cart is empty

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));

        assertThrows(IllegalArgumentException.class, () -> cartService.updateCartItemQuantity(10L, 5L, email));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testRemoveCartItem_Success() {
        Cart cart = new Cart();
        cart.setUser(testUser);
        cart.setCartItems(new ArrayList<>());

        Cartitems existingItem = new Cartitems();
        existingItem.setCart(cart);
        existingItem.setProduct(testProduct);
        existingItem.setQuantity(3L);
        existingItem.setUnitPrice(100.0);
        cart.getCartItems().add(existingItem);

        RemoveProductFromCartDTO removeDTO = new RemoveProductFromCartDTO();
        removeDTO.setProduct_id(10L);

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));

        cartService.removeCartItem(removeDTO, email);

        assertTrue(cart.getCartItems().isEmpty());
        assertEquals(0.0, cart.getSubtotal());
        assertEquals(0.0, cart.getTotal());

        verify(cartRepository).save(cart);
    }

    @Test
    void testApplyCoupon_Success_Percentage() {
        Cart cart = new Cart();
        cart.setUser(testUser);
        cart.setCartItems(new ArrayList<>());

        Cartitems item = new Cartitems();
        item.setCart(cart);
        item.setProduct(testProduct);
        item.setQuantity(3L);
        item.setUnitPrice(100.0); // Subtotal = 300.0
        cart.getCartItems().add(item);

        Discount discount = new Discount();
        discount.setId(1L);
        discount.setCode("DISCOUNT20");
        discount.setType(DiscountType.PERCENTAGE);
        discount.setValue(20.0); // 20%
        discount.setActive(DiscountStatus.ACTIVE);

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(discountRepository.findByCode("DISCOUNT20")).thenReturn(Optional.of(discount));

        cartService.applyCoupon("DISCOUNT20", email);

        assertEquals(discount, cart.getAppliedDiscount());
        assertEquals(300.0, cart.getSubtotal());
        assertEquals(60.0, cart.getDiscountAmount()); // 20% of 300.0
        assertEquals(240.0, cart.getTotal());

        verify(cartRepository).save(cart);
    }

    @Test
    void testApplyCoupon_Success_Flat() {
        Cart cart = new Cart();
        cart.setUser(testUser);
        cart.setCartItems(new ArrayList<>());

        Cartitems item = new Cartitems();
        item.setCart(cart);
        item.setProduct(testProduct);
        item.setQuantity(3L);
        item.setUnitPrice(100.0); // Subtotal = 300.0
        cart.getCartItems().add(item);

        Discount discount = new Discount();
        discount.setId(2L);
        discount.setCode("FLAT50");
        discount.setType(DiscountType.FLAT);
        discount.setValue(50.0); // $50 off
        discount.setActive(DiscountStatus.ACTIVE);

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(discountRepository.findByCode("FLAT50")).thenReturn(Optional.of(discount));

        cartService.applyCoupon("FLAT50", email);

        assertEquals(discount, cart.getAppliedDiscount());
        assertEquals(300.0, cart.getSubtotal());
        assertEquals(50.0, cart.getDiscountAmount());
        assertEquals(250.0, cart.getTotal());

        verify(cartRepository).save(cart);
    }

    @Test
    void testApplyCoupon_InvalidCoupon() {
        Cart cart = new Cart();
        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(discountRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> cartService.applyCoupon("INVALID", email));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testApplyCoupon_InactiveDiscount() {
        Cart cart = new Cart();
        Discount discount = new Discount();
        discount.setActive(DiscountStatus.INACTIVE);

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(discountRepository.findByCode("INACTIVE")).thenReturn(Optional.of(discount));

        assertThrows(IllegalArgumentException.class, () -> cartService.applyCoupon("INACTIVE", email));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testApplyCoupon_ExpiredDiscount() {
        Cart cart = new Cart();
        Discount discount = new Discount();
        discount.setActive(DiscountStatus.ACTIVE);
        discount.setEndDate(LocalDateTime.now().minusDays(1)); // expired yesterday

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(discountRepository.findByCode("EXPIRED")).thenReturn(Optional.of(discount));

        assertThrows(IllegalArgumentException.class, () -> cartService.applyCoupon("EXPIRED", email));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testApplyCoupon_NotYetActiveDiscount() {
        Cart cart = new Cart();
        Discount discount = new Discount();
        discount.setActive(DiscountStatus.ACTIVE);
        discount.setStartDate(LocalDateTime.now().plusDays(1)); // starts tomorrow

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(discountRepository.findByCode("FUTURE")).thenReturn(Optional.of(discount));

        assertThrows(IllegalArgumentException.class, () -> cartService.applyCoupon("FUTURE", email));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testApplyCoupon_MinimumCartValueNotMet() {
        Cart cart = new Cart();
        cart.setUser(testUser);
        cart.setCartItems(new ArrayList<>());

        Cartitems item = new Cartitems();
        item.setCart(cart);
        item.setProduct(testProduct);
        item.setQuantity(2L);
        item.setUnitPrice(100.0); // Subtotal = 200.0
        cart.getCartItems().add(item);

        Discount discount = new Discount();
        discount.setCode("MIN500");
        discount.setActive(DiscountStatus.ACTIVE);
        discount.setMinCartValue(500.0); // Min subtotal of 500 required

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(discountRepository.findByCode("MIN500")).thenReturn(Optional.of(discount));

        assertThrows(IllegalArgumentException.class, () -> cartService.applyCoupon("MIN500", email));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testApplyCoupon_MaxDiscountCap() {
        Cart cart = new Cart();
        cart.setUser(testUser);
        cart.setCartItems(new ArrayList<>());

        Cartitems item = new Cartitems();
        item.setCart(cart);
        item.setProduct(testProduct);
        item.setQuantity(3L);
        item.setUnitPrice(100.0); // Subtotal = 300.0
        cart.getCartItems().add(item);

        Discount discount = new Discount();
        discount.setCode("CAP50");
        discount.setType(DiscountType.PERCENTAGE);
        discount.setValue(50.0); // 50% discount -> would be 150.0
        discount.setMaxDiscount(50.0); // Max discount capped at 50.0
        discount.setActive(DiscountStatus.ACTIVE);

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(discountRepository.findByCode("CAP50")).thenReturn(Optional.of(discount));

        cartService.applyCoupon("CAP50", email);

        assertEquals(50.0, cart.getDiscountAmount()); // capped at 50.0 instead of 150.0
        assertEquals(250.0, cart.getTotal());
    }

    @Test
    void testApplyCoupon_EligibleProductOnly() {
        Cart cart = new Cart();
        cart.setUser(testUser);
        cart.setCartItems(new ArrayList<>());

        // Eligible product
        Cartitems item1 = new Cartitems();
        item1.setCart(cart);
        item1.setProduct(testProduct); // Product ID = 10
        item1.setQuantity(2L);
        item1.setUnitPrice(100.0); // Subtotal = 200.0
        cart.getCartItems().add(item1);

        // Non-eligible product
        Product nonEligibleProduct = new Product();
        nonEligibleProduct.setId(20L);
        nonEligibleProduct.setPrice(150.0);

        Cartitems item2 = new Cartitems();
        item2.setCart(cart);
        item2.setProduct(nonEligibleProduct);
        item2.setQuantity(1L);
        item2.setUnitPrice(150.0); // Subtotal = 150.0. Combined subtotal = 350.0
        cart.getCartItems().add(item2);

        Discount discount = new Discount();
        discount.setCode("ELIGIBLE");
        discount.setType(DiscountType.PERCENTAGE);
        discount.setValue(20.0); // 20% discount on eligible subtotal (200.0 * 20% = 40.0)
        discount.setActive(DiscountStatus.ACTIVE);

        DiscountItem discountItem = new DiscountItem();
        discountItem.setProduct(testProduct);
        discount.setDiscountItems(List.of(discountItem));

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(discountRepository.findByCode("ELIGIBLE")).thenReturn(Optional.of(discount));

        cartService.applyCoupon("ELIGIBLE", email);

        assertEquals(40.0, cart.getDiscountAmount()); // only eligible product gets 20% off
        assertEquals(310.0, cart.getTotal()); // 350.0 - 40.0 = 310.0
    }

    @Test
    void testApplyCoupon_NonEligibleProductThrows() {
        Cart cart = new Cart();
        cart.setUser(testUser);
        cart.setCartItems(new ArrayList<>());

        Cartitems item = new Cartitems();
        item.setCart(cart);
        item.setProduct(testProduct); // Product ID = 10
        item.setQuantity(2L);
        item.setUnitPrice(100.0);
        cart.getCartItems().add(item);

        Discount discount = new Discount();
        discount.setCode("OTHER");
        discount.setActive(DiscountStatus.ACTIVE);

        Product otherProduct = new Product();
        otherProduct.setId(99L);
        DiscountItem discountItem = new DiscountItem();
        discountItem.setProduct(otherProduct);
        discount.setDiscountItems(List.of(discountItem)); // coupon restricted to product ID 99

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(discountRepository.findByCode("OTHER")).thenReturn(Optional.of(discount));

        assertThrows(IllegalArgumentException.class, () -> cartService.applyCoupon("OTHER", email));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testRemoveCoupon_Success() {
        Cart cart = new Cart();
        cart.setUser(testUser);
        cart.setCartItems(new ArrayList<>());

        Cartitems item = new Cartitems();
        item.setCart(cart);
        item.setProduct(testProduct);
        item.setQuantity(2L);
        item.setUnitPrice(100.0);
        cart.getCartItems().add(item);

        Discount discount = new Discount();
        discount.setCode("FLAT50");
        discount.setType(DiscountType.FLAT);
        discount.setValue(50.0);
        discount.setActive(DiscountStatus.ACTIVE);

        cart.setAppliedDiscount(discount);
        cart.setDiscountAmount(50.0);
        cart.setSubtotal(200.0);
        cart.setTotal(150.0);

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));

        cartService.removeCoupon(email);

        assertNull(cart.getAppliedDiscount());
        assertEquals(0.0, cart.getDiscountAmount());
        assertEquals(200.0, cart.getSubtotal());
        assertEquals(200.0, cart.getTotal());

        verify(cartRepository).save(cart);
    }
}

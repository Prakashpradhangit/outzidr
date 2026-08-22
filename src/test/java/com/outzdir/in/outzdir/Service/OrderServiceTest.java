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
import org.modelmapper.ModelMapper;

import com.outzdir.in.outzdir.DTO.OrderRequestDTO;
import com.outzdir.in.outzdir.Entity.Cart;
import com.outzdir.in.outzdir.Entity.Cartitems;
import com.outzdir.in.outzdir.Entity.Discount;
import com.outzdir.in.outzdir.Entity.DiscountItem;
import com.outzdir.in.outzdir.Entity.Order;
import com.outzdir.in.outzdir.Entity.Orderitem;
import com.outzdir.in.outzdir.Entity.Product;
import com.outzdir.in.outzdir.Entity.Users;
import com.outzdir.in.outzdir.Entity.TYPE.DiscountStatus;
import com.outzdir.in.outzdir.Entity.TYPE.DiscountType;
import com.outzdir.in.outzdir.Entity.TYPE.OrderStatus;
import com.outzdir.in.outzdir.Entity.TYPE.PaymentStatus;
import com.outzdir.in.outzdir.Entity.TYPE.ProductStatus;
import com.outzdir.in.outzdir.Repository.CartRepository;
import com.outzdir.in.outzdir.Repository.OrderRepository;
import com.outzdir.in.outzdir.Repository.ProductRepository;
import com.outzdir.in.outzdir.Repository.UsersRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private OrderService orderService;

    private Users testUser;
    private Product testProduct;
    private Cart cart;
    private Cartitems cartitem;
    private OrderRequestDTO orderRequestDTO;
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
        testProduct.setQuantity(10L); // 10 available stock
        testProduct.setProductStatus(ProductStatus.ACTIVE);

        cart = new Cart();
        cart.setId(5L);
        cart.setUser(testUser);
        cart.setCartItems(new ArrayList<>());
        cart.setSubtotal(200.0);
        cart.setDiscountAmount(0.0);
        cart.setTotal(200.0);

        cartitem = new Cartitems();
        cartitem.setId(100L);
        cartitem.setCart(cart);
        cartitem.setProduct(testProduct);
        cartitem.setQuantity(2L); // order quantity 2
        cartitem.setUnitPrice(100.0);
        cart.getCartItems().add(cartitem);

        orderRequestDTO = new OrderRequestDTO("COD", "123 Test Street");
    }

    @Test
    void testCreateOrder_Success_COD() {
        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.createOrder(orderRequestDTO, email);

        assertNotNull(order);
        assertEquals(testUser, order.getUser());
        assertEquals(200.0, order.getSubtotal());
        assertEquals(0.0, order.getDiscountAmount());
        assertEquals(200.0, order.getTotal());
        assertEquals("COD", order.getPaymentMethod());
        assertEquals("123 Test Street", order.getShippingAddress());
        assertEquals(OrderStatus.CREATED, order.getOrderStatus());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());

        assertEquals(1, order.getOrderItems().size());
        Orderitem orderitem = order.getOrderItems().get(0);
        assertEquals(testProduct, orderitem.getProduct());
        assertEquals(2L, orderitem.getQuantity());
        assertEquals(100.0, orderitem.getUnitPrice());

        // Verify stock is reduced
        assertEquals(8L, testProduct.getQuantity()); // 10 - 2 = 8
        verify(productRepository).save(testProduct);

        // Verify cart is cleared
        assertTrue(cart.getCartItems().isEmpty());
        assertNull(cart.getAppliedDiscount());
        assertEquals(0.0, cart.getSubtotal());
        assertEquals(0.0, cart.getDiscountAmount());
        assertEquals(0.0, cart.getTotal());
        verify(cartRepository).save(cart);

        verify(orderRepository).save(order);
    }

    @Test
    void testCreateOrder_EmptyCart() {
        cart.getCartItems().clear();

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(orderRequestDTO, email));
        assertTrue(exception.getMessage().contains("shopping cart is empty"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCreateOrder_InsufficientInventory() {
        cartitem.setQuantity(11L); // Product only has 10L stock

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(testProduct));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(orderRequestDTO, email));
        assertTrue(exception.getMessage().contains("Insufficient stock"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCreateOrder_InactiveProduct() {
        testProduct.setProductStatus(ProductStatus.INACTIVE);

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(testProduct));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(orderRequestDTO, email));
        assertTrue(exception.getMessage().contains("Product is no longer active"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCreateOrder_CouponApplied_Success() {
        Discount discount = new Discount();
        discount.setId(1L);
        discount.setCode("FLAT50");
        discount.setType(DiscountType.FLAT);
        discount.setValue(50.0);
        discount.setActive(DiscountStatus.ACTIVE);
        cart.setAppliedDiscount(discount);

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.createOrder(orderRequestDTO, email);

        assertNotNull(order);
        assertEquals(discount, order.getAppliedDiscount());
        assertEquals(200.0, order.getSubtotal());
        assertEquals(50.0, order.getDiscountAmount());
        assertEquals(150.0, order.getTotal());

        // Verify cart is cleared
        assertTrue(cart.getCartItems().isEmpty());
        assertNull(cart.getAppliedDiscount());
        assertEquals(0.0, cart.getTotal());
        verify(cartRepository).save(cart);
    }

    @Test
    void testCreateOrder_CouponExpired() {
        Discount discount = new Discount();
        discount.setId(1L);
        discount.setCode("EXPIRED");
        discount.setActive(DiscountStatus.ACTIVE);
        discount.setEndDate(LocalDateTime.now().minusDays(1)); // Expired
        cart.setAppliedDiscount(discount);

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(testProduct));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(orderRequestDTO, email));
        assertTrue(exception.getMessage().contains("Applied coupon has expired"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCreateOrder_CouponMinCartValueNotMet() {
        Discount discount = new Discount();
        discount.setId(1L);
        discount.setCode("MIN500");
        discount.setActive(DiscountStatus.ACTIVE);
        discount.setMinCartValue(500.0); // requires 500.0, current subtotal = 200.0
        cart.setAppliedDiscount(discount);

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(testProduct));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(orderRequestDTO, email));
        assertTrue(exception.getMessage().contains("Minimum cart value"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCreateOrder_CouponProductNotEligible() {
        Discount discount = new Discount();
        discount.setId(1L);
        discount.setCode("OTHER");
        discount.setActive(DiscountStatus.ACTIVE);

        Product otherProduct = new Product();
        otherProduct.setId(99L);
        DiscountItem discountItem = new DiscountItem();
        discountItem.setProduct(otherProduct);
        discount.setDiscountItems(List.of(discountItem)); // coupon only for product 99L, cart has 10L
        cart.setAppliedDiscount(discount);

        when(usersRepository.findByEmail(email)).thenReturn(testUser);
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(testProduct));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(orderRequestDTO, email));
        assertTrue(exception.getMessage().contains("None of the products in your cart are eligible"));
        verify(orderRepository, never()).save(any());
    }
}

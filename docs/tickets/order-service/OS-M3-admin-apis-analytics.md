# OS-M3: Order Service Admin APIs & Analytics

**Phase**: M3 Admin/Advanced | **Service**: order-service | **Priority**: Medium | **Estimated Effort**: 4-5 days

## 🎯 **Summary**
Implement comprehensive admin APIs for order management, customer service tools, financial reporting, and analytics. Provide admin interface for order oversight, refund processing, Romanian invoice management, and business intelligence for the e-commerce platform.

## 📋 **Scope**

### **Admin Endpoints (via gateway `/api/admin/...`)**

#### **Order Management APIs**
- `GET /admin/orders` - Get all orders with advanced search and filtering
- `GET /admin/orders/{orderId}` - Get detailed order with full admin information
- `PUT /admin/orders/{orderId}/status` - Update order status with admin override
- `POST /admin/orders/{orderId}/cancel` - Admin cancel order with refund processing
- `POST /admin/orders/{orderId}/refund` - Process partial or full refunds
- `GET /admin/orders/{orderId}/timeline` - Complete order timeline with admin notes

#### **Customer Service APIs**
- `GET /admin/orders/customer/{customerId}` - Get all orders for specific customer
- `POST /admin/orders/{orderId}/notes` - Add admin notes to order
- `GET /admin/orders/problematic` - Get orders requiring admin attention
- `POST /admin/orders/bulk-update` - Bulk order status updates
- `GET /admin/orders/search` - Advanced order search with multiple criteria

#### **Financial Management APIs**
- `GET /admin/orders/invoices` - List all invoices with Romanian compliance data
- `POST /admin/orders/{orderId}/invoice/regenerate` - Regenerate invoice (admin only)
- `GET /admin/orders/financial-summary` - Financial reporting and analytics
- `GET /admin/orders/refunds` - Refund management and tracking

#### **Analytics & Reporting APIs**
- `GET /admin/orders/analytics/overview` - Order analytics dashboard data
- `GET /admin/orders/analytics/revenue` - Revenue analytics with time periods
- `GET /admin/orders/analytics/conversion` - Conversion funnel analytics
- `GET /admin/orders/export` - Export orders for financial reporting

## 🏗️ **Implementation Requirements**

### **Admin Services (Implementation-First)**
```java
@Service
@Transactional
public class OrderAdminService {
    // Order management for admin interface
    // Advanced search and filtering
    // Bulk operations for efficiency
    // Admin override capabilities
    
    public Page<OrderAdminDto> getOrdersWithFilters(OrderSearchCriteria criteria, Pageable pageable) {
        // Advanced filtering by status, date range, customer, amount
        // Full-text search across order data
        // Sorting by multiple criteria
        // Efficient pagination for large datasets
    }
    
    public OrderAdminDetailDto getOrderAdminDetails(Long orderId) {
        // Complete order information for admin
        // Payment history and refund details
        // Customer communication history
        // Internal notes and flags
    }
    
    public RefundResult processAdminRefund(Long orderId, AdminRefundRequest request) {
        // Admin-initiated refund processing
        // Override normal refund restrictions
        // Comprehensive audit logging
        // Integration with Stripe refund API
    }
}

@Service
@Transactional
public class OrderAnalyticsService {
    // Business intelligence and reporting
    // Revenue analytics and trends
    // Order conversion funnel analysis
    // Customer behavior insights
    
    public OrderAnalyticsOverview getAnalyticsOverview(DateRange dateRange) {
        // Key metrics: total orders, revenue, conversion rate
        // Period-over-period comparisons
        // Top products and categories
        // Geographic distribution analysis
    }
    
    public RevenueAnalytics getRevenueAnalytics(DateRange dateRange, RevenueGrouping grouping) {
        // Revenue trends over time
        // Breakdown by product categories
        // Payment method analysis
        // Refund impact on revenue
    }
}

@Service
@Transactional
public class InvoiceManagementService {
    // Romanian invoice compliance and management
    // Invoice regeneration and correction
    // Bulk invoice operations
    // Legal compliance reporting
    
    public Invoice regenerateInvoice(Long orderId, InvoiceRegenerationRequest request) {
        // Admin-only invoice regeneration
        // Maintain legal compliance with sequential numbering
        // Update invoice data while preserving audit trail
        // Generate new PDF with corrections
    }
    
    public InvoiceComplianceReport getComplianceReport(DateRange dateRange) {
        // Romanian legal compliance verification
        // Sequential numbering validation
        // Missing or corrupted invoice detection
        // VAT reporting summary
    }
}
```

### **Admin DTOs**
```
dto/admin/request/
├── OrderSearchCriteriaDto.java
├── AdminRefundRequestDto.java
├── OrderStatusUpdateRequestDto.java
├── BulkOrderUpdateRequestDto.java
├── InvoiceRegenerationRequestDto.java
└── AdminNoteRequestDto.java

dto/admin/response/
├── OrderAdminDto.java
├── OrderAdminDetailDto.java
├── OrderAnalyticsOverviewDto.java
├── RevenueAnalyticsDto.java
├── CustomerOrderSummaryDto.java
├── RefundManagementDto.java
└── InvoiceComplianceReportDto.java
```

### **Admin Controllers with OpenAPI**
```java
@RestController
@RequestMapping("/admin/orders")
@Tag(name = "Order Administration", description = "Admin APIs for order management")
@PreAuthorize("hasRole('ADMIN')")
public class OrderAdminController {
    
    @Operation(summary = "Get orders with advanced filtering",
               description = "Retrieve orders with comprehensive search and filtering options")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderAdminDto>>> getOrdersWithFilters(
            @Valid OrderSearchCriteriaDto criteria,
            @PageableDefault(size = 50) Pageable pageable) {
        // Advanced order search and filtering
        // Full OpenAPI documentation
        // Role-based authorization
    }
    
    @Operation(summary = "Process admin refund",
               description = "Process refund with admin override capabilities")
    @PostMapping("/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundResultDto>> processAdminRefund(
            @PathVariable Long orderId,
            @Valid @RequestBody AdminRefundRequestDto request) {
        // Admin refund processing
        // Audit logging for admin actions
        // Comprehensive error handling
    }
}

@RestController
@RequestMapping("/admin/orders/analytics")
@Tag(name = "Order Analytics", description = "Business intelligence and reporting")
@PreAuthorize("hasRole('ADMIN')")
public class OrderAnalyticsController {
    
    @Operation(summary = "Get analytics overview",
               description = "Comprehensive order and revenue analytics")
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<OrderAnalyticsOverviewDto>> getAnalyticsOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // Analytics dashboard data
        // Performance metrics and KPIs
    }
}
```

### **Advanced Search Implementation**
```java
@Component
public class OrderSearchService {
    
    private final OrderRepository orderRepository;
    private final EntityManager entityManager;
    
    public Page<OrderAdminDto> searchOrders(OrderSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> query = cb.createQuery(Order.class);
        Root<Order> root = query.from(Order.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        // Status filtering
        if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
            predicates.add(root.get("status").in(criteria.getStatuses()));
        }
        
        // Date range filtering
        if (criteria.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getStartDate()));
        }
        if (criteria.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getEndDate()));
        }
        
        // Amount range filtering
        if (criteria.getMinAmount() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), criteria.getMinAmount()));
        }
        if (criteria.getMaxAmount() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), criteria.getMaxAmount()));
        }
        
        // Customer filtering
        if (criteria.getCustomerEmail() != null) {
            predicates.add(cb.or(
                cb.like(cb.lower(root.get("guestEmail")), "%" + criteria.getCustomerEmail().toLowerCase() + "%"),
                cb.like(cb.lower(root.join("user").get("email")), "%" + criteria.getCustomerEmail().toLowerCase() + "%")
            ));
        }
        
        // Product SKU filtering
        if (criteria.getProductSku() != null) {
            Join<Order, OrderItem> itemJoin = root.join("orderItems");
            predicates.add(cb.equal(itemJoin.get("productSku"), criteria.getProductSku()));
        }
        
        // Full-text search
        if (criteria.getSearchTerm() != null) {
            String searchPattern = "%" + criteria.getSearchTerm().toLowerCase() + "%";
            predicates.add(cb.or(
                cb.like(cb.lower(root.get("orderNumber")), searchPattern),
                cb.like(cb.lower(root.get("guestEmail")), searchPattern),
                cb.like(cb.lower(root.join("user", JoinType.LEFT).get("email")), searchPattern)
            ));
        }
        
        query.where(predicates.toArray(new Predicate[0]));
        
        // Apply sorting
        if (pageable.getSort().isSorted()) {
            List<javax.persistence.criteria.Order> orders = new ArrayList<>();
            for (Sort.Order sortOrder : pageable.getSort()) {
                if (sortOrder.isAscending()) {
                    orders.add(cb.asc(root.get(sortOrder.getProperty())));
                } else {
                    orders.add(cb.desc(root.get(sortOrder.getProperty())));
                }
            }
            query.orderBy(orders);
        }
        
        // Execute query with pagination
        TypedQuery<Order> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        List<Order> orders = typedQuery.getResultList();
        
        // Convert to DTOs
        List<OrderAdminDto> adminDtos = orders.stream()
            .map(orderAdminMapper::toAdminDto)
            .collect(toList());
        
        // Get total count for pagination
        long totalCount = countOrdersWithCriteria(criteria);
        
        return new PageImpl<>(adminDtos, pageable, totalCount);
    }
}
```

## 🧪 **Testing Requirements (Implementation-First)**

### **Admin Service Testing**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class OrderAdminServiceTest {
    
    @Autowired
    private OrderAdminService adminService;
    
    @Test
    void getOrdersWithFilters_WithStatusFilter_ShouldReturnFilteredResults() {
        // Create orders with different statuses
        createAndSaveOrderWithStatus(OrderStatus.CONFIRMED);
        createAndSaveOrderWithStatus(OrderStatus.SHIPPED);
        createAndSaveOrderWithStatus(OrderStatus.DELIVERED);
        
        OrderSearchCriteria criteria = OrderSearchCriteria.builder()
            .statuses(List.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED))
            .build();
        
        Page<OrderAdminDto> result = adminService.getOrdersWithFilters(criteria, Pageable.unpaged());
        
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(order -> 
            order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.SHIPPED);
    }
    
    @Test
    void processAdminRefund_WithValidRequest_ShouldProcessRefundAndUpdateOrder() {
        Order order = createAndSaveOrderWithPayment();
        AdminRefundRequest request = AdminRefundRequest.builder()
            .amount(new BigDecimal("50.00"))
            .reason("Admin refund for damaged item")
            .adminNotes("Customer service request #12345")
            .build();
        
        RefundResult result = adminService.processAdminRefund(order.getId(), request);
        
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("50.00"));
        
        // Verify admin notes added
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getAdminNotes()).contains("Customer service request #12345");
    }
    
    // Target: 80%+ coverage for admin services
}
```

### **Analytics Service Testing**
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class OrderAnalyticsServiceTest {
    
    @Autowired
    private OrderAnalyticsService analyticsService;
    
    @Test
    void getAnalyticsOverview_WithDateRange_ShouldReturnCorrectMetrics() {
        // Create test orders across different dates
        createOrdersForAnalytics();
        
        DateRange dateRange = DateRange.builder()
            .startDate(LocalDate.now().minusDays(30))
            .endDate(LocalDate.now())
            .build();
        
        OrderAnalyticsOverview overview = analyticsService.getAnalyticsOverview(dateRange);
        
        assertThat(overview.getTotalOrders()).isPositive();
        assertThat(overview.getTotalRevenue()).isPositive();
        assertThat(overview.getAverageOrderValue()).isPositive();
    }
    
    // Target: 75%+ coverage for analytics logic
}
```

### **Admin Controller Testing**
```java
@WebMvcTest(OrderAdminController.class)
@Import({OrderAdminMapper.class})
class OrderAdminControllerTest {
    
    @MockBean
    private OrderAdminService adminService;
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrdersWithFilters_WithValidCriteria_ShouldReturnFilteredOrders() throws Exception {
        Page<OrderAdminDto> mockPage = createMockOrderPage();
        when(adminService.getOrdersWithFilters(any(), any())).thenReturn(mockPage);
        
        mockMvc.perform(get("/admin/orders")
                .param("status", "CONFIRMED")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }
    
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getOrdersWithFilters_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/admin/orders"))
                .andExpect(status().isForbidden());
    }
    
    // Target: 75%+ coverage for admin controllers
}
```

## ✅ **Acceptance Criteria**

### **Order Management**
- [ ] Advanced order search with multiple filter criteria
- [ ] Bulk order operations for admin efficiency
- [ ] Admin order status override capabilities
- [ ] Complete order timeline with admin notes

### **Financial Management**
- [ ] Admin refund processing with override capabilities
- [ ] Romanian invoice management and regeneration
- [ ] Financial reporting and compliance validation
- [ ] Revenue analytics with multiple breakdowns

### **Customer Service Tools**
- [ ] Customer order history across all channels
- [ ] Problematic order identification and flagging
- [ ] Admin note system for order management
- [ ] Comprehensive order search capabilities

### **Analytics & Reporting**
- [ ] Order analytics dashboard with key metrics
- [ ] Revenue trends and conversion funnel analysis
- [ ] Export functionality for financial reporting
- [ ] Performance benchmarking and insights

## 📚 **Reference Materials**
- **Admin Patterns**: `.cursorrules` - Admin API implementation guidelines
- **Business Logic**: `docs/tickets/order-service/BUSINESS-LOGIC.md`
- **Romanian Legal**: Invoice compliance and financial reporting requirements
- **Analytics Requirements**: Business intelligence and KPI definitions

## 🚀 **Getting Started**
1. **Implement admin services** with comprehensive search and filtering
2. **Create analytics services** for business intelligence
3. **Build admin controllers** with proper authorization
4. **Add invoice management** with Romanian compliance
5. **Test admin workflows** and analytics accuracy
6. **Optimize admin interface** performance and usability

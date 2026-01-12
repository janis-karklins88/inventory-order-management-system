package lv.janis.iom.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity(name = "OrderItem")
@Table(
    name = "order_items",
    indexes = {
        @Index(name = "idx_order_item_product_id", columnList = "product_id"),
        @Index(name = "idx_order_item_order_id", columnList = "order_id")
    }
)
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal priceAtOrderTime;

    protected OrderItem() {
    }

    private OrderItem(Product product, Integer quantity, BigDecimal priceAtOrderTime) {
        this.product = product;
        this.quantity = quantity;
        this.priceAtOrderTime = priceAtOrderTime;
    }

    public static OrderItem createFor(Product product, Integer quantity, BigDecimal priceAtOrderTime) {
        if (product == null) throw new IllegalArgumentException("product required");
        if (quantity == null || quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (priceAtOrderTime == null || priceAtOrderTime.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("priceAtOrderTime must be non-negative");
        return new OrderItem(product, quantity, priceAtOrderTime);
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getPriceAtOrderTime() {
        return priceAtOrderTime;
    }

    void attachTo(CustomerOrder order) {
        this.order = order;
    }

    void detach() {
        this.order = null;
    }

    public BigDecimal getTotalPrice() {
        return priceAtOrderTime.multiply(BigDecimal.valueOf(quantity));
    }

    public void setQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        this.quantity = quantity;
    }
}

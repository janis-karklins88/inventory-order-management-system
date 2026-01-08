package lv.janis.iom.entity;

import jakarta.persistence.*;

@Entity(name = "Inventory")
@Table (
    name = "inventory",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_product_id", columnNames = {"product_id"})
    }
)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
        
    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer reservedQuantity;

    protected Inventory() {
    }

    private Inventory(Product product) {
        this.product = product;
        this.quantity = 0;
        this.reservedQuantity = 0;
    }

    public static Inventory createFor(Product product) {
        if (product == null) throw new IllegalArgumentException("product required");
        return new Inventory(product);
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

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    @Transient
    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    public void increaseQuantity(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        this.quantity += amount;
    }

    public void decreaseQuantity(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (amount > this.quantity - this.reservedQuantity) {
            throw new IllegalArgumentException("not enough available quantity to decrease");
        }
        this.quantity -= amount;
    }

    public void reserveQuantity(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (amount > this.quantity - this.reservedQuantity) {
            throw new IllegalArgumentException("not enough available quantity to reserve");
        }
        this.reservedQuantity += amount;
    }

    public void unreserveQuantity(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (amount > this.reservedQuantity) {
            throw new IllegalArgumentException("not enough reserved quantity to unreserve");
        }
        this.reservedQuantity -= amount;
    }

    public void deductReservedQuantity(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (amount > this.reservedQuantity) {
            throw new IllegalArgumentException("not enough reserved quantity to fulfill");
        }
        this.reservedQuantity -= amount;
        this.quantity -= amount;
    }

}
package lv.janis.iom.service;

import org.springframework.stereotype.Service;

import lv.janis.iom.repository.CustomerOrderRepository;

@Service
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;

    public OrderService(CustomerOrderRepository customerOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
    }
}

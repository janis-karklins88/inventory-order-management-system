package lv.janis.iom.service;

import org.springframework.stereotype.Service;

import lv.janis.iom.repository.InventoryRepository;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }
}

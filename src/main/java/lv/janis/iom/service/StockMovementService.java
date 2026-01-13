package lv.janis.iom.service;

import lv.janis.iom.dto.StockMovementCreationRequest;
import lv.janis.iom.entity.StockMovement;
import lv.janis.iom.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StockMovementService {
    private final StockMovementRepository stockMovementRepository;

    public StockMovementService(StockMovementRepository stockMovementRepository) {
        this.stockMovementRepository = stockMovementRepository;
    }

    public StockMovement createStockMovement(StockMovementCreationRequest request) {
        StockMovement movement = new StockMovement(
            request.getInventory(),
            request.getDelta(),
            request.getReason(),
            request.getOrderId(),
            request.getMovementType()          
        );

        return stockMovementRepository.save(movement);
    }
}

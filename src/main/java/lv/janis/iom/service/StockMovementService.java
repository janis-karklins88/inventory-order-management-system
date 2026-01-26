package lv.janis.iom.service;

import lv.janis.iom.dto.filters.StockMovmentFilter;
import lv.janis.iom.dto.requests.StockMovementCreationRequest;
import lv.janis.iom.dto.response.StockMovementResponse;
import lv.janis.iom.entity.StockMovement;
import lv.janis.iom.repository.StockMovementRepository;
import lv.janis.iom.repository.specification.StockMovementSpecification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

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
                request.getMovementType());

        return stockMovementRepository.save(movement);
    }

    public Page<StockMovementResponse> getStockMovement(StockMovmentFilter filter, @NonNull Pageable pageable) {
        var safeFilter = filter != null ? filter : new StockMovmentFilter();
        var spec = Specification.where(
                StockMovementSpecification.search(
                        safeFilter.getProductId(),
                        safeFilter.getInventoryId(),
                        safeFilter.getOrderId()).and(
                                StockMovementSpecification.createdBetween(
                                        safeFilter.getFrom(),
                                        safeFilter.getTo()))
                        .and(StockMovementSpecification.orderStatusEquals(
                                safeFilter.getMovementType()))
                        .and(StockMovementSpecification.stockMovementDirSpecification(
                                safeFilter.getDirection())));

        return stockMovementRepository.findAll(spec, pageable).map(StockMovementResponse::from);
    }

}

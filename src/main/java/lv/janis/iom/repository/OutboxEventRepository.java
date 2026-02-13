package lv.janis.iom.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import lv.janis.iom.entity.OutboxEvent;
import lv.janis.iom.enums.OutboxEventStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
  @Query("""
      select e.id
      from OutboxEvent e
      where (
           (e.status in :statuses and e.availableAt <= :now and e.attempts < :maxAttempts)
        or (e.status = 'PROCESSING' and e.lockedAt < :staleBefore and e.attempts < :maxAttempts)
      )
      order by e.id
      """)
  List<Long> findCandidateIds(@Param("statuses") Collection<OutboxEventStatus> statuses,
      @Param("now") Instant now,
      @Param("staleBefore") Instant staleBefore,
      @Param("maxAttempts") int maxAttempts,
      Pageable pageable);

  @Transactional
  @Modifying
  @Query("""
      update OutboxEvent e
         set e.status = 'PROCESSING',
             e.lockedAt = :now,
             e.lockedBy = :lockedBy
       where e.id = :id
         and (
              (e.status in ('PENDING','FAILED') and e.availableAt <= :now and e.attempts < :maxAttempts)
           or (e.status = 'PROCESSING' and e.lockedAt < :staleBefore and e.attempts < :maxAttempts)
         )
      """)
  int claim(@Param("id") Long id,
      @Param("now") Instant now,
      @Param("staleBefore") Instant staleBefore,
      @Param("maxAttempts") int maxAttempts,
      @Param("lockedBy") String lockedBy);

}

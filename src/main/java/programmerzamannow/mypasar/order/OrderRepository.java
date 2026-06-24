package programmerzamannow.mypasar.order;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import programmerzamannow.mypasar.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, String> {

    Optional<Order> findOrderById(String id);

    @Modifying
    @Query("DELETE FROM Order o WHERE o.status = :status AND o.expiredAt < :currentTime")
    void deleteByStatusAndExpiredAtLessThan(@Param("status") String status, @Param("currentTime") long currentTime);

    

}

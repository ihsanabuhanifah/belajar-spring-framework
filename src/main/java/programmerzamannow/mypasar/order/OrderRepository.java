package programmerzamannow.mypasar.order;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import programmerzamannow.mypasar.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, String> {

    Optional<Order> findOrderById(String id);

}

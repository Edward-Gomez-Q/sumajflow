package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ucb.edu.bo.sumajflow.entity.ReporteQuimico;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReporteQuimicoRepository extends JpaRepository<ReporteQuimico, Integer> {

}
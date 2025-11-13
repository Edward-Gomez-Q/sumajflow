package ucb.edu.bo.sumajflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ucb.edu.bo.sumajflow.entity.Usuarios;


public interface UsuariosRepository extends JpaRepository<Usuarios, Integer> {
}
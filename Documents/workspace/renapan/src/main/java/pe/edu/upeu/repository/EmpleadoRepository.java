package pe.edu.upeu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.edu.upeu.entity.Empleado;
@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long>{

}
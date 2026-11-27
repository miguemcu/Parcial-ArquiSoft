package com.udea.parcial_equipo_4.repository;

import com.udea.parcial_equipo_4.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByDocumento(String documento);

    boolean existsByDocumento(String documento);
}

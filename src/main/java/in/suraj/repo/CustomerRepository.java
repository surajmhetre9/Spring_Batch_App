package in.suraj.repo;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import in.suraj.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Serializable> {

}
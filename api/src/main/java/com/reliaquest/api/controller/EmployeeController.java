package com.reliaquest.api.controller;

import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.EmployeeService;

import java.util.List;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employee")
public class EmployeeController implements IEmployeeController<Employee, CreateEmployeeRequest> {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);
    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    /**
     * @return A list of all employees, or an empty list if none are found or an error occurs.
     */
    @Override
    public ResponseEntity<List<Employee>> getAllEmployees() {
        log.info("Controller: GET /employees");
        return ResponseEntity.ok(service.getAllEmployees());
    }

    /**
     * @param searchString
     * @return A list of all employees matching name, or an empty list if none are found or an error occurs.
     */
    @Override
    public ResponseEntity<List<Employee>> getEmployeesByNameSearch(@PathVariable String searchString) {
        log.info("Controller: GET /employees/search/{}", searchString);
        return ResponseEntity.ok(service.searchByName(searchString));
    }

    /**
     * @param id
     * @return A employees object matching ID, or null if none are found or an error occurs.
     */
    @Override
    public ResponseEntity<Employee> getEmployeeById(@PathVariable String id) {
        log.info("Controller: GET /employees/{}", id);
        var e = service.getById(id);
        return e == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(e);
    }

    /**
     * @return highest salary among all employees
     */
    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        log.info("Controller: GET /employees/highestSalary");
        return ResponseEntity.ok(service.getHighestSalaryOfEmployees());
    }

    /**
     * @return top 10 employees with highest salary.
     */
    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        log.info("Controller: GET /employees/topTenHighestEarningEmployeeNames");
        return ResponseEntity.ok(service.top10NamesBySalary());
    }

    /**
     * @param employeeInput
     * @return newly created employee along with its ID and EMAIL.
     */
    @Override
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody CreateEmployeeRequest employeeInput) {
        log.info("Controller: POST /employees name={}", employeeInput.getName());
        return ResponseEntity.ok(service.create(employeeInput));
    }

    /**
     * @param id
     * @return The name of the deleted employee, if the deletion was successful.
     * @throws IllegalArgumentException if no employee is found for the given ID.
     * @throws IllegalStateException    if the employee could not be deleted on the remote API.
     */
    @Override
    public ResponseEntity<String> deleteEmployeeById(@PathVariable String id) {
        log.info("Controller: DELETE /employees/{}", id);
        return ResponseEntity.ok(service.deleteById(id));
    }
}

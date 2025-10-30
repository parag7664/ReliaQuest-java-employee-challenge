package com.reliaquest.api.service;

import com.reliaquest.api.client.EmployeeApiClient;
import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.Employee;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);
    private final EmployeeApiClient client;

    public EmployeeService(EmployeeApiClient client) {
        this.client = client;
    }

    public List<Employee> getAllEmployees() {
        log.info("Service: getAllEmployees()");
        return client.getAllEmployees();
    }

    public List<Employee> searchByName(String fragment) {
        log.info("Service: search employees by name contains='{}'", fragment);
        String f = fragment == null ? "" : fragment.toLowerCase(Locale.ROOT);
        List<Employee> filtered = client.getAllEmployees().stream()
                .filter(e -> e.getName() != null
                        && e.getName().toLowerCase(Locale.ROOT).contains(f))
                .toList();
        log.debug("Search fragment='{}' -> {} matches", fragment, filtered.size());
        return filtered;
    }

    public Employee getById(String id) {
        log.info("Service: getEmployeeById id={}", id);
        return client.getById(id);
    }

    public Integer getHighestSalaryOfEmployees() {
        log.info("Service: highestSalary()");
        Integer max = client.getAllEmployees().stream()
                .map(Employee::getSalary)
                .filter(s -> s != null)
                .max(Integer::compareTo)
                .orElse(0);
        log.debug("Highest salary computed={}", max);
        return max;
    }

    public List<String> top10NamesBySalary() {
        log.info("Service: top10NamesBySalary()");
        List<String> names = client.getAllEmployees().stream()
                .sorted(Comparator.comparing(Employee::getSalary, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .limit(10)
                .map(Employee::getName)
                .toList();

        log.debug("Top10 names computed size={} top={}", names.size(), names.isEmpty() ? "(none)" : names.get(0));
        return names;
    }

    public Employee create(CreateEmployeeRequest input) {
        log.info("Service: createEmployee name={}", input.getName());
        return client.create(input);
    }

    /**
     * Delete by id → resolve name → delete by name (mock quirk).
     */
    public String deleteById(String id) {
        log.info("Service: deleteEmployeeById id={}", id);
        Employee e = client.getById(id);
        if (e == null || e.getName() == null) {
            log.warn("Delete aborted: id={} not found", id);
            throw new IllegalArgumentException("Employee not found for id=" + id);
        }
        boolean ok = client.deleteByName(e.getName());
        if (!ok) {
            log.warn("Delete failed: id={} name={}", id, e.getName());
            throw new IllegalStateException("Failed to delete employee name=" + e.getName());
        }
        log.info("Deleted id={} name={}", id, e.getName());
        return e.getName();
    }
}

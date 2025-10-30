package com.reliaquest.api.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.reliaquest.api.client.EmployeeApiClient;
import com.reliaquest.api.model.Employee;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EmployeeServiceTest {

    private EmployeeApiClient client;
    private EmployeeService service;

    @BeforeEach
    void setUp() {
        client = mock(EmployeeApiClient.class); // create mock manually
        service = new EmployeeService(client); // inject mock manually
    }

    @Test
    void search_returnsMatches() {
        List<Employee> list = List.of(
                new Employee("1", "Landon Barrows", 100, 30, "mr", "landon@gmail.com"),
                new Employee("2", "Bob", 120, 25, "mr", "bob@gmail.com"));
        when(client.getAllEmployees()).thenReturn(list);
        List<Employee> res = service.searchByName("Landon");
        assertEquals(1, res.size());
        assertEquals("Landon Barrows", res.get(0).getName());
    }


    @Test
    void getHighestSalary_returnsMax() {
        when(client.getAllEmployees())
                .thenReturn(List.of(
                        new Employee("1", "A", 100, 30, "T", "a@b.com"),
                        new Employee("2", "B", 320800, 61, "T2", "b@a.com")));
        assertThat(service.getHighestSalaryOfEmployees()).isEqualTo(320800);
    }

    @Test
    void getTop10() {
        when(client.getAllEmployees())
                .thenReturn(List.of(
                        new Employee("1", "X", 10, 20, "", ""),
                        new Employee("2", "Y", 30, 20, "", ""),
                        new Employee("3", "Z", 20, 20, "", "")));
        assertThat(service.top10NamesBySalary()).containsExactly("Y", "Z", "X");
    }

    @Test
    void deleteByUserName() {

        when(client.getById("id-123"))
                .thenReturn(
                        new Employee("id-123", "Bill Bob", 89750, 24, "Documentation Engineer", "billBob@company.com"));
        when(client.deleteByName("Bill Bob")).thenReturn(true);

        String result = service.deleteById("id-123");
        assertThat(result).isEqualTo("Bill Bob");
        verify(client).getById("id-123");
        verify(client).deleteByName("Bill Bob");
    }

}

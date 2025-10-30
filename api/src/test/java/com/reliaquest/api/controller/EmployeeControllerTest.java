package com.reliaquest.api.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.EmployeeService;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeController.class)
public class EmployeeControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private EmployeeService service;

    @Test
    void getAllEmployees_ok() throws Exception {
        Mockito.when(service.getAllEmployees())
                .thenReturn(List.of(
                        new Employee("1", "A", 100, 30, "T", "a@x.com"),
                        new Employee("2", "B", 200, 40, "T2", "b@x.com")));

        mvc.perform(get("/api/v1/employee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].employee_name", is("A")));
    }

    @Test
    void search_ok() throws Exception {
        Mockito.when(service.searchByName("ti"))
                .thenReturn(List.of(new Employee("1", "Tiger Nixon", 320800, 61, "Vice Chair", "t@x.com")));

        mvc.perform(get("/api/v1/employee/search/ti"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employee_name", containsString("Tiger")));
    }

    @Test
    void getById_found() throws Exception {
        Mockito.when(service.getById("abc")).thenReturn(new Employee("abc", "Alex", 100, 20, "Dev", "a@x.com"));
        mvc.perform(get("/api/v1/employee/abc")).andExpect(status().isOk()).andExpect(jsonPath("$.id", is("abc")));
    }

    @Test
    void getById_notFound() throws Exception {
        Mockito.when(service.getById("nope")).thenReturn(null);
        mvc.perform(get("/api/v1/employee/nope")).andExpect(status().isNotFound());
    }

    @Test
    void highestSalary_ok() throws Exception {
        Mockito.when(service.getHighestSalaryOfEmployees()).thenReturn(320800);
        mvc.perform(get("/api/v1/employee/highestSalary"))
                .andExpect(status().isOk())
                .andExpect(content().string("320800"));
    }

    @Test
    void top10_ok() throws Exception {
        Mockito.when(service.top10NamesBySalary()).thenReturn(List.of("A", "B"));
        mvc.perform(get("/api/v1/employee/topTenHighestEarningEmployeeNames"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]", is("A")));
    }

    @Test
    void create_ok() throws Exception {
        var created = new Employee("id1", "Jill Jenkins", 139082, 48, "Financial Advisor", "jillj@company.com");
        Mockito.when(service.create(Mockito.any())).thenReturn(created);

        String body = """
                  {"name":"Jill Jenkins","salary":139082,"age":48,"title":"Financial Advisor"}
                """;

        mvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee_name", is("Jill Jenkins")))
                .andExpect(jsonPath("$.employee_email", is("jillj@company.com")));
    }

    @Test
    void delete_ok() throws Exception {
        Mockito.when(service.deleteById("5255")).thenReturn("Bill Bob");
        mvc.perform(delete("/api/v1/employee/5255"))
                .andExpect(status().isOk())
                .andExpect(content().string("Bill Bob"));
    }
}

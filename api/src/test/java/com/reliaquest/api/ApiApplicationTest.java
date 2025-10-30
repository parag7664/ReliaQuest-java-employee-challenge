package com.reliaquest.api;

import com.reliaquest.api.client.EmployeeApiClient;
import com.reliaquest.api.model.CreateEmployeeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.model.Employee;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiApplicationTest {


    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate http;

    @Autowired
    ObjectMapper om;

    @MockBean
    EmployeeApiClient client;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void getAllEmployees_returnsList() {
        Mockito.when(client.getAllEmployees()).thenReturn(List.of(
                new Employee("1", "Tiger Nixon", 320800, 61, "Vice Chair", "tnixon@company.com"),
                new Employee("2", "Garrett Winters", 170750, 63, "Director", "gwinters@company.com")
        ));

        ResponseEntity<Employee[]> resp = http.getForEntity(url("/api/v1/employee"), Employee[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(2);
        assertThat(resp.getBody()[0].getName()).isEqualTo("Tiger Nixon");
    }

    @Test
    void getEmployeesByNameSearch_filtersCaseInsensitive() {
        Mockito.when(client.getAllEmployees()).thenReturn(List.of(
                new Employee("1", "Tiger Nixon", 320800, 61, "Vice Chair", "tnixon@company.com"),
                new Employee("2", "Garrett Winters", 170750, 63, "Director", "gwinters@company.com")
        ));

        ResponseEntity<Employee[]> resp =
                http.getForEntity(url("/api/v1/employee/search/ti"), Employee[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).extracting(Employee::getName)
                .containsExactly("Tiger Nixon");
    }

    @Test
    void getEmployeeById_found_and_notFound() {
        Mockito.when(client.getById("abc")).thenReturn(
                new Employee("abc", "Alex", 100000, 30, "Dev", "alex@x.com")
        );
        Mockito.when(client.getById("zzz")).thenReturn(null);

        // found
        ResponseEntity<Employee> ok =
                http.getForEntity(url("/api/v1/employee/abc"), Employee.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody().getId()).isEqualTo("abc");

        // not found
        ResponseEntity<String> nf =
                http.getForEntity(url("/api/v1/employee/zzz"), String.class);
        assertThat(nf.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getHighestSalaryOfEmployees_returnsMax() {
        Mockito.when(client.getAllEmployees()).thenReturn(List.of(
                new Employee("1", "A", 100, 20, "", ""),
                new Employee("2", "B", 320800, 61, "", "")
        ));

        ResponseEntity<Integer> resp =
                http.getForEntity(url("/api/v1/employee/highestSalary"), Integer.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(320800);
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_sortedAndLimited() {
        Mockito.when(client.getAllEmployees()).thenReturn(List.of(
                new Employee("1", "Low", 10, 20, "", ""),
                new Employee("2", "Mid", 20, 20, "", ""),
                new Employee("3", "High", 30, 20, "", "")
        ));

        ResponseEntity<String[]> resp =
                http.getForEntity(url("/api/v1/employee/topTenHighestEarningEmployeeNames"), String[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsExactly("High", "Mid", "Low");
    }

    @Test
    void createEmployee_validBody_returnsCreated() {
        CreateEmployeeRequest input = new CreateEmployeeRequest("Jill Jenkins", 139082, 48, "Financial Advisor");
        Employee created = new Employee("id-1", "Jill Jenkins", 139082, 48, "Financial Advisor", "jillj@company.com");

        Mockito.when(client.create(any(CreateEmployeeRequest.class))).thenReturn(created);

        ResponseEntity<Employee> resp =
                http.postForEntity(url("/api/v1/employee"), input, Employee.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getEmail()).isEqualTo("jillj@company.com");
    }

    @Test
    void createEmployee_invalidBody_returns400() {
        // missing name & title, salary 0 (invalid), age 15 (invalid)
        Map<String, Object> bad = Map.of(
                "salary", 0,
                "age", 15
        );

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(bad, h);

        ResponseEntity<String> resp =
                http.postForEntity(url("/api/v1/employee"), req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deleteEmployeeById_happyPath_resolvesNameThenDeletes() {
        Employee e = new Employee("id-123", "Bill Bob", 89750, 24, "Documentation Engineer", "bill@x.com");
        Mockito.when(client.getById("id-123")).thenReturn(e);
        Mockito.when(client.deleteByName("Bill Bob")).thenReturn(true);

        ResponseEntity<String> resp =
                http.exchange(url("/api/v1/employee/id-123"), HttpMethod.DELETE, null, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo("Bill Bob");

        Mockito.verify(client).getById("id-123");
        Mockito.verify(client).deleteByName("Bill Bob");
    }

    @Test
    void deleteEmployeeById_notFound_returns404() {
        Mockito.when(client.getById("nope")).thenReturn(null);

        ResponseEntity<String> resp =
                http.exchange(url("/api/v1/employee/nope"), HttpMethod.DELETE, null, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteEmployeeById_deleteFails_returns409() {
        Employee e = new Employee("x", "Cant Delete", 1, 20, "", "");
        Mockito.when(client.getById("x")).thenReturn(e);
        Mockito.when(client.deleteByName("Cant Delete")).thenReturn(false);

        ResponseEntity<String> resp =
                http.exchange(url("/api/v1/employee/x"), HttpMethod.DELETE, null, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}

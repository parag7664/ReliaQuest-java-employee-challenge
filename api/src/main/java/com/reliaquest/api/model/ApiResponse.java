package com.reliaquest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.reliaquest.api.client.EmployeeApiClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic wrapper for API responses from the Mock Employee API (or other APIs that follow
 * the same response envelope pattern), Used in EmployeeApiClient (wrapper around WebClient)
 * <p>
 * The Mock Employee API returns all results wrapped in a JSON object containing:
 * <ul>
 *   <li>{@code data} – the actual payload, which can be a single object, a list, or a primitive
 *       type (e.g., {@code Boolean}, {@code Integer}).</li>
 *   <li>{@code status} – a human-readable status message, typically indicating success.</li>
 *   <li>{@code error} – an optional error message if the request failed.</li>
 * </ul>
 *
 * <p>Example of a single employee response:
 * <pre>
 * {
 *   "data": {
 *     "id": "8c0b46c2-da59-40f7-92c6-91967942007a",
 *     "employee_name": "Fred Hamill",
 *     "employee_salary": 66661,
 *     "employee_age": 26,
 *     "employee_title": "Product Producer",
 *     "employee_email": "zontrax@company.com"
 *   },
 *   "status": "200 OK"
 * }
 * </pre>
 *
 * @param <T> the type of the {@code data} field, e.g., {@code Employee}, {@code List<Employee>}, {@code Boolean}, etc.
 * @author Parag Soni
 * @see EmployeeApiClient
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private T data;
    private String status;

    @JsonProperty("error")
    private String error;
}

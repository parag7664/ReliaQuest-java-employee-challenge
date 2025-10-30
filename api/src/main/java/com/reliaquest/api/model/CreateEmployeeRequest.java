package com.reliaquest.api.model;

import com.reliaquest.api.constants.EmployeeConstraints;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

/**
 * Request Data Transfer Object (DTO) representing the payload required to create a new
 * {@link com.reliaquest.api.model.Employee} via the API.
 * <p>
 * This object is used as the request body in
 * {@link com.reliaquest.api.controller.IEmployeeController#createEmployee(Object)}
 * and contains only the fields that a client must provide when creating a new employee.
 * Bean Validation annotations are used to enforce input constraints at the API boundary.
 *
 * <p>Validation constraints:
 * <ul>
 *   <li>{@code name} – must not be blank</li>
 *   <li>{@code salary} – must be a positive integer</li>
 *   <li>{@code age} – must be between 16 and 75 (inclusive)</li>
 *   <li>{@code title} – must not be blank</li>
 * </ul>
 * <p>
 * <p>
 * Usage example with {@link org.springframework.web.reactive.function.client.WebClient}:
 * <pre>
 * Employee create(CreateEmployeeRequest req)
 * </pre>
 *
 * @param name   the employee's full name (must not be blank)
 * @param salary the employee's salary (must be greater than zero)
 * @param age    the employee's age (must be between 16 and 75)
 * @param title  the employee's job title (must not be blank)
 * @author Parag Soni
 * @see com.reliaquest.api.model.Employee
 * @see com.reliaquest.api.model.ApiResponse
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Validated
public class CreateEmployeeRequest {

    @NotBlank
    private String name;

    @NotNull
    @Min(EmployeeConstraints.MIN_SALARY)
    private Integer salary;

    @NotNull
    @Min(EmployeeConstraints.MIN_AGE)
    @Max(EmployeeConstraints.MAX_AGE)
    private Integer age;

    @NotBlank
    private String title;
}

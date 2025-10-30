package com.reliaquest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing an employee record within the system.
 * <p>
 * This class is used internally to represent employee data returned from
 * the Mock Employee API or persisted within the application. It is also
 * used in DTO mappings to {@link com.reliaquest.api.model.ApiResponse}.
 *
 * @param id     the unique identifier of the employee
 * @param name   the full name of the employee (mapped from {@code employee_name})
 * @param salary the salary of the employee (mapped from {@code employee_salary})
 * @param age    the age of the employee (mapped from {@code employee_age})
 * @param title  the job title of the employee (mapped from {@code employee_title})
 * @param email  the email address of the employee (mapped from {@code employee_email})
 * @author Parag Soni
 * @see com.reliaquest.api.model.CreateEmployeeRequest
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Employee {
    private String id;

    @JsonProperty("employee_name")
    private String name;

    @JsonProperty("employee_salary")
    private Integer salary;

    @JsonProperty("employee_age")
    private Integer age;

    @JsonProperty("employee_title")
    private String title;

    @JsonProperty("employee_email")
    private String email;
}

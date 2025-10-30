package com.reliaquest.api.constants;

/**
 * Constraints for {@link com.reliaquest.api.model.CreateEmployeeRequest}
 * Alternatively, the constraints could be specified in a configuration class
 * then added to application.yml. A custom annotation could be used to enforce
 * along with a custom validator else we could directly populate this annotations as @Min(16)
 *
 */
public final class EmployeeConstraints {
    public static final int MIN_AGE = 16;
    public static final int MAX_AGE = 75;
    public static final int MIN_SALARY = 1;

    private EmployeeConstraints() {
    }
}

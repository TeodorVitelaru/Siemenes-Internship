package com.siemens.internship.exceptions;

import java.util.Date;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Class representing validation error details to be returned in API responses.
 */
@Getter
@AllArgsConstructor
public class ValidationErrorResponse {
    private Date timestamp;
    private String message;
    private Map<String, String> errors;
}
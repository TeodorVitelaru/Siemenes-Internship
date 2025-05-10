package com.siemens.internship.exceptions;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Class representing error details to be returned in API responses.
 */
@Getter
@AllArgsConstructor
public class ErrorDetails {
    private Date timestamp;
    private String message;
    private String details;
}
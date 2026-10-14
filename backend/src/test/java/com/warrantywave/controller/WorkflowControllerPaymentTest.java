package com.warrantywave.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class WorkflowControllerPaymentTest {
    @Test
    void doesNotRecordPaymentWithoutConfiguredProvider() {
        WorkflowController controller = new WorkflowController(null, null, null, null, null, null,
            null, null, null, null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> controller.payInstallment(null, 1L));

        assertEquals(HttpStatus.NOT_IMPLEMENTED, exception.getStatusCode());
    }
}
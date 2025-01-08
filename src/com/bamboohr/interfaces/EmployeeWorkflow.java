package com.bamboohr.interfaces.employee_workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.Map;

@WorkflowInterface
public interface EmployeeWorkflow {
    @WorkflowMethod
    Map<String, Object> execute(String args);
}

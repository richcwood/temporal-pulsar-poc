package com.bamboohr.interfaces.department_workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.Map;

@WorkflowInterface
public interface DepartmentWorkflow {
    @WorkflowMethod
    Map<String, Object> execute(String args);
}
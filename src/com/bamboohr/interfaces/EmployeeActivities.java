package com.bamboohr.interfaces.employee_activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import java.util.Map;

@ActivityInterface
public interface EmployeeActivities {
    @ActivityMethod
    String employeeActivity1(String args);
    @ActivityMethod
    String employeeActivity2(String args);
    @ActivityMethod
    String employeeActivity3(String args);
}
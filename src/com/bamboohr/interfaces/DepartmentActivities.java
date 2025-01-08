package com.bamboohr.interfaces.department_activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import java.util.Map;

@ActivityInterface
public interface DepartmentActivities {
    @ActivityMethod
    String departmentActivity1(String args);
}
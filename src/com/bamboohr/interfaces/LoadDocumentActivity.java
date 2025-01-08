package com.bamboohr.interfaces.load_document_activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface LoadDocumentActivity {
    @ActivityMethod(name = "load-document")
    String loadDocument(String args);
} 
package com.example.studymate.docs;

import java.io.File;

/** Strategy interface for turning a document file into a displayable model. */
public interface DocumentParser<R> {
    R parse(File file) throws Exception;
}

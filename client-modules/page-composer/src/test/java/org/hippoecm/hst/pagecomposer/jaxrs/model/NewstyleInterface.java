package org.hippoecm.hst.pagecomposer.jaxrs.model;

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.ImageSetPath;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

import java.util.Date;

interface NewstyleInterface {
    @Parameter(name = "00-color", defaultValue = "blue")
    @Color
    String getColor();

    @Parameter(name = "01-documentLocation")
    @DocumentLink(docLocation = "/content", docType = "hst:testdocument")
    String getDocumentLocation();

    @Parameter(name = "02-image", defaultValue = "/content/gallery/default.png")
    @ImageSetPath
    String getImage();

    @Parameter(name = "03-date")
    Date getDate();

    @Parameter(name = "04-boolean")
    boolean isBoolean();

    @Parameter(name = "05-booleanClass")
    Boolean isBooleanClass();

    @Parameter(name = "06-int")
    int getInt();

    @Parameter(name = "07-integerClass")
    Integer getIntegerClass();

    @Parameter(name = "08-long")
    long getLong();

    @Parameter(name = "09-longClass")
    Long getLongClass();

    @Parameter(name = "10-short")
    short getShort();

    @Parameter(name = "11-shortClass")
    Short getShortClass();

    @Parameter(name = "12-comboBox")
    @DropDownList(value = {"value1", "value2", "value3"})
    String getDropDownValue();

    @Parameter(name = "13-linkpicker")
    @JcrPath(isRelative = true, pickerConfiguration = "cms-pickers/documents")
    String getDocumentPath();
}
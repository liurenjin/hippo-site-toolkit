package org.hippoecm.hst.demo.components;

import org.hippoecm.hst.configuration.components.Parameter;

public interface BlockInfo {
    @Parameter(name = "bgcolor", defaultValue="", displayName = "Background Color")
    String getBgColor();

}

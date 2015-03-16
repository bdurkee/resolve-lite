/**
 * ResolveCompilerListener.java
 * ---------------------------------
 * Copyright (c) 2014
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package resolvelite.compiler;

public interface ResolveCompilerListener {

    public void info(String msg);

    public void error(ResolveMessage msg);

    public void warning(ResolveMessage msg);
}

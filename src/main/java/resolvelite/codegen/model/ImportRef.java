/*
 * [The "BSD license"]
 * Copyright (c) 2015 Clemson University
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * 3. The name of the author may not be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package resolvelite.codegen.model;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImportRef extends OutputModelObject {

    public List<String> segs = new ArrayList<String>();

    public ImportRef(String fileName) {
        this.segs = listifyFileString(fileName);
        this.segs.remove(segs.size() - 1); //prune last dir off.
    }

    public ImportRef(String... s) {
        this(Arrays.asList(s));
    }

    public ImportRef(List<String> segs) {
        this.segs = segs;
    }

    @Override
    public boolean equals(Object o) {
        boolean result = (o instanceof ImportRef);
        if (result) {
            String oAsStr = "";
            String thisAsStr = "";
            for (String s : segs) {
                thisAsStr += s;
            }
            for (String s : ((ImportRef) o).segs) {
                oAsStr += s;
            }
            result = oAsStr.equals(thisAsStr);
        }
        return result;
    }

    public int hashCode() {
        String thisAsStr = "";
        for (String s : segs) {
            thisAsStr += s;
        }
        return thisAsStr.hashCode();
    }

    public static List<String> listifyFileString(String fileName) {
        boolean foundStart = false;
        List<String> result = new ArrayList<String>();
        File f = new File(fileName);
        if (!f.exists()) {
            throw new IllegalArgumentException("the directory doesn't exist");
        }
        for (Path p : f.toPath()) {
            if (p.toString().equals("RESOLVE")) {
                foundStart = true;
            }
            if (foundStart) {
                result.add(p.toString());
            }
        }
        return result;
    }
}

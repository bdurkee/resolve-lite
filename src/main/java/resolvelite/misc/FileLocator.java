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
package resolvelite.misc;

import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Traverses a tree of directories. Each file encountered is reported via the
 * <code>visitFile</code> method and each directory via an optional
 * <code>visitDirectory</code> method (override others as needed).
 */
public class FileLocator extends SimpleFileVisitor<Path> {

    private final PathMatcher myMatcher;
    private String myPattern = null;

    private List<File> myMatches = new ArrayList<File>();

    /**
     * Constructs a new <code>FileLocator</code> that will match based on the
     * <code>pattern</code> (or, name) and <code>extension</code> pair provided.
     * 
     * @param pattern An <em>extensionless</em> pattern.
     * @param extensions An list of valid extensions to choose from after a
     *        pattern is matched (i.e. ["java", "cpp", "groovy"]).
     */
    public FileLocator(String pattern, List<String> extensions) {
        myPattern = pattern;

        myMatcher =
                FileSystems.getDefault().getPathMatcher(
                        "glob:" + pattern + parseExtensions(extensions));
    }

    public FileLocator(String extension) {
        myMatcher =
                FileSystems.getDefault().getPathMatcher(
                        "glob:*.{" + extension + "}");
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        Path name = file.getFileName();
        if (name != null && myMatcher.matches(name)) {
            myMatches.add(file.toFile());
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * Returns a single file matching <code>myPattern</code>.
     * 
     * @throws java.nio.file.NoSuchFileException If a file matching
     *         <code>myPattern</code> could not be found.
     * 
     * @return The matching file.
     */
    public File getFile() throws IOException {
        if (myMatches.size() == 0) {
            throw new NoSuchFileException("File matching name '" + myPattern
                    + "' could not be found.");
        }
        return myMatches.get(0);
    }

    public List<File> getFiles() {
        return myMatches;
    }

    private String parseExtensions(List<String> extensions) {
        ST result = new ST("*.{<exts; separator={,}>}");
        return result.add("exts", extensions).render();
    }
}
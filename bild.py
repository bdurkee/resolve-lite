#!/usr/bin/env python

# bootstrap by downloading bilder.py if not found
import urllib
import os
import glob

# ASSUMES YOU HAVE GNU indent installed. ($ brew install gnu-indent on mac os x)
# Executable is gindent
# http://www.gnu.org/software/indent/manual/

if not os.path.exists("bilder.py"):
    print "bootstrapping; downloading bilder.py"
    urllib.urlretrieve(
        "https://raw.githubusercontent.com/parrt/bild/master/src/python/bilder.py",
        "bilder.py")

# assumes bilder.py is in current directory
from bilder import *

VERSION = "0.0.1"

def parser():
    antlr4(srcdir="compiler/src/edu/clemson/resolve/parser", trgdir="gen",
           package="edu.clemson.resolve.parser",
           version="4.5",
           args=["-visitor"])

def compile():
    require(parser)
    #require(regen_tests)
    cp = uniformpath("out") + os.pathsep + \
            os.path.join(JARCACHE, "antlr-4.5-complete.jar") + os.pathsep
    srcpath = ["gen", "compiler/src"]
    args = ["-Xlint", "-Xlint:-serial", "-g", "-sourcepath", string.join(srcpath, os.pathsep)]
    for sp in srcpath:
        javac(sp, "out", version="1.8", cp=cp, args=args)

    junit_jar, hamcrest_jar = load_junitjars()
    cp += os.pathsep + uniformpath("out") \
         + os.pathsep + junit_jar \
         + os.pathsep + hamcrest_jar
    javac("./compiler/src", "out", version="1.8", cp=cp, args=args)

def mkjar():
    require(compile)
    mkdir("dist")
    jarfile = "dist/resolve"+VERSION+".jar"
    manifest = \
        "Main-Class: edu.clemson.resolve.compiler.ResolveCompiler\n" + \
        "Implementation-Title: Resolve lite compiler\n" + \
        "Implementation-Vendor-Id: edu.clemson\n" + \
        "Built-By: %s\n" + \
        "Build-Jdk: 1.8\n" + \
        "Created-By: http://www.bildtool.org\n" + \
        "\n"
    manifest = manifest % os.getlogin()
    download("http://www.antlr.org/download/antlr-4.5-complete.jar", JARCACHE)
    unjar(os.path.join(JARCACHE, "antlr-4.5-complete.jar"), trgdir="out")
    copytree(src="compiler/resources", trg="out")  # messages, Java code gen, etc...
    jar(jarfile, srcdir="out", manifest=manifest)
    print_and_log("Generated " + jarfile)

def test(jfile):
    log("TEST "+jfile)

def tests():
    require(compile)
    for file in glob.glob("tests/cs652/j/*.j"):
        test(file)

def clean():
    rmdir("out")
    rmdir("gen")

def all():
    tests()

processargs(globals())

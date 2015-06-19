#!/usr/bin/env python

# bootstrap by downloading bilder.py if not found
import urllib
import os
import glob

if not os.path.exists("bilder.py"):
    print "bootstrapping; downloading bilder.py"
    urllib.urlretrieve(
        "https://raw.githubusercontent.com/parrt/bild/master/src/python/bilder.py",
        "bilder.py")

# assumes bilder.py is in current directory
from bilder import *

VERSION = "0.0.1"

def parser():
    antlr4(srcdir="src/edu/clemson/resolve/parser", trgdir="gen",
           package="edu.clemson.resolve",
           version="4.5",
           args=["-visitor", "-listener"])

def compile():
    require(parser)
    cp = "src:gen:out:resources:"\
         +JARCACHE+"/antlr-4.5-complete.jar:"+\
         JARCACHE+"/jgrapht-core-0.9.0.jar"
    javac("src", "out", javacVersion="1.8", cp=cp)
    #now for the 'test' folder
    junit_jar, hamcrest_jar = load_junitjars()
    cp += os.pathsep + uniformpath("out") \
      + os.pathsep + junit_jar \
      + os.pathsep + hamcrest_jar
    javac("test", "out/test", version="1.8", cp=cp)

def mkjar():
    rmdir("out")
    require(compile)
    mkdir("dist")
    jarfile = "dist/resolve_"+VERSION+".jar"
    manifest = \
        "Main-Class: edu.clemson.resolve.compiler.ResolveCompiler\n" +\
        "Implementation-Title: RESOLVE compiler\n" +\
        "Implementation-Vendor-Id: edu.clemson\n" +\
        "Built-By: %s\n" +\
        "Build-Jdk: 1.8\n" +\
        "Created-By: http://www.bildtool.org\n" +\
        "\n"
    manifest = manifest % os.getlogin()
    download("http://www.antlr.org/download/antlr-4.5-complete.jar", JARCACHE)
    unjar(os.path.join(JARCACHE, "antlr-4.5-complete.jar"), trgdir="out")
    download("http://central.maven.org/maven2/org/jgrapht/jgrapht-core/0.9.0/jgrapht-core-0.9.0.jar", JARCACHE)
    unjar(os.path.join(JARCACHE, "jgrapht-core-0.9.0.jar"), trgdir="out")
    copytree(src="resources", trg="out")  # messages, Java code gen, etc...
    jar(jarfile, srcdir="out", manifest=manifest)
    print_and_log("Generated " + jarfile)

def tests():
    require(mkjar)
    print_and_log("Testing ...")
    try:
        test()
        print "tests complete"
    except Exception as e:
        print "tests failed: "+e

def test():
    junit_jar, hamcrest_jar = load_junitjars()
    junit("out/test", cp='/Users/daniel/resolve-lite/out/test:'+uniformpath("dist/resolve_"+VERSION+".jar"), verbose=False)

def clean():
    os.remove("bild.log")
    rmdir("out")
    rmdir("gen")

def clean_full():
    rmdir("dist")
    os.remove("bild.log")
    rmdir("out")
    rmdir("gen")

def all():
    tests()

processargs(globals())

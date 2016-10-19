JDOC = javadoc
JDOCFLAGS = -windowtitle $(DOCTITLE) -doctitle $(DOCTITLE) -subpackages SSHelpers -version -author
JFLAGS = -Xlint:deprecation
JC = javac
DOCTITLE = 'SS Proxy Cache API Specification'

.PHONY: *.java default clean docs

default: *.class

%.class: %.java
	$(JC) $(JFLAGS) $<

clean:
	$(RM) *.class

docs: *.java
	$(JDOC) $^ -d docs $(JDOCFLAGS)

docs-clean:
	$(RM) docs/*

run:
	java ProxyCache 8031

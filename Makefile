###############################################################
# Makefile for mspsim
#
# Needed stuff in the PATH:
#  java, javac (JDK 1.5 or newer)
#
# Under MS-DOS/Windows
#  A GNU compatible Make (for example Cygwin's)
###############################################################

###############################################################
# Settings
###############################################################
CC=javac
JAVACC=javacc
JAR=jar
RM=rm -f
CP=cp

###############################################################
# System dependent
###############################################################

ifndef WINDIR
  ifdef OS
    ifneq (,$(findstring Windows,$(OS)))
      WINDIR := Windows
    endif
  endif
endif

ifndef WINDIR
  # This settings are for UNIX
  SEPARATOR=:
  # Add "'" around filenames when removing them because UNIX expands "$"
  APO='#'  (last apostrophe to avoid incorrect font-lock)
else
  # These setting are for Windows
  SEPARATOR=;
  APO=
endif


###############################################################
# Arguments
###############################################################

CCARGS=-deprecation -classpath .


###############################################################
# SERVER OBJECTS
###############################################################

FIRMWAREFILE = blinker2.ihex

CPUTEST := tests/cputest.firmware

PACKAGES := ${addprefix se/sics/mspsim/,core platform/esb platform/sky util}

SOURCES := ${wildcard *.java $(addsuffix /*.java,$(PACKAGES))}

OBJECTS := $(SOURCES:.java=.class)


###############################################################
# MAKE
###############################################################

.PHONY: compile $(CPUTEST)

compile:	$(OBJECTS)

all:	compile

help:
	@echo "Usage: make [all,compile,clean]"

.PHONY: run
run:	compile
	java se.sics.mspsim.util.IHexReader $(FIRMWAREFILE) $(MAPFILE)

runesb:	compile
	java se.sics.mspsim.platform.esb.ESBNode $(FIRMWAREFILE) $(MAPFILE)

runsky:	compile
	java se.sics.mspsim.platform.sky.SkyNode $(FIRMWAREFILE) $(MAPFILE)

.PHONY: cputest test
test:	cputest

cputest:	$(CPUTEST)
	java se.sics.mspsim.util.Test $(CPUTEST)

$(CPUTEST):
	(cd tests && $(MAKE))

.PHONY: mtest
mtest:	compile $(CPUTEST)
	@-$(RM) mini-test_cpu.txt
	java se.sics.util.Test -debug $(CPUTEST) >mini-test_cpu.txt


###############################################################
# CLASS COMPILATION
###############################################################

%.class : %.java
	$(CC) $(CCARGS) $<


###############################################################
# CLEAN  (untrusted, use with great care!!!)
###############################################################

.PHONY:	clean claen clena

claen:	clean

clena:	clean

clean:
ifdef WINDIR
	-$(RM) *.class ${addsuffix /*.class,$(PACKAGES)}
else
	-$(RM) $(foreach f,$(wildcard *.class),$(APO)$(f)$(APO)) $(foreach dir,$(PACKAGES),$(dir)/*.class)
endif

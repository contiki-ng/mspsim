###############################################################
# $Revision: 1.11 $ $Date: 2007/10/21 19:47:53 $
#
# Needed stuff in the PATH:
#  java, javac (JDK 1.2 or newer)
#
# Under MS-DOS/Windows 95/NT
#  A GNU compatible Make (for example Cygnus GNU-Win 32,
#			  http://www.cygnus.com/misc/gnu-win32/)
# Note: might need to be called with "make --win32" under Windows!!!
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
  # These setting are for MS-DOS/Windows 95/Windows NT
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

IHEXFILE = blinker2.ihex

CPUTEST := tests/cputest.ihex
CPUTESTMAP := $(CPUTEST:.ihex=.map)

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
	java se.sics.util.IHexReader $(IHEXFILE) $(MAPFILE)

runesb:	compile
	java se.sics.mspsim.platform.esb.ESBNode $(IHEXFILE) $(MAPFILE)

runsky:	compile
	java se.sics.mspsim.platform.sky.SkyNode $(IHEXFILE) $(MAPFILE)

.PHONY: cputest test
test:	cputest

cputest:	$(CPUTEST)
	java se.sics.util.Test $(CPUTEST) $(CPUTESTMAP)

$(CPUTEST):
	(cd tests && $(MAKE))

test:
	cd tests && make
	java se.sics.util.Test $(CPUTEST) $(CPUTESTMAP)

.PHONY: mtest
mtest:	compile $(CPUTEST)
	@-$(RM) mini-test_cpu.txt
	java se.sics.util.Test -debug $(CPUTEST) $(CPUTESTMAP) >mini-test_cpu.txt


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

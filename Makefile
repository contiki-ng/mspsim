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

ifndef FIRMWAREFILE
ESBFIRMWARE = firmware/esb/sensor-demo.firmware
SKYFIRMWARE = firmware/sky/blink.firmware
else
ESBFIRMWARE = $FIRMWAREFILE
SKYFIRMWARE = $FIRMWAREFILE
endif

CPUTEST := tests/cputest.firmware

BINARY := README.txt license.txt images/*.jpg firmware/*/*.firmware

PACKAGES := ${addprefix se/sics/mspsim/,core platform/esb platform/sky util chip extutil/highlight}

SOURCES := ${wildcard *.java $(addsuffix /*.java,$(PACKAGES))}

OBJECTS := $(SOURCES:.java=.class)

JARFILE := mspsim.jar

###############################################################
# MAKE
###############################################################

.PHONY: all compile jar help run runesb runsky test cputest $(CPUTEST) mtest

all:	compile

compile:	$(OBJECTS)

jar:	compile
	$(JAR) cf $(JARFILE) ${addsuffix /*.class,$(PACKAGES)} images/*.jpg

help:
	@echo "Usage: make [all,compile,clean,run,runsky,runesb]"

run:	compile
	java se.sics.mspsim.util.IHexReader $(FIRMWAREFILE) $(MAPFILE)

runesb:	compile
	java se.sics.mspsim.platform.esb.ESBNode $(ESBFIRMWARE) $(MAPFILE)

runsky:	compile
	java se.sics.mspsim.platform.sky.SkyNode $(SKYFIRMWARE) $(MAPFILE)

test:	cputest

cputest:	$(CPUTEST)
	java se.sics.mspsim.util.Test $(CPUTEST)

$(CPUTEST):
	(cd tests && $(MAKE))

mtest:	compile $(CPUTEST)
	@-$(RM) mini-test_cpu.txt
	java se.sics.util.Test -debug $(CPUTEST) >mini-test_cpu.txt


###############################################################
# ARCHIVE GENERATION
###############################################################

source:
	zip -9 mspsim-source-`date '+%F'`.zip Makefile $(BINARY) *.java $(addsuffix /*.java,$(PACKAGES)) tests/Makefile tests/*.c tests/*.h


###############################################################
# CLASS COMPILATION
###############################################################

%.class : %.java
	$(CC) $(CCARGS) $<


###############################################################
# CLEAN  (untrusted, use with great care!!!)
###############################################################

.PHONY:	clean

clean:
ifdef WINDIR
	-$(RM) *.class ${addsuffix /*.class,$(PACKAGES)}
else
	-$(RM) $(foreach f,$(wildcard *.class),$(APO)$(f)$(APO)) $(foreach dir,$(PACKAGES),$(dir)/*.class)
endif

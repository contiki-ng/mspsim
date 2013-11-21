###############################################################
# Makefile for mspsim
#
# Needed stuff in the PATH:
#  java, javac (JDK 1.7 or newer)
#
# Under MS-DOS/Windows
#  A GNU compatible Make (for example Cygwin's)
###############################################################

###############################################################
# Settings
###############################################################
CC=javac
JAVA=java
JAR=jar
RM=rm -f

###############################################################
# System dependent
###############################################################

ifndef HOST_OS
  ifeq ($(OS),Windows_NT)
    HOST_OS := Windows
  else
    HOST_OS := $(shell uname)
  endif
endif

ifeq ($(HOST_OS),Windows)
  # These setting are for Windows
  SEPARATOR=;
else
  # This settings are for UNIX
  SEPARATOR=:
endif


###############################################################
# Arguments
###############################################################

EMPTY :=
SPACE := ${EMPTY} ${EMPTY}
LIBS := ${wildcard lib/*.jar}
BUILD := build
CLASSPATH=${subst ${SPACE},${SEPARATOR},$(BUILD)/ ${LIBS}}
CCARGS=-deprecation -Xlint:unchecked -source 1.7 -target 1.7 -classpath ".${SEPARATOR}${CLASSPATH}" -d $(BUILD)

JAVAARGS=-classpath "${CLASSPATH}"


###############################################################
# SERVER OBJECTS
###############################################################

ifndef FIRMWAREFILE
ESBFIRMWARE = firmware/esb/sensor-demo.esb
SKYFIRMWARE = firmware/sky/blink.sky
Z1FIRMWARE = firmware/z1/blink.z1
WISMOTEFIRMWARE = firmware/wismote/blink.wismote
TYNDALLFIRMWARE = firmware/tyndall/blink.tyndall
EXP5438FIRMWARE = firmware/exp5438/testcase-bits.exp5438
MSPEXP430F5438FIRMWARE = firmware/mspexp430f5438/LCDflat.out
else
ESBFIRMWARE = ${FIRMWAREFILE}
SKYFIRMWARE = ${FIRMWAREFILE}
Z1FIRMWARE = ${FIRMWAREFILE}
WISMOTEFIRMWARE = ${FIRMWAREFILE}
TYNDALLFIRMWARE = ${FIRMWAREFILE}
EXP5438FIRMWARE = ${FIRMWAREFILE}
MSPEXP430F5438FIRMWARE = ${FIRMWAREFILE}
endif

ifdef MAPFILE
MAPARGS := -map=$(MAPFILE)
endif

CPUTEST := tests/cputest.firmware
TIMERTEST := tests/timertest.firmware

SCRIPTS := ${addprefix scripts/,autorun.sc duty.sc}
BINARY := README.txt license.txt CHANGE_LOG.txt images/*.jpg images/*.png firmware/*/*.firmware ${SCRIPTS}

PACKAGES := se/sics/mspsim ${addprefix se/sics/mspsim/,core chip cli config debug platform ${addprefix platform/,esb sky jcreate sentillausb z1 tyndall ti MSPEXP430F5438 wismote} plugin profiler emulink net ui util extutil/highlight extutil/jfreechart}

SOURCES := ${wildcard *.java $(addsuffix /*.java,$(PACKAGES))}

OBJECTS := ${addprefix $(BUILD)/,$(SOURCES:.java=.class)}

JARFILE := mspsim.jar

###############################################################
# MAKE
###############################################################

.PHONY: all compile jar help run runesb runsky test cputest $(CPUTEST) mtest

all:	compile

compile:	$(OBJECTS)

jar:	$(JARFILE)

$(JARFILE):	$(OBJECTS)
	-@$(RM) JarManifest.txt
	@echo >>JarManifest.txt "Manifest-Version: 1.0"
	@echo >>JarManifest.txt "Sealed: true"
	@echo >>JarManifest.txt "Main-Class: se.sics.mspsim.Main"
	@echo >>JarManifest.txt "Class-path: ${LIBS}"
	$(JAR) cfm $(JARFILE) JarManifest.txt images/*.jpg -C $(BUILD) .
	-@$(RM) JarManifest.txt

%.esb:	jar
	java -jar $(JARFILE) -platform=esb $@ $(ARGS)

%.sky:	jar
	java -jar $(JARFILE) -platform=sky $@ $(ARGS)

%.z1:	jar
	java -jar $(JARFILE) -platform=z1 $@ $(ARGS)

%.exp5438:	jar
	java -jar $(JARFILE) -platform=exp5438 $@ $(ARGS)

%.mspexp5438:	jar
	java -jar $(JARFILE) -platform=exp5438 $@ $(ARGS)


%.tyndall:	jar
	java -jar $(JARFILE) -platform=tyndall $@ $(ARGS)

%.wismote:	jar
	java -jar $(JARFILE) -platform=wismote $@ $(ARGS)

help:
	@echo "Usage: make [all,compile,clean,run,runsky,runesb]"

run:	compile
	$(JAVA) $(JAVAARGS) se.sics.mspsim.Main $(FIRMWAREFILE) $(MAPARGS) $(ARGS)

runesb:	compile
	$(JAVA) $(JAVAARGS) se.sics.mspsim.platform.esb.ESBNode $(ESBFIRMWARE) $(MAPARGS) $(ARGS)

runsky:	compile
	$(JAVA) $(JAVAARGS) se.sics.mspsim.platform.sky.SkyNode $(SKYFIRMWARE) $(MAPARGS) $(ARGS)

runskyprof:	compile
	$(JAVA) -agentlib:yjpagent $(JAVAARGS) se.sics.mspsim.platform.sky.SkyNode $(SKYFIRMWARE) $(MAPARGS) $(ARGS)

runtelos:	compile
	$(JAVA) $(JAVAARGS) se.sics.mspsim.platform.sky.TelosNode $(SKYFIRMWARE) $(MAPARGS) $(ARGS)
runz1:	compile
	$(JAVA) $(JAVAARGS) se.sics.mspsim.platform.z1.Z1Node $(Z1FIRMWARE) $(MAPARGS) $(ARGS)
runtyndall:	compile
	$(JAVA) $(JAVAARGS) se.sics.mspsim.platform.tyndall.TyndallNode $(TYNDALLFIRMWARE) $(MAPARGS) $(ARGS)
runwismote:	compile
	$(JAVA) $(JAVAARGS) se.sics.mspsim.platform.wismote.WismoteNode $(WISMOTEFIRMWARE) $(MAPARGS) $(ARGS)

runexp5438:	compile
	$(JAVA) $(JAVAARGS) se.sics.mspsim.platform.ti.Exp5438Node $(EXP5438FIRMWARE) $(MAPARGS) $(ARGS)
runmspexp5438:	compile
	$(JAVA) $(JAVAARGS) se.sics.mspsim.platform.MSPEXP430F5438.Exp5438Node $(MSPEXP430F5438FIRMWARE) $(MAPARGS) $(ARGS)

test:	cputest

cputest:	$(CPUTEST)
	$(JAVA) $(JAVAARGS) se.sics.mspsim.util.Test $(CPUTEST)

timertest:	$(TIMERTEST)
	$(JAVA) $(JAVAARGS) se.sics.mspsim.util.Test $(TIMERTEST)

$(CPUTEST):
	(cd tests && $(MAKE))
$(TIMERTEST):
	(cd tests && $(MAKE))

mtest:	compile $(CPUTEST)
	@-$(RM) mini-test_cpu.txt
	$(JAVA) $(JAVAARGS) se.sics.util.Test -debug $(CPUTEST) >mini-test_cpu.txt


###############################################################
# ARCHIVE GENERATION
###############################################################

source:
	zip -9 mspsim-source-`date '+%F'`.zip Makefile $(BINARY) $(addsuffix /*.java,$(PACKAGES)) tests/Makefile tests/*.c tests/*.h lib/*.*


###############################################################
# CLASS COMPILATION
###############################################################

$(BUILD):
	@mkdir $@

$(BUILD)/%.class : %.java $(BUILD)
	$(CC) $(CCARGS) $<


###############################################################
# CLEAN  (untrusted, use with great care!!!)
###############################################################

.PHONY:	clean

clean:
	-$(RM) -r $(BUILD)

distclean: clean
	-$(RM) -f $(JARFILE)

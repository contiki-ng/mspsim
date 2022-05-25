.SUFFIXES:

#MCU=msp430f149
MCU=msp430f1611

### Compiler definitions
CC       = msp430-gcc
LD       = msp430-ld
AS       = msp430-as
AR       = msp430-ar
OBJCOPY  = msp430-objcopy
STRIP    = msp430-strip
BSL      = msp430-bsl
CFLAGSNO = -I. -Wall -mmcu=$(MCU) -g -gstabs+
CFLAGS  += $(CFLAGSNO) -Os

SOURCES := msp430setup.c

OBJECTS := $(SOURCES:.c=.o)

#all:	cputest.ihex
all:	cputest.firmware timertest.firmware


%.firmware:	%.o $(OBJECTS)
	$(CC) $(CFLAGS) -Wl,-Map=$(@:.firmware=.map) -o $@ $^

%.ihex: %.firmware
	$(OBJCOPY) $^ -O ihex $@

%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

%.u:	%.ihex
	msp430-jtag -eI $^

clean:
	rm -f *~ *.lst *.map *.o *.ihex *.firmware

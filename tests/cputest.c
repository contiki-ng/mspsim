/*
 * Copyright (c) 2007, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 *
 * $Id: cputest.c,v 1.19 2007/10/24 22:17:46 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * Author  : Adam Dunkels, Joakim Eriksson, Niclas Finne
 * Created : 2006-03-07
 * Updated : $Date: 2007/10/24 22:17:46 $
 *           $Revision: 1.19 $
 */

#include "msp430setup.h"
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <io.h>

/* From Adams test-suite */
#define TEST(...) if(__VA_ARGS__) {					 \
                    printf("OK: " #__VA_ARGS__ " passed at %s:%d\n", __FILE__,__LINE__); \
                  } else {						 \
                    printf("FAIL: " #__VA_ARGS__ " failed at %s:%d\n", __FILE__,__LINE__); \
                  }

#define TEST2(text,...) if(__VA_ARGS__) {					 \
                    printf("OK: " #text " passed at %s:%d\n", __FILE__,__LINE__); \
                  } else {						 \
                    printf("FAIL: " #text " failed at %s:%d\n", __FILE__,__LINE__); \
                  }

#define assertTrue(...) TEST(__VA_ARGS__)
#define assertFalse(...) TEST(!(__VA_ARGS__))

#define assertTrue2(text,...) TEST2(text,__VA_ARGS__)
#define assertFalse2(text,...) TEST2(text,!(__VA_ARGS__))

static int testzero(int hm)
{
  return hm > 0;
}

static int caseID = 0;

/*---------------------------------------------------------------------------*/
static int pos = 0;
static unsigned int times[10];
interrupt(TIMERB1_VECTOR) timerb1 (void) {
  if(TBIV == 2) {
	  if (pos < 10) {
		  times[pos] = TBR;
		  pos++;
		  TBCCR1 = TBCCR1 + 100;
	  }
  }
}
/*---------------------------------------------------------------------------*/

static void initTest() {
  caseID = 0;
}

static void testCase(char *description) {
  caseID++;
  printf("-------------\n");
  printf("TEST %d: %s\n", caseID, description);
}

static void testSimple() {
  int a,b,c;
  a = 1; b = 2; c = 4;
  testCase("Arithmetic Operations");

  assertTrue((a << b) == 4);
  assertTrue((c >> a) == 2);

  assertFalse(0 > 0);
  assertFalse(a > b);

  assertFalse(testzero(0));
}

static void testIntegers() {
  int a,b,c;
  int t[3];
  testCase("Integer Operations");
  a = 1; b = 2; c = -42;
  t[0] = 1;
  t[1] = 2;
  t[2] = 3;
  assertTrue(a == 1);
  assertTrue((b + c) == -40);
  assertTrue(t[0] == 1);
  assertTrue(t[1] == 2);
  assertTrue(t[t[0]] == 2);
  assertTrue((a+b) == 3);
  assertTrue((b-a) == 1);
  assertTrue((a-b) == -1);
  assertTrue((a*b) == 2);
  assertTrue(a > 0);
  assertTrue(b > a);
  assertTrue(b != a);
  assertTrue((a ^ b) == 3);
  assertTrue((a ^ 4) == 5);
  assertTrue((a ^ a) == 0);
  assertFalse(a > b);

  a = 15;
  b = 17;
  assertTrue((a & ~b) == 14);
}

static void testFloats() {
  int i;
  float f;
  i = 2;
  f = 0.5;
  testCase("Float Operations");
  assertTrue((i * f) == 1);
  i++;
  assertTrue((i * f) == 1.5);
}

static void testStrings() {
  char buf[10];

  testCase("String Operations");
  sprintf(buf, "test");
  assertTrue2("test => test", strcmp(buf, "test") == 0);
  sprintf(buf, "%c", 'a');
  assertTrue2("buf == 'a'", strcmp(buf, "a") == 0);
}

/*--------------------------------------------------------------------------*/

static int id(int first, ...)
{
  va_list marker;
  va_start( marker, first );
  first = va_arg( marker, int);
  va_end( marker );
  return first;
}

static void testFunctions() {
  volatile int i;

  testCase("Functions");
  i = 47;

/*   printf("i=%d i+1=%d id(i)=%d\n", i, i + 1, id(0, i)); */

  assertTrue(i == 47);
  assertTrue(id(0,47) == 47);
  assertTrue(id(0,i) == 47);
}

/*--------------------------------------------------------------------------*/

static void testModulo() {
  int c;
  testCase("Modulo");

  for(c = 2; c >= 0; c--) {
    if((c % 5) == 0) {
      assertTrue(c != 1);
    }
/*     printf("(%d,%d,%d)\n", c, (c % 5), c==1); */
/*     printf("(%d,%d,%d)\n", c, (c % 5), c==1); */
  }
}

/*--------------------------------------------------------------------------*/

/* Bit field tests */

static struct {
  char green:4, yellow:4, red:4;
} leds;

static struct {
  char green:2, yellow:2, red:2;  /* These bit fields should really be
				     only one bit wide, but that
				     crashed gcc... */
} invert;

#include <io.h>
#ifndef BV
#define BV(n) (1 << (n))
#endif

void testBis() {
  P4DIR |= BV(2) | BV(5) | BV(6);
  assertTrue(P4DIR == 100);
}

static void testBitFields() {
  static int pelle = 0;

  testCase("Bit field Operations");

  P4DIR = 0;
  testBis();

  leds.green = 1;
  leds.yellow = 1;
  leds.red = 1;

  assertTrue(leds.green > 0);
  assertTrue(leds.yellow > 0);
  assertTrue(leds.red > 0);

  leds.green -= 1;
  assertTrue(leds.green == 0);

  pelle = 0x4711;

  invert.green = 1;
  invert.yellow = 1;
  invert.red = 1;

  assertTrue(invert.green > 0);
  assertTrue(invert.yellow > 0);
  assertTrue(invert.red > 0);

  invert.green ^= 1;
  assertTrue(invert.green == 0);

}


/*--------------------------------------------------------------------------*/
static int flag;
interrupt(UART0TX_VECTOR)
     usart_tx_test0(void)
{
  printf("*IRQ: Flags:%d %d\n", IFG1, UTCTL0);
  flag++;
}

static void testUSART() {
  int delay = 10000;
  testCase("Bit USART Operations");
  flag = 0xff;
  UCTL0 = CHAR;                         /* 8-bit character */
  UTCTL0 = SSEL1;                       /* UCLK = MCLK */
  ME1 |= (UTXE0 | URXE0);                 /* Enable USART0 TXD/RXD */
  IE1 |= UTXIE0;                        /* Enable USART0 TX interrupt  */
  TXBUF_0 = 'a';
 
  while(flag == 0) {
  }
  while(delay-- > 0);
  
  printf("output finished...\n");
}

static void testTimer() {
  int i;
  pos = 0;
  dint();
  /* Select SMCLK (2.4576MHz), clear TAR; This makes the rtimer count
     the number of processor cycles executed by the CPU. */
  //TBCTL = TBSSEL1 | TBCLR;
  /* Select ACLK 32768Hz clock, divide by 1 (was ID_8 previously) */
  TBCTL = TBSSEL0 | TBCLR | ID_0;

  /* CCR1 interrupt enabled, interrupt occurs when timer equals CCR1. */
  TBCCTL1 = CCIE;

  /* Start Timer_B in continuous mode. */
  TBCTL |= MC1;

  TBR = 0;
  TBCCR1 = 100;
  
  /* Enable interrupts. */
  eint();
  
  while (pos < 10) {
	  printf("waiting for timer...%d\n", pos);
  }
  
  for (i = 0; i < pos; i++) {
	  printf("Trigg time %d => %d\n", i + 1, times[i]);
  }
}
	
/*--------------------------------------------------------------------------*/

int
main(void)
{
  msp430_setup();

  initTest();

  testSimple();
  testIntegers();
  testFloats();
  testStrings();
  testBitFields();
  testFunctions();
  testModulo();
  testUSART();
  testTimer();
  /*  printf("PROFILE\n"); */
  printf("EXIT\n");
  return 0;
}
